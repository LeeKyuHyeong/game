package com.kh.game.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kh.game.entity.*;
import com.kh.game.repository.FanChallengeRecordRepository;
import com.kh.game.repository.FanChallengeStageConfigRepository;
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
    private final FanChallengeStageConfigRepository stageConfigRepository;
    private final ObjectMapper objectMapper;
    private final BadgeService badgeService;

    // 챌린지 곡 수 (20곡 고정)
    public static final int CHALLENGE_SONG_COUNT = 20;

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
        // NORMAL 모드는 항상 1단계 (20곡)
        return startChallenge(member, nickname, artist, difficulty, 1);
    }

    /**
     * 팬 챌린지 게임 시작 (난이도 + 단계 지정)
     * - HARDCORE 모드에서만 단계 시스템 적용
     * - NORMAL 모드는 stageLevel 무시하고 항상 20곡
     */
    @Transactional
    public GameSession startChallenge(Member member, String nickname, String artist,
                                       FanChallengeDifficulty difficulty, Integer stageLevel) {
        // NORMAL 모드는 항상 20곡 고정
        int requiredSongs;
        int effectiveStageLevel;

        if (difficulty == FanChallengeDifficulty.NORMAL) {
            requiredSongs = CHALLENGE_SONG_COUNT;
            effectiveStageLevel = 1;
        } else {
            // HARDCORE 모드: 단계별 곡 수 적용
            effectiveStageLevel = (stageLevel != null && stageLevel > 0) ? stageLevel : 1;

            // 단계 설정 조회
            FanChallengeStageConfig stageConfig = stageConfigRepository.findByStageLevel(effectiveStageLevel)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 단계입니다: " + effectiveStageLevel));

            // 단계 활성화 확인
            if (!Boolean.TRUE.equals(stageConfig.getIsActive())) {
                throw new IllegalArgumentException("아직 개방되지 않은 단계입니다: " + effectiveStageLevel + "단계");
            }

            requiredSongs = stageConfig.getRequiredSongs();
        }

        // 아티스트의 모든 곡 가져오기 (YouTube 검증 포함)
        List<Song> allSongs = songService.getAllValidatedSongsByArtist(artist);

        if (allSongs.size() < requiredSongs) {
            throw new IllegalArgumentException(
                String.format("이 아티스트는 %d단계(%d곡)에 도전할 수 없습니다. (현재 %d곡)",
                    effectiveStageLevel, requiredSongs, allSongs.size()));
        }

        // requiredSongs 곡 랜덤 선택
        List<Song> songs = new ArrayList<>(allSongs);
        Collections.shuffle(songs);
        songs = songs.subList(0, requiredSongs);

        // 게임 세션 생성
        GameSession session = new GameSession();
        session.setSessionUuid(UUID.randomUUID().toString());
        session.setMember(member);
        session.setNickname(nickname);
        session.setGameType(GameSession.GameType.FAN_CHALLENGE);
        session.setGameMode(GameSession.GameMode.FIXED_ARTIST);
        session.setTotalRounds(requiredSongs);
        session.setCompletedRounds(0);
        session.setTotalScore(0);
        session.setCorrectCount(0);
        session.setSkipCount(0);
        session.setRemainingLives(difficulty.getInitialLives());
        session.setChallengeArtist(artist);
        session.setStatus(GameSession.GameStatus.PLAYING);

        // 난이도 설정을 JSON으로 저장 (stageLevel 포함)
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("difficulty", difficulty.name());
            settings.put("playTimeMs", difficulty.getPlayTimeMs());
            settings.put("answerTimeMs", difficulty.getAnswerTimeMs());
            settings.put("initialLives", difficulty.getInitialLives());
            settings.put("showChosungHint", difficulty.isShowChosungHint());
            settings.put("stageLevel", effectiveStageLevel);
            settings.put("requiredSongs", requiredSongs);
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
        // 세션에서 stageLevel 추출
        int stageLevel = getStageLevelFromSession(session);
        return updateRecord(session, difficulty, stageLevel);
    }

    /**
     * 기록 업데이트 (난이도 + 단계별)
     */
    @Transactional
    public FanChallengeRecord updateRecord(GameSession session, FanChallengeDifficulty difficulty, int stageLevel) {
        if (session.getMember() == null) {
            return null;
        }

        String artist = session.getChallengeArtist();
        Member member = session.getMember();

        // 난이도 + 단계별 기록 조회
        Optional<FanChallengeRecord> existingRecord =
                fanChallengeRecordRepository.findByMemberAndArtistAndDifficultyAndStageLevel(
                        member, artist, difficulty, stageLevel);

        FanChallengeRecord record;
        boolean isNewPerfectClear = false;

        long currentTimeMs = session.getPlayTimeSeconds() * 1000;

        if (existingRecord.isPresent()) {
            record = existingRecord.get();
            boolean wasPerfect = Boolean.TRUE.equals(record.getIsPerfectClear());

            // 더 좋은 기록인 경우 업데이트
            if (session.getCorrectCount() > record.getCorrectCount()) {
                record.setCorrectCount(session.getCorrectCount());
                record.setTotalSongs(session.getTotalRounds());
                record.setBestTimeMs(currentTimeMs);
                record.setAchievedAt(LocalDateTime.now());

                // 퍼펙트 클리어 체크
                if (session.getCorrectCount().equals(session.getTotalRounds())) {
                    record.setIsPerfectClear(true);
                    record.setIsCurrentPerfect(true);
                    if (!wasPerfect) {
                        isNewPerfectClear = true;
                    }
                }
            } else if (session.getCorrectCount().equals(record.getCorrectCount())) {
                // 동점일 때 시간이 더 빠르면 갱신
                if (record.getBestTimeMs() == null || currentTimeMs < record.getBestTimeMs()) {
                    record.setBestTimeMs(currentTimeMs);
                    record.setAchievedAt(LocalDateTime.now());
                }
            }
        } else {
            record = new FanChallengeRecord(member, artist, session.getTotalRounds(), difficulty, stageLevel);
            record.setCorrectCount(session.getCorrectCount());
            record.setBestTimeMs(currentTimeMs);
            record.setAchievedAt(LocalDateTime.now());

            if (session.getCorrectCount().equals(session.getTotalRounds())) {
                record.setIsPerfectClear(true);
                record.setIsCurrentPerfect(true);
                isNewPerfectClear = true;
            }
        }

        FanChallengeRecord savedRecord = fanChallengeRecordRepository.save(record);

        // 퍼펙트 클리어 시 뱃지 체크
        if (isNewPerfectClear) {
            // 기존 마일스톤 뱃지
            List<Badge> newBadges = badgeService.checkBadgesAfterFanChallengePerfect(member, difficulty);

            // 아티스트별 단계 뱃지 (NORMAL: 1단계만, HARDCORE: 모든 단계)
            // NORMAL은 stageLevel 무관하게 항상 1단계 배지만 부여
            int effectiveStageLevel = (difficulty == FanChallengeDifficulty.NORMAL) ? 1 : stageLevel;
            Badge stageBadge = badgeService.awardStageBadge(member, artist, difficulty, effectiveStageLevel);
            if (stageBadge != null) {
                newBadges.add(stageBadge);
            }

            if (!newBadges.isEmpty()) {
                log.info("팬챌린지 퍼펙트 뱃지 획득: {} -> {} (난이도: {})",
                    member.getNickname(),
                    newBadges.stream().map(Badge::getName).toList(),
                    difficulty.getDisplayName());
            }
        }

        return savedRecord;
    }

    /**
     * 세션에서 단계 레벨 추출
     */
    public int getStageLevelFromSession(GameSession session) {
        if (session.getSettings() == null || session.getSettings().isEmpty()) {
            return 1;
        }
        try {
            Map<String, Object> settings = objectMapper.readValue(session.getSettings(), Map.class);
            Object stageLevel = settings.get("stageLevel");
            if (stageLevel instanceof Number) {
                return ((Number) stageLevel).intValue();
            }
        } catch (Exception e) {
            log.warn("stageLevel 파싱 실패", e);
        }
        return 1;
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
     * 회원의 특정 아티스트 난이도+단계별 기록 조회
     */
    public Optional<FanChallengeRecord> getMemberRecord(Member member, String artist,
                                                         FanChallengeDifficulty difficulty, int stageLevel) {
        return fanChallengeRecordRepository.findByMemberAndArtistAndDifficultyAndStageLevel(
                member, artist, difficulty, stageLevel);
    }

    /**
     * 회원의 특정 아티스트 전체 단계 기록 조회 (HARDCORE)
     */
    public List<FanChallengeRecord> getMemberRecordAllStages(Member member, String artist) {
        return fanChallengeRecordRepository.findByMemberAndArtistAllStages(member, artist);
    }

    /**
     * 아티스트 + 단계별 랭킹 조회 (HARDCORE 전용)
     */
    public List<FanChallengeRecord> getArtistStageRanking(String artist, int stageLevel, int limit) {
        return fanChallengeRecordRepository.findTopByArtistAndStage(artist, stageLevel, PageRequest.of(0, limit));
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
     * 홈 페이지용 아티스트 TOP1 기록 조회 (HARDCORE 기본 stageLevel=1)
     * 정렬: correctCount DESC → bestTimeMs ASC
     */
    public List<Map<String, Object>> getTopArtistsWithTopRecord() {
        return getTopArtistsWithTopRecord(1);
    }

    /**
     * 홈 페이지용 아티스트 TOP1 기록 조회 (HARDCORE 단계별)
     * 정렬: correctCount DESC → bestTimeMs ASC
     */
    public List<Map<String, Object>> getTopArtistsWithTopRecord(int stageLevel) {
        // 1. HARDCORE 해당 stageLevel 기록이 있는 모든 아티스트 목록
        List<String> allArtists = fanChallengeRecordRepository.findAllArtistsWithStageRecords(stageLevel);

        // 2. 아티스트별 곡 수 맵 (표시용)
        Map<String, Integer> artistSongCountMap = new HashMap<>();
        for (Map<String, Object> artistInfo : songService.getArtistsWithCountForFanChallenge()) {
            artistSongCountMap.put((String) artistInfo.get("name"), (Integer) artistInfo.get("count"));
        }

        // 3. 각 아티스트별 1위 기록 조회
        List<Map<String, Object>> result = new ArrayList<>();
        for (String artist : allArtists) {
            List<FanChallengeRecord> topRecords = fanChallengeRecordRepository.findTopByArtistAndStage(artist, stageLevel, PageRequest.of(0, 1));

            if (!topRecords.isEmpty()) {
                FanChallengeRecord top = topRecords.get(0);
                Map<String, Object> item = new HashMap<>();
                item.put("artist", artist);
                item.put("nickname", top.getMember().getNickname());
                item.put("correctCount", top.getCorrectCount());
                item.put("totalSongs", top.getTotalSongs());
                item.put("isPerfectClear", top.getIsPerfectClear());
                item.put("bestTimeMs", top.getBestTimeMs());
                item.put("songCount", artistSongCountMap.getOrDefault(artist, 0));
                item.put("stageLevel", stageLevel);
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
            boolean isGameOver,
            String gameOverReason
    ) {}

    // ========== 이력 기반 랭킹 메서드 ==========

    /**
     * 기록의 랭킹 데이터 조회 (달성시점 + 현재시점)
     */
    public Map<String, Object> getRankingDataWithHistory(FanChallengeRecord record) {
        Map<String, Object> data = new HashMap<>();

        // 달성시점 데이터
        int achievedTotalSongs = record.getTotalSongs() != null ? record.getTotalSongs() : 0;
        int correctCount = record.getCorrectCount() != null ? record.getCorrectCount() : 0;
        double achievedClearRate = achievedTotalSongs > 0
                ? (double) correctCount / achievedTotalSongs * 100
                : 0.0;

        // 현재시점 데이터
        int currentTotalSongs = songService.countActiveSongsByArtist(record.getArtist());
        double currentClearRate;
        if (currentTotalSongs == 0) {
            currentClearRate = 0.0;
        } else {
            currentClearRate = (double) correctCount / currentTotalSongs * 100;
            // max 100%
            currentClearRate = Math.min(currentClearRate, 100.0);
        }

        data.put("achievedTotalSongs", achievedTotalSongs);
        data.put("currentTotalSongs", currentTotalSongs);
        data.put("correctCount", correctCount);
        data.put("achievedClearRate", Math.round(achievedClearRate * 10.0) / 10.0);
        data.put("currentClearRate", Math.round(currentClearRate * 10.0) / 10.0);
        data.put("isPerfectClear", record.getIsPerfectClear());
        data.put("isCurrentPerfect", record.getIsCurrentPerfect());
        data.put("bestTimeMs", record.getBestTimeMs());
        data.put("achievedAt", record.getAchievedAt());

        return data;
    }

    /**
     * 아티스트별 랭킹 조회 (현재시점 클리어율 기준)
     */
    public List<Map<String, Object>> getArtistRankingWithCurrentRate(String artist, int limit) {
        // 현재 곡 수
        int currentTotalSongs = songService.countActiveSongsByArtist(artist);

        // 하드코어 기록 조회
        List<FanChallengeRecord> records = fanChallengeRecordRepository.findTopByArtist(
                artist, org.springframework.data.domain.PageRequest.of(0, limit * 2)); // 여유분

        List<Map<String, Object>> result = new ArrayList<>();
        for (FanChallengeRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            int correctCount = record.getCorrectCount() != null ? record.getCorrectCount() : 0;

            // 현재시점 클리어율 계산
            double currentClearRate;
            if (currentTotalSongs == 0) {
                currentClearRate = 0.0;
            } else {
                currentClearRate = (double) correctCount / currentTotalSongs * 100;
                currentClearRate = Math.min(currentClearRate, 100.0);
            }

            item.put("nickname", record.getMember().getNickname());
            item.put("correctCount", correctCount);
            item.put("achievedTotalSongs", record.getTotalSongs());
            item.put("currentTotalSongs", currentTotalSongs);
            item.put("currentClearRate", Math.round(currentClearRate * 10.0) / 10.0);
            item.put("isPerfectClear", record.getIsPerfectClear());
            item.put("isCurrentPerfect", record.getIsCurrentPerfect());
            item.put("bestTimeMs", record.getBestTimeMs());
            item.put("achievedAt", record.getAchievedAt());

            result.add(item);
        }

        // 현재시점 클리어율 → 시간 순으로 정렬
        result.sort((a, b) -> {
            double rateA = (double) a.get("currentClearRate");
            double rateB = (double) b.get("currentClearRate");
            if (rateA != rateB) {
                return Double.compare(rateB, rateA); // 클리어율 높은 순
            }
            // 동률이면 시간 빠른 순
            Long timeA = (Long) a.get("bestTimeMs");
            Long timeB = (Long) b.get("bestTimeMs");
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return Long.compare(timeA, timeB);
        });

        // limit 적용
        if (result.size() > limit) {
            return result.subList(0, limit);
        }
        return result;
    }

    // ========== 글로벌 랭킹 메서드 (랭킹 페이지용) ==========

    /**
     * 퍼펙트 클리어 횟수 랭킹 (HARDCORE 기준)
     * @return [memberId, perfectCount] 형태
     */
    public List<Object[]> getPerfectClearRanking(int limit) {
        return fanChallengeRecordRepository.findPerfectClearCountRanking(
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    /**
     * 도전 아티스트 수 랭킹 (클리어한 고유 아티스트 수, HARDCORE 기준)
     * @return [memberId, artistCount] 형태
     */
    public List<Object[]> getArtistClearCountRanking(int limit) {
        return fanChallengeRecordRepository.findArtistClearCountRanking(
                org.springframework.data.domain.PageRequest.of(0, limit));
    }
}
