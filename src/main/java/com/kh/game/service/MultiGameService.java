package com.kh.game.service;

import com.kh.game.entity.*;
import com.kh.game.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MultiGameService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRoomParticipantRepository participantRepository;
    private final GameRoomChatRepository chatRepository;
    private final SongService songService;
    private final GenreService genreService;
    private final AnswerValidationService answerValidationService;
    private final MemberService memberService;
    private final ObjectMapper objectMapper;

    // 이미 출제된 노래 ID를 방별로 관리
    private final Map<Long, Set<Long>> usedSongsByRoom = new HashMap<>();

    // ========== 게임 진행 ==========

    /**
     * 게임 시작 (방장만) - 대기 상태로 전환
     */
    @Transactional
    public void startGame(GameRoom room, Member host) {
        if (!room.isHost(host)) {
            throw new IllegalStateException("방장만 게임을 시작할 수 있습니다.");
        }

        if (room.getStatus() != GameRoom.RoomStatus.WAITING) {
            throw new IllegalStateException("이미 게임이 진행중입니다.");
        }

        List<GameRoomParticipant> participants = participantRepository.findActiveParticipants(room);
        if (participants.size() < 2) {
            throw new IllegalStateException("최소 2명 이상 필요합니다.");
        }

        boolean allReady = participants.stream().allMatch(GameRoomParticipant::getIsReady);
        if (!allReady) {
            throw new IllegalStateException("모든 참가자가 준비되지 않았습니다.");
        }

        // 게임 상태 변경
        room.setStatus(GameRoom.RoomStatus.PLAYING);
        room.setCurrentRound(0);
        room.setRoundPhase(null);  // 아직 라운드 시작 전

        // 참가자 상태 변경
        for (GameRoomParticipant p : participants) {
            p.setStatus(GameRoomParticipant.ParticipantStatus.PLAYING);
            p.resetScore();
        }

        // 사용된 노래 목록 초기화
        usedSongsByRoom.put(room.getId(), new HashSet<>());

        // 시스템 메시지
        addSystemMessage(room, host, "🎮 게임이 시작되었습니다! 방장이 라운드를 시작하면 노래가 재생됩니다.");
    }

    /**
     * 라운드 시작 (방장만) - 노래 선택 및 재생
     */
    @Transactional
    public Map<String, Object> startRound(GameRoom room, Member host) {
        Map<String, Object> result = new HashMap<>();

        if (!room.isHost(host)) {
            result.put("success", false);
            result.put("message", "방장만 라운드를 시작할 수 있습니다.");
            return result;
        }

        if (room.getStatus() != GameRoom.RoomStatus.PLAYING) {
            result.put("success", false);
            result.put("message", "게임이 진행중이 아닙니다.");
            return result;
        }

        // 이미 PLAYING 상태면 중복 시작 방지
        if (room.getRoundPhase() == GameRoom.RoundPhase.PLAYING) {
            result.put("success", false);
            result.put("message", "이미 라운드가 진행중입니다.");
            return result;
        }

        // 라운드 증가
        room.setCurrentRound(room.getCurrentRound() + 1);

        // 총 라운드 초과 체크
        if (room.getCurrentRound() > room.getTotalRounds()) {
            finishGame(room);
            result.put("success", true);
            result.put("isGameOver", true);
            return result;
        }

        // 노래 선택
        Song song = selectSong(room);
        if (song == null) {
            finishGame(room);
            result.put("success", true);
            result.put("isGameOver", true);
            result.put("message", "출제할 노래가 없습니다.");
            return result;
        }

        // 라운드 상태 설정 - 바로 PLAYING 단계로 시작
        room.setCurrentSong(song);
        room.setRoundPhase(GameRoom.RoundPhase.PLAYING);
        room.setRoundStartTime(LocalDateTime.now());
        room.setWinner(null);  // 정답자 초기화

        // 오디오 바로 재생
        room.setAudioPlaying(true);
        room.setAudioPlayedAt(System.currentTimeMillis());

        // 사용된 노래 기록
        usedSongsByRoom.computeIfAbsent(room.getId(), k -> new HashSet<>()).add(song.getId());

        // 시스템 메시지
        addSystemMessage(room, host, "🎵 라운드 " + room.getCurrentRound() + " - 노래를 맞춰보세요!");

        result.put("success", true);
        result.put("isGameOver", false);
        result.put("currentRound", room.getCurrentRound());

        return result;
    }

    /**
     * 참가자 라운드 준비 완료
     */
    @Transactional
    public Map<String, Object> setRoundReady(GameRoom room, Member member) {
        Map<String, Object> result = new HashMap<>();

        if (room.getRoundPhase() != GameRoom.RoundPhase.PREPARING) {
            result.put("success", false);
            result.put("message", "준비 단계가 아닙니다.");
            return result;
        }

        GameRoomParticipant participant = participantRepository.findByGameRoomAndMember(room, member)
                .orElse(null);
        if (participant == null) {
            result.put("success", false);
            result.put("message", "참가자가 아닙니다.");
            return result;
        }

        participant.setRoundReady(true);
        result.put("success", true);

        // 모든 참가자가 준비됐는지 체크
        boolean allReady = checkAllRoundReady(room);
        result.put("allReady", allReady);

        if (allReady) {
            // 자동으로 PLAYING 단계로 전환
            startPlaying(room);
        }

        return result;
    }

    /**
     * 모든 참가자가 라운드 준비 완료했는지 체크
     */
    private boolean checkAllRoundReady(GameRoom room) {
        List<GameRoomParticipant> participants = participantRepository.findGameParticipants(room);
        return participants.stream().allMatch(GameRoomParticipant::getRoundReady);
    }

    /**
     * PREPARING에서 PLAYING으로 전환 (모든 참가자 준비 완료 시)
     */
    private void startPlaying(GameRoom room) {
        room.setRoundPhase(GameRoom.RoundPhase.PLAYING);
        room.setRoundStartTime(LocalDateTime.now());  // 실제 재생 시작 시간으로 리셋
        room.setAudioPlaying(true);
        room.setAudioPlayedAt(System.currentTimeMillis());

        addSystemMessage(room, room.getHost(), "🎵 모든 참가자 준비 완료! 노래를 맞춰보세요!");
    }

    /**
     * 현재 곡 스킵 (재생 오류 시 방장만)
     * PREPARING 또는 PLAYING 상태에서 호출 가능
     */
    @Transactional
    public Map<String, Object> skipCurrentSong(GameRoom room, Member host, Long songId) {
        Map<String, Object> result = new HashMap<>();

        if (!room.isHost(host)) {
            result.put("success", false);
            result.put("message", "방장만 곡을 스킵할 수 있습니다.");
            return result;
        }

        if (room.getRoundPhase() != GameRoom.RoundPhase.PLAYING) {
            result.put("success", false);
            result.put("message", "현재 스킵할 수 없는 상태입니다.");
            return result;
        }

        // 현재 곡이 맞는지 확인
        Song currentSong = room.getCurrentSong();
        if (currentSong == null || !currentSong.getId().equals(songId)) {
            result.put("success", false);
            result.put("message", "이미 다른 곡이 선택되었습니다.");
            return result;
        }

        // 새 노래 선택
        Song newSong = selectSong(room);
        if (newSong == null) {
            // 더 이상 곡이 없으면 게임 종료
            finishGame(room);
            result.put("success", true);
            result.put("isGameOver", true);
            result.put("message", "출제할 노래가 없습니다.");
            return result;
        }

        // 새 노래 사용 기록
        usedSongsByRoom.computeIfAbsent(room.getId(), k -> new HashSet<>()).add(newSong.getId());

        // 노래 교체 및 바로 재생
        room.setCurrentSong(newSong);
        room.setRoundPhase(GameRoom.RoundPhase.PLAYING);
        room.setRoundStartTime(LocalDateTime.now());
        room.setAudioPlaying(true);
        room.setAudioPlayedAt(System.currentTimeMillis());

        // 시스템 메시지
        addSystemMessage(room, host, "⚠️ 재생 오류로 다른 곡으로 변경되었습니다. 노래를 맞춰보세요!");

        result.put("success", true);
        result.put("isGameOver", false);
        result.put("currentRound", room.getCurrentRound());

        return result;
    }

    /**
     * 다음 라운드로 (방장만) - RESULT 상태에서 호출, 바로 다음 라운드 시작
     */
    @Transactional
    public Map<String, Object> nextRound(GameRoom room, Member host) {
        Map<String, Object> result = new HashMap<>();

        if (!room.isHost(host)) {
            result.put("success", false);
            result.put("message", "방장만 다음 라운드를 진행할 수 있습니다.");
            return result;
        }

        // 마지막 라운드였으면 게임 종료
        if (room.getCurrentRound() >= room.getTotalRounds()) {
            finishGame(room);
            result.put("success", true);
            result.put("isGameOver", true);
            return result;
        }

        // 라운드 증가
        room.setCurrentRound(room.getCurrentRound() + 1);

        // 총 라운드 초과 체크
        if (room.getCurrentRound() > room.getTotalRounds()) {
            finishGame(room);
            result.put("success", true);
            result.put("isGameOver", true);
            return result;
        }

        // 노래 선택
        Song song = selectSong(room);
        if (song == null) {
            finishGame(room);
            result.put("success", true);
            result.put("isGameOver", true);
            result.put("message", "출제할 노래가 없습니다.");
            return result;
        }

        // 라운드 상태 설정 - 바로 PLAYING 단계로 시작
        room.setCurrentSong(song);
        room.setRoundPhase(GameRoom.RoundPhase.PLAYING);
        room.setRoundStartTime(LocalDateTime.now());
        room.setWinner(null);

        // 오디오 바로 재생
        room.setAudioPlaying(true);
        room.setAudioPlayedAt(System.currentTimeMillis());

        // 사용된 노래 기록
        usedSongsByRoom.computeIfAbsent(room.getId(), k -> new HashSet<>()).add(song.getId());

        // 시스템 메시지
        addSystemMessage(room, host, "🎵 라운드 " + room.getCurrentRound() + " - 노래를 맞춰보세요!");

        result.put("success", true);
        result.put("isGameOver", false);
        result.put("currentRound", room.getCurrentRound());

        return result;
    }

    // ========== 채팅 ==========

    /**
     * 채팅 전송 (정답 체크 포함)
     */
    @Transactional
    public Map<String, Object> sendChat(GameRoom room, Member member, String message) {
        Map<String, Object> result = new HashMap<>();

        if (message == null || message.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "메시지를 입력해주세요.");
            return result;
        }

        String trimmedMessage = message.trim();
        if (trimmedMessage.length() > 200) {
            trimmedMessage = trimmedMessage.substring(0, 200);
        }

        // 참가자 확인
        GameRoomParticipant participant = participantRepository.findByGameRoomAndMember(room, member)
                .orElse(null);
        if (participant == null) {
            result.put("success", false);
            result.put("message", "참가자가 아닙니다.");
            return result;
        }

        // PLAYING 상태이고 정답자가 없으면 정답 체크
        boolean isCorrectAnswer = false;
        if (room.getRoundPhase() == GameRoom.RoundPhase.PLAYING && room.getWinner() == null) {
            Song currentSong = room.getCurrentSong();
            if (currentSong != null) {
                isCorrectAnswer = answerValidationService.validateAnswer(trimmedMessage, currentSong);
            }
        }

        if (isCorrectAnswer) {
            // 정답 처리 (이미 정답자가 있으면 false 반환)
            boolean wasWinner = handleCorrectAnswer(room, member, participant, trimmedMessage);
            if (wasWinner) {
                result.put("isCorrect", true);
            } else {
                // 이미 다른 사람이 먼저 맞춤 - 일반 채팅으로 저장
                GameRoomChat chat = GameRoomChat.chat(room, member, trimmedMessage);
                chatRepository.save(chat);
                result.put("isCorrect", false);
            }
        } else {
            // 일반 채팅 저장
            GameRoomChat chat = GameRoomChat.chat(room, member, trimmedMessage);
            chatRepository.save(chat);
            result.put("isCorrect", false);
        }

        result.put("success", true);
        return result;
    }

    /**
     * 정답 처리 (동시 제출 방지를 위한 synchronized)
     * @return 정답 처리 성공 여부 (이미 정답자가 있으면 false)
     */
    private synchronized boolean handleCorrectAnswer(GameRoom room, Member member, GameRoomParticipant participant, String answer) {
        // 이미 정답자가 있으면 무시 (동시 제출 방지)
        if (room.getWinner() != null) {
            return false;
        }

        // 정답자 설정
        room.setWinner(member);

        // 오디오 정지
        room.setAudioPlaying(false);
        room.setAudioPlayedAt(null);

        // 라운드 결과로 전환
        room.setRoundPhase(GameRoom.RoundPhase.RESULT);

        // 점수 추가 (100점 고정)
        participant.addScore(100);
        participant.incrementCorrect();

        // 정답 채팅 저장
        GameRoomChat correctChat = GameRoomChat.correctAnswer(room, member, answer, room.getCurrentRound());
        chatRepository.save(correctChat);

        // 정답 정보 시스템 메시지
        Song song = room.getCurrentSong();
        String answerMessage = String.format("🎉 정답: %s - %s", song.getArtist(), song.getTitle());
        addSystemMessage(room, member, answerMessage);

        return true;
    }

    /**
     * 시스템 메시지 추가
     */
    private void addSystemMessage(GameRoom room, Member member, String message) {
        GameRoomChat systemChat = GameRoomChat.system(room, member, message);
        chatRepository.save(systemChat);
    }

    /**
     * 채팅 목록 조회 (lastId 이후)
     */
    public List<Map<String, Object>> getChats(GameRoom room, Long lastId) {
        List<GameRoomChat> chats;
        if (lastId == null || lastId == 0) {
            chats = chatRepository.findByGameRoomOrderByCreatedAtAsc(room);
        } else {
            chats = chatRepository.findByGameRoomAndIdGreaterThan(room, lastId);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (GameRoomChat chat : chats) {
            Map<String, Object> chatInfo = new HashMap<>();
            chatInfo.put("id", chat.getId());
            chatInfo.put("memberId", chat.getMember().getId());
            chatInfo.put("nickname", chat.getMember().getNickname());
            chatInfo.put("message", chat.getMessage());
            chatInfo.put("messageType", chat.getMessageType().name());
            chatInfo.put("roundNumber", chat.getRoundNumber());
            chatInfo.put("createdAt", chat.getCreatedAt().toString());
            chatInfo.put("isHost", room.isHost(chat.getMember()));
            result.add(chatInfo);
        }

        return result;
    }

    // ========== 게임 상태 조회 ==========

    /**
     * 현재 라운드 정보 조회
     */
    public Map<String, Object> getCurrentRoundInfo(GameRoom room) {
        Map<String, Object> info = new HashMap<>();

        info.put("currentRound", room.getCurrentRound());
        info.put("totalRounds", room.getTotalRounds());
        info.put("roundPhase", room.getRoundPhase() != null ? room.getRoundPhase().name() : null);
        info.put("status", room.getStatus().name());

        // 오디오 상태
        info.put("audioPlaying", room.getAudioPlaying());
        info.put("audioPlayedAt", room.getAudioPlayedAt());
        info.put("serverTime", System.currentTimeMillis());  // 클라이언트 시간 동기화용

        // 정답자 정보
        if (room.getWinner() != null) {
            info.put("winnerId", room.getWinner().getId());
            info.put("winnerNickname", room.getWinner().getNickname());
        }

        Song currentSong = room.getCurrentSong();
        // PLAYING 상태에서 노래 파일 정보 (정답은 숨김)
        if (currentSong != null && room.getRoundPhase() == GameRoom.RoundPhase.PLAYING) {
            Map<String, Object> songInfo = new HashMap<>();
            songInfo.put("id", currentSong.getId());
            songInfo.put("youtubeVideoId", currentSong.getYoutubeVideoId());
            songInfo.put("filePath", currentSong.getFilePath());
            songInfo.put("startTime", currentSong.getStartTime());
            songInfo.put("playDuration", currentSong.getPlayDuration());
            info.put("song", songInfo);
        }

        // RESULT 상태에서 정답 정보
        if (room.getRoundPhase() == GameRoom.RoundPhase.RESULT && currentSong != null) {
            Map<String, Object> answerInfo = new HashMap<>();
            answerInfo.put("title", currentSong.getTitle());
            answerInfo.put("artist", currentSong.getArtist());
            answerInfo.put("releaseYear", currentSong.getReleaseYear());
            if (currentSong.getGenre() != null) {
                answerInfo.put("genre", currentSong.getGenre().getName());
            }
            info.put("answer", answerInfo);
        }

        // 참가자별 점수 (PLAYING 상태도 포함)
        List<GameRoomParticipant> participants = participantRepository.findGameParticipants(room);
        List<Map<String, Object>> participantInfos = new ArrayList<>();
        for (GameRoomParticipant p : participants) {
            Map<String, Object> pInfo = new HashMap<>();
            pInfo.put("memberId", p.getMember().getId());
            pInfo.put("nickname", p.getMember().getNickname());
            pInfo.put("score", p.getScore());
            pInfo.put("correctCount", p.getCorrectCount());
            pInfo.put("isHost", room.isHost(p.getMember()));
            pInfo.put("roundReady", p.getRoundReady());  // 라운드 준비 상태 추가
            participantInfos.add(pInfo);
        }

        // 점수순 정렬
        participantInfos.sort((a, b) -> (Integer) b.get("score") - (Integer) a.get("score"));
        info.put("participants", participantInfos);

        return info;
    }

    /**
     * 최종 결과 조회
     */
    public List<Map<String, Object>> getFinalResult(GameRoom room) {
        List<GameRoomParticipant> participants = participantRepository.findByGameRoomOrderByScoreDesc(room);
        List<Map<String, Object>> result = new ArrayList<>();

        int rank = 1;
        for (GameRoomParticipant p : participants) {
            Map<String, Object> pInfo = new HashMap<>();
            pInfo.put("rank", rank++);
            pInfo.put("memberId", p.getMember().getId());
            pInfo.put("nickname", p.getMember().getNickname());
            pInfo.put("score", p.getScore());
            pInfo.put("correctCount", p.getCorrectCount());
            pInfo.put("isHost", room.isHost(p.getMember()));
            result.add(pInfo);
        }

        return result;
    }

    // ========== 내부 헬퍼 ==========

    /**
     * 노래 선택 (YouTube 사전 검증 포함)
     */
    private Song selectSong(GameRoom room) {
        Set<Long> usedSongs = usedSongsByRoom.getOrDefault(room.getId(), new HashSet<>());
        String gameMode = getGameMode(room);

        Long targetGenreId = null;
        if ("FIXED_GENRE".equals(gameMode)) {
            targetGenreId = getFixedGenreId(room);
        }

        // YouTube 검증 포함된 메서드 사용
        return songService.getValidatedRandomSongExcluding(targetGenreId, usedSongs);
    }

    private String getGameMode(GameRoom room) {
        try {
            if (room.getSettings() != null) {
                Map<String, Object> settings = objectMapper.readValue(room.getSettings(), Map.class);
                return (String) settings.getOrDefault("gameMode", "RANDOM");
            }
        } catch (Exception e) {
            log.error("설정 파싱 오류", e);
        }
        return "RANDOM";
    }

    private Long getFixedGenreId(GameRoom room) {
        try {
            if (room.getSettings() != null) {
                Map<String, Object> settings = objectMapper.readValue(room.getSettings(), Map.class);
                Object genreId = settings.get("fixedGenreId");
                if (genreId != null) {
                    return ((Number) genreId).longValue();
                }
            }
        } catch (Exception e) {
            log.error("설정 파싱 오류", e);
        }
        return null;
    }

    /**
     * 방 종료 시 정리
     */
    @Transactional
    public void cleanupRoom(GameRoom room) {
        usedSongsByRoom.remove(room.getId());
        room.setStatus(GameRoom.RoomStatus.FINISHED);
    }

    /**
     * 게임 종료 처리 - Member 통계 업데이트
     */
    @Transactional
    public void finishGame(GameRoom room) {
        room.setStatus(GameRoom.RoomStatus.FINISHED);
        usedSongsByRoom.remove(room.getId());

        // 모든 참가자의 통계를 Member에 반영
        List<GameRoomParticipant> participants = participantRepository.findGameParticipants(room);
        int totalRounds = room.getCurrentRound();  // 실제 진행된 라운드 수

        for (GameRoomParticipant participant : participants) {
            Member member = participant.getMember();
            if (member != null) {
                memberService.addMultiGameResult(
                        member.getId(),
                        participant.getScore(),
                        participant.getCorrectCount(),
                        totalRounds
                );
            }
        }

        log.info("멀티게임 종료 - 방: {}, 참가자: {}명, 라운드: {}",
                room.getId(), participants.size(), totalRounds);
    }
}