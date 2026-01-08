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
    private final SongService songService;
    private final GenreService genreService;
    private final AnswerValidationService answerValidationService;
    private final ObjectMapper objectMapper;

    // 이미 출제된 노래 ID를 방별로 관리
    private final Map<Long, Set<Long>> usedSongsByRoom = new HashMap<>();

    /**
     * 게임 시작 (방장만)
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

        // 참가자 상태 변경
        for (GameRoomParticipant p : participants) {
            p.setStatus(GameRoomParticipant.ParticipantStatus.PLAYING);
            p.resetScore();
            p.resetRoundAnswer();
        }

        // 사용된 노래 목록 초기화
        usedSongsByRoom.put(room.getId(), new HashSet<>());

        // 게임 모드에 따라 첫 라운드 시작
        String gameMode = getGameMode(room);
        if ("GENRE_PER_ROUND".equals(gameMode)) {
            room.setRoundPhase(GameRoom.RoundPhase.GENRE_SELECT);
        } else {
            // RANDOM 또는 FIXED_GENRE 모드는 바로 라운드 시작
            startNextRound(room, null);
        }
    }

    /**
     * 다음 라운드 시작
     */
    @Transactional
    public Song startNextRound(GameRoom room, Long genreId) {
        room.nextRound();

        // 오디오 상태 초기화
        room.setAudioPlaying(false);
        room.setAudioPlayedAt(null);

        // 참가자 라운드 답변 초기화
        List<GameRoomParticipant> participants = participantRepository.findActiveParticipants(room);
        for (GameRoomParticipant p : participants) {
            p.resetRoundAnswer();
        }

        // 노래 선택
        Song song = selectSong(room, genreId);
        if (song == null) {
            // 노래 없으면 게임 종료
            room.setStatus(GameRoom.RoomStatus.FINISHED);
            room.setRoundPhase(null);
            return null;
        }

        room.setCurrentSong(song);
        room.setRoundPhase(GameRoom.RoundPhase.PLAYING);
        room.setRoundStartTime(LocalDateTime.now());

        // 사용된 노래 기록
        usedSongsByRoom.computeIfAbsent(room.getId(), k -> new HashSet<>()).add(song.getId());

        return song;
    }

    /**
     * 장르 선택 (GENRE_PER_ROUND 모드, 방장만)
     */
    @Transactional
    public Song selectGenre(GameRoom room, Member host, Long genreId) {
        if (!room.isHost(host)) {
            throw new IllegalStateException("방장만 장르를 선택할 수 있습니다.");
        }

        if (room.getRoundPhase() != GameRoom.RoundPhase.GENRE_SELECT) {
            throw new IllegalStateException("장르 선택 단계가 아닙니다.");
        }

        return startNextRound(room, genreId);
    }

    /**
     * 답변 제출
     */
    @Transactional
    public Map<String, Object> submitAnswer(GameRoom room, Member member, String answer) {
        Map<String, Object> result = new HashMap<>();

        if (room.getStatus() != GameRoom.RoomStatus.PLAYING) {
            result.put("success", false);
            result.put("message", "게임이 진행중이 아닙니다.");
            return result;
        }

        if (room.getRoundPhase() != GameRoom.RoundPhase.PLAYING) {
            result.put("success", false);
            result.put("message", "답변 제출 단계가 아닙니다.");
            return result;
        }

        GameRoomParticipant participant = participantRepository.findByGameRoomAndMember(room, member)
                .orElseThrow(() -> new IllegalArgumentException("참가자 정보를 찾을 수 없습니다."));

        if (participant.getHasAnswered()) {
            result.put("success", false);
            result.put("message", "이미 답변을 제출했습니다.");
            return result;
        }

        Song currentSong = room.getCurrentSong();
        if (currentSong == null) {
            result.put("success", false);
            result.put("message", "출제된 노래가 없습니다.");
            return result;
        }

        // 정답 확인
        boolean isCorrect = answerValidationService.validateAnswer(answer, currentSong.getTitle());

        // 점수 계산 (빠르게 맞출수록 높은 점수)
        int earnedScore = 0;
        if (isCorrect) {
            // 먼저 맞춘 순서에 따라 점수 차등
            long answeredCount = participantRepository.findActiveParticipants(room).stream()
                    .filter(p -> p.getHasAnswered() && p.getCurrentRoundCorrect())
                    .count();

            if (answeredCount == 0) {
                earnedScore = 100;  // 1등
            } else if (answeredCount == 1) {
                earnedScore = 80;   // 2등
            } else if (answeredCount == 2) {
                earnedScore = 60;   // 3등
            } else {
                earnedScore = 50;   // 4등 이하
            }
        }

        // 답변 저장
        participant.submitAnswer(answer, isCorrect, earnedScore);

        result.put("success", true);
        result.put("isCorrect", isCorrect);
        result.put("earnedScore", earnedScore);
        result.put("totalScore", participant.getScore());

        // 모든 참가자가 답변했는지 확인
        boolean allAnswered = checkAllAnswered(room);
        result.put("allAnswered", allAnswered);

        return result;
    }

    /**
     * 라운드 결과 보기로 전환
     */
    @Transactional
    public void showRoundResult(GameRoom room) {
        room.setRoundPhase(GameRoom.RoundPhase.RESULT);
    }

    /**
     * 다음 라운드 또는 게임 종료
     */
    @Transactional
    public Map<String, Object> proceedToNext(GameRoom room) {
        Map<String, Object> result = new HashMap<>();

        if (room.getCurrentRound() >= room.getTotalRounds()) {
            // 게임 종료
            room.setStatus(GameRoom.RoomStatus.FINISHED);
            room.setRoundPhase(null);
            result.put("isGameOver", true);
        } else {
            // 다음 라운드
            String gameMode = getGameMode(room);
            if ("GENRE_PER_ROUND".equals(gameMode)) {
                room.setRoundPhase(GameRoom.RoundPhase.GENRE_SELECT);
            } else {
                startNextRound(room, null);
            }
            result.put("isGameOver", false);
        }

        return result;
    }

    /**
     * 모든 참가자가 답변했는지 확인
     */
    public boolean checkAllAnswered(GameRoom room) {
        List<GameRoomParticipant> participants = participantRepository.findActiveParticipants(room);
        return participants.stream()
                .filter(p -> p.getStatus() == GameRoomParticipant.ParticipantStatus.PLAYING)
                .allMatch(GameRoomParticipant::getHasAnswered);
    }

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

        Song currentSong = room.getCurrentSong();
        if (currentSong != null && room.getRoundPhase() == GameRoom.RoundPhase.PLAYING) {
            Map<String, Object> songInfo = new HashMap<>();
            songInfo.put("id", currentSong.getId());
            songInfo.put("filePath", currentSong.getFilePath());
            songInfo.put("startTime", currentSong.getStartTime());
            songInfo.put("playDuration", currentSong.getPlayDuration());
            info.put("song", songInfo);
        }

        // 라운드 결과일 때 정답 정보
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

        // 참가자별 상태
        List<GameRoomParticipant> participants = participantRepository.findActiveParticipants(room);
        List<Map<String, Object>> participantInfos = new ArrayList<>();
        for (GameRoomParticipant p : participants) {
            Map<String, Object> pInfo = new HashMap<>();
            pInfo.put("memberId", p.getMember().getId());
            pInfo.put("nickname", p.getMember().getNickname());
            pInfo.put("score", p.getScore());
            pInfo.put("correctCount", p.getCorrectCount());
            pInfo.put("hasAnswered", p.getHasAnswered());
            pInfo.put("isHost", room.isHost(p.getMember()));

            // 결과 단계에서만 답변 정보 공개
            if (room.getRoundPhase() == GameRoom.RoundPhase.RESULT) {
                pInfo.put("currentAnswer", p.getCurrentAnswer());
                pInfo.put("currentRoundCorrect", p.getCurrentRoundCorrect());
                pInfo.put("currentRoundScore", p.getCurrentRoundScore());
            }

            participantInfos.add(pInfo);
        }

        // 점수순 정렬
        participantInfos.sort((a, b) -> (Integer) b.get("score") - (Integer) a.get("score"));
        info.put("participants", participantInfos);

        return info;
    }

    /**
     * 오디오 재생 (방장만)
     */
    @Transactional
    public void playAudio(GameRoom room, Member host) {
        if (!room.isHost(host)) {
            throw new IllegalStateException("방장만 오디오를 컨트롤할 수 있습니다.");
        }
        room.setAudioPlaying(true);
        room.setAudioPlayedAt(System.currentTimeMillis());
    }

    /**
     * 오디오 일시정지 (방장만)
     */
    @Transactional
    public void pauseAudio(GameRoom room, Member host) {
        if (!room.isHost(host)) {
            throw new IllegalStateException("방장만 오디오를 컨트롤할 수 있습니다.");
        }
        room.setAudioPlaying(false);
        room.setAudioPlayedAt(null);
    }

    /**
     * 오디오 상태 초기화 (라운드 변경 시)
     */
    @Transactional
    public void resetAudioState(GameRoom room) {
        room.setAudioPlaying(false);
        room.setAudioPlayedAt(null);
    }

    /**
     * 장르별 남은 노래 수 조회
     */
    public List<Map<String, Object>> getGenresWithCount(GameRoom room) {
        Set<Long> usedSongs = usedSongsByRoom.getOrDefault(room.getId(), new HashSet<>());
        List<Genre> genres = genreService.findActiveGenres();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Genre genre : genres) {
            int count = songService.getAvailableCountByGenreExcluding(genre.getId(), usedSongs);

            Map<String, Object> genreInfo = new HashMap<>();
            genreInfo.put("id", genre.getId());
            genreInfo.put("name", genre.getName());
            genreInfo.put("availableCount", count);
            result.add(genreInfo);
        }

        // 남은 곡 수 내림차순 정렬
        result.sort((a, b) -> (Integer) b.get("availableCount") - (Integer) a.get("availableCount"));

        return result;
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

    /**
     * 노래 선택
     */
    private Song selectSong(GameRoom room, Long genreId) {
        Set<Long> usedSongs = usedSongsByRoom.getOrDefault(room.getId(), new HashSet<>());
        String gameMode = getGameMode(room);

        Long targetGenreId = genreId;
        if ("FIXED_GENRE".equals(gameMode) && genreId == null) {
            targetGenreId = getFixedGenreId(room);
        }

        return songService.getRandomSongExcluding(targetGenreId, usedSongs);
    }

    /**
     * 게임 모드 조회
     */
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

    /**
     * 고정 장르 ID 조회
     */
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
}