package com.kh.game.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kh.game.entity.*;
import com.kh.game.repository.FanChallengeRecordRepository;
import com.kh.game.repository.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FanChallengeService {

    private final SongService songService;
    private final GameSessionRepository gameSessionRepository;
    private final FanChallengeRecordRepository fanChallengeRecordRepository;
    private final ObjectMapper objectMapper;

    // 한글 초성 배열
    private static final char[] CHOSUNG = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    /**
     * 팬 챌린지 게임 시작 (기본 난이도: NORMAL)
     */
    @Transactional
    public GameSession startChallenge(Member member, String nickname, String artist) {
        return startChallenge(member, nickname, artist, FanChallengeDifficulty.NORMAL);
    }

    /**
     * 팬 챌린지 게임 시작 (난이도 지정)
     */
    @Transactional
    public GameSession startChallenge(Member member, String nickname, String artist, FanChallengeDifficulty difficulty) {
        // 아티스트의 모든 곡 가져오기 (YouTube 검증 포함)
        List<Song> songs = songService.getAllValidatedSongsByArtist(artist);

        if (songs.isEmpty()) {
            throw new IllegalArgumentException("해당 아티스트의 유효한 곡이 없습니다: " + artist);
        }

        // 게임 세션 생성
        GameSession session = new GameSession();
        session.setSessionUuid(UUID.randomUUID().toString());
        session.setMember(member);
        session.setNickname(nickname);
        session.setGameType(GameSession.GameType.FAN_CHALLENGE);
        session.setGameMode(GameSession.GameMode.FIXED_ARTIST);
        session.setTotalRounds(songs.size());
        session.setCompletedRounds(0);
        session.setTotalScore(0);
        session.setCorrectCount(0);
        session.setSkipCount(0);
        session.setRemainingLives(difficulty.getInitialLives());
        session.setChallengeArtist(artist);
        session.setStatus(GameSession.GameStatus.PLAYING);

        // 난이도 설정을 JSON으로 저장
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("difficulty", difficulty.name());
            settings.put("playTimeMs", difficulty.getPlayTimeMs());
            settings.put("answerTimeMs", difficulty.getAnswerTimeMs());
            settings.put("initialLives", difficulty.getInitialLives());
            settings.put("showChosungHint", difficulty.isShowChosungHint());
            session.setSettings(objectMapper.writeValueAsString(settings));
        } catch (JsonProcessingException e) {
            log.warn("게임 설정 JSON 변환 실패", e);
        }

        GameSession savedSession = gameSessionRepository.save(session);

        // 라운드 생성
        for (int i = 0; i < songs.size(); i++) {
            GameRound round = new GameRound();
            round.setGameSession(savedSession);
            round.setRoundNumber(i + 1);
            round.setSong(songs.get(i));
            round.setGenre(songs.get(i).getGenre());
            round.setPlayStartTime(songs.get(i).getStartTime());
            round.setPlayDuration(songs.get(i).getPlayDuration());
            round.setStatus(GameRound.RoundStatus.WAITING);
            round.setAttemptCount(0);
            round.setScore(0);
            savedSession.getRounds().add(round);
        }

        return gameSessionRepository.save(savedSession);
    }

    /**
     * 정답 처리
     */
    @Transactional
    public AnswerResult processAnswer(Long sessionId, int roundNumber, String answer, long answerTimeMs) {
        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다"));

        if (session.getStatus() != GameSession.GameStatus.PLAYING) {
            throw new IllegalStateException("게임이 진행 중이 아닙니다");
        }

        GameRound round = session.getRounds().stream()
                .filter(r -> r.getRoundNumber() == roundNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("라운드를 찾을 수 없습니다"));

        // 난이도 설정에서 시간 제한 가져오기
        FanChallengeDifficulty difficulty = getDifficultyFromSession(session);
        long timeLimit = difficulty.getTotalTimeMs();

        // 시간 초과 체크 (서버 측 검증)
        boolean isTimeout = answerTimeMs > timeLimit;
        boolean isCorrect = false;

        if (!isTimeout && answer != null && !answer.trim().isEmpty()) {
            isCorrect = songService.checkAnswer(round.getSong().getId(), answer);
        }

        // 라운드 업데이트
        round.setUserAnswer(answer);
        round.setAnswerTimeMs(answerTimeMs);
        round.setAttemptCount(1);

        if (isTimeout) {
            round.setStatus(GameRound.RoundStatus.TIMEOUT);
            round.setIsCorrect(false);
            round.setScore(0);
            session.setRemainingLives(session.getRemainingLives() - 1);
        } else if (isCorrect) {
            round.setStatus(GameRound.RoundStatus.ANSWERED);
            round.setIsCorrect(true);
            round.setScore(1);
            session.setCorrectCount(session.getCorrectCount() + 1);
            session.setTotalScore(session.getTotalScore() + 1);
        } else {
            round.setStatus(GameRound.RoundStatus.ANSWERED);
            round.setIsCorrect(false);
            round.setScore(0);
            session.setRemainingLives(session.getRemainingLives() - 1);
        }

        session.setCompletedRounds(session.getCompletedRounds() + 1);

        // 게임 종료 체크
        boolean isGameOver = false;
        String gameOverReason = null;

        if (session.getRemainingLives() <= 0) {
            isGameOver = true;
            gameOverReason = "LIFE_EXHAUSTED";
            session.setStatus(GameSession.GameStatus.COMPLETED);
            session.setEndedAt(LocalDateTime.now());
        } else if (session.getCompletedRounds() >= session.getTotalRounds()) {
            isGameOver = true;
            // 모든 문제를 맞춰야만 PERFECT_CLEAR
            if (session.getCorrectCount().equals(session.getTotalRounds())) {
                gameOverReason = "PERFECT_CLEAR";
            } else {
                gameOverReason = "ALL_ROUNDS_COMPLETED";
            }
            session.setStatus(GameSession.GameStatus.COMPLETED);
            session.setEndedAt(LocalDateTime.now());
        }

        gameSessionRepository.save(session);

        // 게임 종료 시 기록 업데이트
        if (isGameOver && session.getMember() != null) {
            updateRecord(session, difficulty);
        }

        return new AnswerResult(
                isCorrect,
                isTimeout,
                round.getSong().getTitle(),
                round.getSong().getArtist(),
                session.getRemainingLives(),
                session.getCorrectCount(),
                session.getCompletedRounds(),
                session.getTotalRounds(),
                isGameOver,
                gameOverReason
        );
    }

    /**
     * 시간 초과 처리 (클라이언트에서 호출)
     */
    @Transactional
    public AnswerResult processTimeout(Long sessionId, int roundNumber) {
        GameSession session = gameSessionRepository.findById(sessionId).orElse(null);
        FanChallengeDifficulty difficulty = session != null ? getDifficultyFromSession(session) : FanChallengeDifficulty.NORMAL;
        return processAnswer(sessionId, roundNumber, null, difficulty.getTotalTimeMs() + 1);
    }

    /**
     * 기록 업데이트 (기본 - 하드코어 기준)
     */
    @Transactional
    public FanChallengeRecord updateRecord(GameSession session) {
        return updateRecord(session, FanChallengeDifficulty.HARDCORE);
    }

    /**
     * 기록 업데이트 (난이도별)
     */
    @Transactional
    public FanChallengeRecord updateRecord(GameSession session, FanChallengeDifficulty difficulty) {
        if (session.getMember() == null) {
            return null;
        }

        String artist = session.getChallengeArtist();
        Member member = session.getMember();

        // 난이도별 기록 조회
        Optional<FanChallengeRecord> existingRecord =
                fanChallengeRecordRepository.findByMemberAndArtistAndDifficulty(member, artist, difficulty);

        FanChallengeRecord record;
        if (existingRecord.isPresent()) {
            record = existingRecord.get();
            // 더 좋은 기록인 경우에만 업데이트
            if (session.getCorrectCount() > record.getCorrectCount()) {
                record.setCorrectCount(session.getCorrectCount());
                record.setTotalSongs(session.getTotalRounds());
                record.setAchievedAt(LocalDateTime.now());

                // 퍼펙트 클리어 체크
                if (session.getCorrectCount().equals(session.getTotalRounds())) {
                    record.setIsPerfectClear(true);
                    record.setBestTimeMs(session.getPlayTimeSeconds() * 1000);
                }
            } else if (session.getCorrectCount().equals(record.getCorrectCount())
                    && record.getIsPerfectClear()
                    && session.getPlayTimeSeconds() * 1000 < record.getBestTimeMs()) {
                // 같은 점수인데 시간이 더 빠른 경우
                record.setBestTimeMs(session.getPlayTimeSeconds() * 1000);
                record.setAchievedAt(LocalDateTime.now());
            }
        } else {
            record = new FanChallengeRecord(member, artist, session.getTotalRounds(), difficulty);
            record.setCorrectCount(session.getCorrectCount());
            record.setAchievedAt(LocalDateTime.now());

            if (session.getCorrectCount().equals(session.getTotalRounds())) {
                record.setIsPerfectClear(true);
                record.setBestTimeMs(session.getPlayTimeSeconds() * 1000);
            }
        }

        return fanChallengeRecordRepository.save(record);
    }

    /**
     * 세션에서 난이도 설정 가져오기
     */
    public FanChallengeDifficulty getDifficultyFromSession(GameSession session) {
        if (session.getSettings() == null || session.getSettings().isEmpty()) {
            return FanChallengeDifficulty.NORMAL;
        }
        try {
            Map<String, Object> settings = objectMapper.readValue(session.getSettings(), Map.class);
            String difficultyStr = (String) settings.get("difficulty");
            return FanChallengeDifficulty.fromString(difficultyStr);
        } catch (Exception e) {
            log.warn("게임 설정 파싱 실패", e);
            return FanChallengeDifficulty.NORMAL;
        }
    }

    /**
     * 세션 조회
     */
    public GameSession getSession(Long sessionId) {
        return gameSessionRepository.findById(sessionId).orElse(null);
    }

    /**
     * 아티스트별 랭킹 조회 (하드코어만 - 공식 랭킹)
     */
    public List<FanChallengeRecord> getArtistRanking(String artist, int limit) {
        return fanChallengeRecordRepository.findTopByArtist(artist, PageRequest.of(0, limit));
    }

    /**
     * 난이도별 아티스트 랭킹 조회
     */
    public List<FanChallengeRecord> getArtistRankingByDifficulty(String artist, FanChallengeDifficulty difficulty, int limit) {
        return fanChallengeRecordRepository.findTopByArtistAndDifficulty(artist, difficulty, PageRequest.of(0, limit));
    }

    /**
     * 회원의 특정 아티스트 기록 조회 (기존 호환 - 하드코어)
     */
    public Optional<FanChallengeRecord> getMemberRecord(Member member, String artist) {
        return fanChallengeRecordRepository.findByMemberAndArtistAndDifficulty(member, artist, FanChallengeDifficulty.HARDCORE);
    }

    /**
     * 회원의 특정 아티스트 난이도별 기록 조회
     */
    public Optional<FanChallengeRecord> getMemberRecord(Member member, String artist, FanChallengeDifficulty difficulty) {
        return fanChallengeRecordRepository.findByMemberAndArtistAndDifficulty(member, artist, difficulty);
    }

    /**
     * 회원의 특정 아티스트 퍼펙트 클리어 뱃지 목록 조회
     */
    public List<FanChallengeRecord> getPerfectBadges(Member member, String artist) {
        return fanChallengeRecordRepository.findPerfectBadgesByMemberAndArtist(member, artist);
    }

    /**
     * 초성 힌트 생성
     * 한글: 초성 추출 (봄날 → ㅂㄴ)
     * 영어: 첫 글자만 + * (Dynamite → D*******)
     * 숫자/특수문자: 그대로 표시
     */
    public String extractChosung(String title) {
        if (title == null || title.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (char c : title.toCharArray()) {
            if (c >= '가' && c <= '힣') {
                // 한글: 초성 추출
                int index = (c - '가') / 588;
                result.append(CHOSUNG[index]);
            } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                // 영어: 첫 글자만 대문자, 나머지는 *
                if (result.length() == 0 || result.charAt(result.length() - 1) == ' ') {
                    result.append(Character.toUpperCase(c));
                } else {
                    result.append('*');
                }
            } else if (c == ' ') {
                // 공백 유지
                result.append(' ');
            } else if (Character.isDigit(c)) {
                // 숫자 유지
                result.append(c);
            } else {
                // 특수문자는 숨김
                result.append('*');
            }
        }
        return result.toString();
    }

    /**
     * 정답 결과 DTO
     */
    public record AnswerResult(
            boolean isCorrect,
            boolean isTimeout,
            String correctAnswer,
            String artist,
            int remainingLives,
            int correctCount,
            int completedRounds,
            int totalRounds,
            boolean isGameOver,
            String gameOverReason
    ) {}
}
