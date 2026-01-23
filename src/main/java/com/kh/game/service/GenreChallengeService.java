package com.kh.game.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kh.game.entity.*;
import com.kh.game.repository.GenreChallengeRecordRepository;
import com.kh.game.repository.GenreRepository;
import com.kh.game.repository.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 장르 챌린지 서비스
 * - 장르별 전곡 도전 모드
 * - 콤보 및 정답수 기록
 * - 영구 랭킹 (리셋 없음)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GenreChallengeService {

    private final SongService songService;
    private final GameSessionRepository gameSessionRepository;
    private final GenreChallengeRecordRepository genreChallengeRecordRepository;
    private final GenreRepository genreRepository;
    private final ObjectMapper objectMapper;

    // 최소 곡 수 (10곡 이상)
    public static final int MIN_SONG_COUNT = 10;

    /**
     * 장르 챌린지 게임 시작 (기본 난이도: NORMAL)
     */
    @Transactional
    public GameSession startChallenge(Member member, String nickname, String genreCode) {
        return startChallenge(member, nickname, genreCode, GenreChallengeDifficulty.NORMAL);
    }

    /**
     * 장르 챌린지 게임 시작 (난이도 지정)
     */
    @Transactional
    public GameSession startChallenge(Member member, String nickname, String genreCode, GenreChallengeDifficulty difficulty) {
        // 장르의 모든 유효한 곡 가져오기 (YouTube 검증 포함)
        List<Song> allSongs = songService.getAllValidatedSongsByGenreCode(genreCode);

        if (allSongs.size() < MIN_SONG_COUNT) {
            throw new IllegalArgumentException(
                String.format("장르 챌린지는 %d곡 이상의 장르만 가능합니다. (현재 %d곡)",
                    MIN_SONG_COUNT, allSongs.size()));
        }

        // 게임 세션 생성
        GameSession session = new GameSession();
        session.setSessionUuid(UUID.randomUUID().toString());
        session.setMember(member);
        session.setNickname(nickname);
        session.setGameType(GameSession.GameType.GENRE_CHALLENGE);
        session.setGameMode(GameSession.GameMode.FIXED_GENRE);
        session.setTotalRounds(allSongs.size());
        session.setCompletedRounds(0);
        session.setTotalScore(0);
        session.setCorrectCount(0);
        session.setSkipCount(0);
        session.setRemainingLives(difficulty.getInitialLives());
        session.setChallengeGenreCode(genreCode);
        session.setCurrentCombo(0);
        session.setMaxCombo(0);
        session.setStatus(GameSession.GameStatus.PLAYING);

        // 난이도 설정을 JSON으로 저장
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("difficulty", difficulty.name());
            settings.put("totalTimeMs", difficulty.getTotalTimeMs());
            settings.put("initialLives", difficulty.getInitialLives());
            session.setSettings(objectMapper.writeValueAsString(settings));
        } catch (JsonProcessingException e) {
            log.warn("게임 설정 JSON 변환 실패", e);
        }

        GameSession savedSession = gameSessionRepository.save(session);

        // 라운드 생성 (모든 곡)
        for (int i = 0; i < allSongs.size(); i++) {
            GameRound round = new GameRound();
            round.setGameSession(savedSession);
            round.setRoundNumber(i + 1);
            round.setSong(allSongs.get(i));
            round.setGenre(allSongs.get(i).getGenre());
            round.setPlayStartTime(allSongs.get(i).getStartTime());
            round.setPlayDuration(allSongs.get(i).getPlayDuration());
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
        GenreChallengeDifficulty difficulty = getDifficultyFromSession(session);
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

        int currentCombo = session.getCurrentCombo() != null ? session.getCurrentCombo() : 0;
        int maxCombo = session.getMaxCombo() != null ? session.getMaxCombo() : 0;

        if (isTimeout) {
            round.setStatus(GameRound.RoundStatus.TIMEOUT);
            round.setIsCorrect(false);
            round.setScore(0);
            session.setRemainingLives(session.getRemainingLives() - 1);
            session.setCurrentCombo(0); // 콤보 리셋
        } else if (isCorrect) {
            round.setStatus(GameRound.RoundStatus.ANSWERED);
            round.setIsCorrect(true);
            round.setScore(1);
            session.setCorrectCount(session.getCorrectCount() + 1);
            session.setTotalScore(session.getTotalScore() + 1);
            currentCombo++;
            session.setCurrentCombo(currentCombo);
            if (currentCombo > maxCombo) {
                session.setMaxCombo(currentCombo);
                maxCombo = currentCombo;
            }
        } else {
            round.setStatus(GameRound.RoundStatus.ANSWERED);
            round.setIsCorrect(false);
            round.setScore(0);
            session.setRemainingLives(session.getRemainingLives() - 1);
            session.setCurrentCombo(0); // 콤보 리셋
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
            gameOverReason = "ALL_ROUNDS_COMPLETED";
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
                session.getCurrentCombo() != null ? session.getCurrentCombo() : 0,
                session.getMaxCombo() != null ? session.getMaxCombo() : 0,
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
        GenreChallengeDifficulty difficulty = session != null ? getDifficultyFromSession(session) : GenreChallengeDifficulty.NORMAL;
        return processAnswer(sessionId, roundNumber, null, difficulty.getTotalTimeMs() + 1);
    }

    /**
     * 기록 업데이트
     */
    @Transactional
    public GenreChallengeRecord updateRecord(GameSession session, GenreChallengeDifficulty difficulty) {
        if (session.getMember() == null) {
            return null;
        }

        String genreCode = session.getChallengeGenreCode();
        Member member = session.getMember();

        // 장르 엔티티 조회
        Genre genre = genreRepository.findByCode(genreCode).orElse(null);
        if (genre == null) {
            log.warn("장르를 찾을 수 없습니다: {}", genreCode);
            return null;
        }

        // 난이도별 기록 조회
        Optional<GenreChallengeRecord> existingRecord =
                genreChallengeRecordRepository.findByMemberAndGenreAndDifficulty(member, genre, difficulty);

        GenreChallengeRecord record;
        long currentTimeMs = session.getPlayTimeSeconds() * 1000;
        int currentCorrectCount = session.getCorrectCount() != null ? session.getCorrectCount() : 0;
        int currentMaxCombo = session.getMaxCombo() != null ? session.getMaxCombo() : 0;

        if (existingRecord.isPresent()) {
            record = existingRecord.get();
            boolean updated = false;

            // 정답수 증가 시 갱신
            if (currentCorrectCount > record.getCorrectCount()) {
                record.setCorrectCount(currentCorrectCount);
                record.setTotalSongs(session.getTotalRounds());
                record.setBestTimeMs(currentTimeMs);
                record.setAchievedAt(LocalDateTime.now());
                updated = true;
            } else if (currentCorrectCount == record.getCorrectCount()) {
                // 동점일 때 시간이 더 빠르면 갱신
                if (record.getBestTimeMs() == null || currentTimeMs < record.getBestTimeMs()) {
                    record.setBestTimeMs(currentTimeMs);
                    record.setAchievedAt(LocalDateTime.now());
                    updated = true;
                }
            }

            // maxCombo는 항상 더 높으면 갱신
            if (currentMaxCombo > record.getMaxCombo()) {
                record.setMaxCombo(currentMaxCombo);
                if (!updated) {
                    record.setAchievedAt(LocalDateTime.now());
                }
            }
        } else {
            record = new GenreChallengeRecord(member, genre, session.getTotalRounds(), difficulty);
            record.setCorrectCount(currentCorrectCount);
            record.setMaxCombo(currentMaxCombo);
            record.setBestTimeMs(currentTimeMs);
            record.setAchievedAt(LocalDateTime.now());
        }

        return genreChallengeRecordRepository.save(record);
    }

    /**
     * 세션에서 난이도 설정 가져오기
     */
    public GenreChallengeDifficulty getDifficultyFromSession(GameSession session) {
        if (session.getSettings() == null || session.getSettings().isEmpty()) {
            return GenreChallengeDifficulty.NORMAL;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> settings = objectMapper.readValue(session.getSettings(), Map.class);
            String difficultyStr = (String) settings.get("difficulty");
            return GenreChallengeDifficulty.fromString(difficultyStr);
        } catch (Exception e) {
            log.warn("게임 설정 파싱 실패", e);
            return GenreChallengeDifficulty.NORMAL;
        }
    }

    /**
     * 세션 조회 (rounds와 song 포함)
     */
    @Transactional(readOnly = true)
    public GameSession getSession(Long sessionId) {
        log.info("getSession 호출: sessionId={}", sessionId);
        GameSession session = gameSessionRepository.findByIdWithRounds(sessionId).orElse(null);
        if (session != null) {
            // rounds와 song 강제 초기화 (Lazy Loading 방지)
            if (session.getRounds() != null) {
                session.getRounds().forEach(round -> {
                    if (round.getSong() != null) {
                        round.getSong().getTitle(); // 강제 초기화
                    }
                });
            }
            log.info("세션 조회 성공: rounds.size={}", session.getRounds().size());
        } else {
            log.warn("세션 조회 실패: sessionId={}", sessionId);
        }
        return session;
    }

    /**
     * 장르별 랭킹 조회 (하드코어만 - 공식 랭킹)
     */
    public List<GenreChallengeRecord> getGenreRanking(Genre genre, int limit) {
        return genreChallengeRecordRepository.findTopByGenre(genre, PageRequest.of(0, limit));
    }

    /**
     * 장르 코드로 랭킹 조회 (하드코어만 - 공식 랭킹)
     */
    public List<GenreChallengeRecord> getGenreRankingByCode(String genreCode, int limit) {
        return genreChallengeRecordRepository.findTopByGenreCode(genreCode, PageRequest.of(0, limit));
    }

    /**
     * 난이도별 장르 랭킹 조회
     */
    public List<GenreChallengeRecord> getGenreRankingByDifficulty(Genre genre, GenreChallengeDifficulty difficulty, int limit) {
        return genreChallengeRecordRepository.findTopByGenreAndDifficulty(genre, difficulty, PageRequest.of(0, limit));
    }

    /**
     * 회원의 특정 장르 기록 조회 (하드코어)
     */
    public Optional<GenreChallengeRecord> getMemberRecord(Member member, Genre genre) {
        return genreChallengeRecordRepository.findByMemberAndGenreAndDifficulty(member, genre, GenreChallengeDifficulty.HARDCORE);
    }

    /**
     * 회원의 특정 장르 난이도별 기록 조회
     */
    public Optional<GenreChallengeRecord> getMemberRecord(Member member, Genre genre, GenreChallengeDifficulty difficulty) {
        return genreChallengeRecordRepository.findByMemberAndGenreAndDifficulty(member, genre, difficulty);
    }

    /**
     * 장르 정보 조회 (곡 수 포함)
     */
    public Map<String, Object> getGenreInfo(String genreCode) {
        Map<String, Object> info = new HashMap<>();

        Genre genre = genreRepository.findByCode(genreCode).orElse(null);
        if (genre == null) {
            return info;
        }

        info.put("code", genre.getCode());
        info.put("name", genre.getName());
        info.put("songCount", songService.getSongCountByGenreCode(genreCode));

        return info;
    }

    /**
     * 홈 페이지용 장르 TOP1 기록 조회 (HARDCORE 기록이 있는 모든 장르)
     * 정렬: correctCount DESC → bestTimeMs ASC
     */
    public List<Map<String, Object>> getTopGenresWithTopRecord() {
        // 1. HARDCORE 기록이 있는 모든 장르 코드 목록
        List<String> allGenreCodes = genreChallengeRecordRepository.findAllGenreCodesWithHardcoreRecords();
        log.info("[GenreTop] HARDCORE 기록이 있는 장르: {}개", allGenreCodes.size());

        // 2. 장르 코드 → 이름 맵
        Map<String, String> genreNameMap = new HashMap<>();
        for (Genre g : genreRepository.findAll()) {
            genreNameMap.put(g.getCode(), g.getName());
        }

        // 3. 각 장르별 1위 기록 조회
        List<Map<String, Object>> result = new ArrayList<>();
        for (String genreCode : allGenreCodes) {
            List<GenreChallengeRecord> topRecords = genreChallengeRecordRepository
                    .findTopByGenreCodeAndHardcore(genreCode, org.springframework.data.domain.PageRequest.of(0, 1));

            if (!topRecords.isEmpty()) {
                GenreChallengeRecord top = topRecords.get(0);
                Map<String, Object> item = new HashMap<>();
                item.put("genreCode", genreCode);
                item.put("genreName", genreNameMap.getOrDefault(genreCode, genreCode));
                item.put("nickname", top.getMember().getNickname());
                item.put("correctCount", top.getCorrectCount());
                item.put("totalSongs", top.getTotalSongs());
                item.put("maxCombo", top.getMaxCombo());
                item.put("bestTimeMs", top.getBestTimeMs());
                result.add(item);
            }
        }

        // 4. 정렬: correctCount DESC → bestTimeMs ASC (null은 뒤로)
        result.sort((a, b) -> {
            int countA = (Integer) a.get("correctCount");
            int countB = (Integer) b.get("correctCount");
            if (countB != countA) {
                return countB - countA;  // DESC
            }
            Long timeA = (Long) a.get("bestTimeMs");
            Long timeB = (Long) b.get("bestTimeMs");
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;  // null은 뒤로
            if (timeB == null) return -1;
            return Long.compare(timeA, timeB);  // ASC
        });

        log.info("[GenreTop] 최종 결과: {}개 장르 반환", result.size());
        return result;
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
            int currentCombo,
            int maxCombo,
            boolean isGameOver,
            String gameOverReason
    ) {}
}
