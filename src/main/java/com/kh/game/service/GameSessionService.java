package com.kh.game.service;

import com.kh.game.dto.GameSettings;
import com.kh.game.entity.GameRound;
import com.kh.game.entity.GameSession;
import com.kh.game.repository.GameRoundRepository;
import com.kh.game.repository.GameSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final GameRoundRepository gameRoundRepository;
    private final ObjectMapper objectMapper;

    public Page<GameSession> findAll(Pageable pageable) {
        return gameSessionRepository.findAll(pageable);
    }

    public Page<GameSession> findByStatus(GameSession.GameStatus status, Pageable pageable) {
        return gameSessionRepository.findByStatus(status, pageable);
    }

    public Page<GameSession> findByGameType(GameSession.GameType gameType, Pageable pageable) {
        return gameSessionRepository.findByGameType(gameType, pageable);
    }

    public Page<GameSession> search(String keyword, Pageable pageable) {
        return gameSessionRepository.findByNicknameContaining(keyword, pageable);
    }

    public Optional<GameSession> findById(Long id) {
        return gameSessionRepository.findById(id);
    }

    public Optional<GameSession> findByIdWithRounds(Long id) {
        return gameSessionRepository.findByIdWithRounds(id);
    }

    public List<GameSession> findBySessionUuid(String sessionUuid) {
        return gameSessionRepository.findBySessionUuidOrderByCreatedAtDesc(sessionUuid);
    }

    public List<GameRound> findRoundsBySessionId(Long sessionId) {
        return gameRoundRepository.findByGameSessionIdOrderByRoundNumberAsc(sessionId);
    }

    public List<GameRound> findRoundsWithAttemptsBySessionId(Long sessionId) {
        return gameRoundRepository.findRoundsWithAttemptsBySessionId(sessionId);
    }

    public List<GameSession> getTopScores(int limit) {
        return gameSessionRepository.findTopScores(Pageable.ofSize(limit));
    }

    public List<GameSession> getDailyTopScores(int limit) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return gameSessionRepository.findTopScoresSince(startOfDay, Pageable.ofSize(limit));
    }

    public List<GameSession> getWeeklyTopScores(int limit) {
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        return gameSessionRepository.findTopScoresSince(startOfWeek, Pageable.ofSize(limit));
    }

    // 게임 타입별 랭킹 조회
    public List<GameSession> getTopScoresByGameType(GameSession.GameType gameType, int limit) {
        return gameSessionRepository.findTopScoresByGameType(gameType, Pageable.ofSize(limit));
    }

    public List<GameSession> getDailyTopScoresByGameType(GameSession.GameType gameType, int limit) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return gameSessionRepository.findTopScoresByGameTypeSince(gameType, startOfDay, Pageable.ofSize(limit));
    }

    public List<GameSession> getWeeklyTopScoresByGameType(GameSession.GameType gameType, int limit) {
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        return gameSessionRepository.findTopScoresByGameTypeSince(gameType, startOfWeek, Pageable.ofSize(limit));
    }

    public Long getTodayGameCount() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return gameSessionRepository.countGamesSince(startOfDay);
    }

    public Double getAverageScore() {
        return gameSessionRepository.getAverageScore();
    }

    public GameSettings parseSettings(String settingsJson) {
        if (settingsJson == null || settingsJson.isEmpty()) {
            return new GameSettings();
        }
        try {
            return objectMapper.readValue(settingsJson, GameSettings.class);
        } catch (JsonProcessingException e) {
            return new GameSettings();
        }
    }

    public String toSettingsJson(GameSettings settings) {
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Transactional
    public GameSession save(GameSession session) {
        return gameSessionRepository.save(session);
    }

    @Transactional
    public void deleteById(Long id) {
        gameSessionRepository.deleteById(id);
    }

    // ========== 30곡 챌린지 랭킹 (점수 → 소요시간 순) ==========

    /**
     * 주간 시작 시간 계산 (이번 주 월요일 00:00)
     */
    private LocalDateTime getWeekStart() {
        return LocalDateTime.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate()
                .atStartOfDay();
    }

    /**
     * 월간 시작 시간 계산 (이번 달 1일 00:00)
     */
    private LocalDateTime getMonthStart() {
        return LocalDateTime.now()
                .withDayOfMonth(1)
                .toLocalDate()
                .atStartOfDay();
    }

    /**
     * 주간 30곡 랭킹 조회 (점수 → 소요시간 순)
     * @return List<Map> containing memberId, nickname, score, durationSeconds, achievedAt, rank
     */
    public List<Map<String, Object>> getWeeklyBest30RankingByDuration(int limit) {
        List<Object[]> results = gameSessionRepository.findWeeklyBest30RankingByDuration(getWeekStart(), limit);
        return convertToRankingResponse(results);
    }

    /**
     * 월간 30곡 랭킹 조회 (점수 → 소요시간 순)
     */
    public List<Map<String, Object>> getMonthlyBest30RankingByDuration(int limit) {
        List<Object[]> results = gameSessionRepository.findMonthlyBest30RankingByDuration(getMonthStart(), limit);
        return convertToRankingResponse(results);
    }

    /**
     * 역대 30곡 랭킹 조회 (점수 → 소요시간 순)
     */
    public List<Map<String, Object>> getAllTimeBest30RankingByDuration(int limit) {
        List<Object[]> results = gameSessionRepository.findAllTimeBest30RankingByDuration(limit);
        return convertToRankingResponse(results);
    }

    /**
     * Object[] 결과를 Map 리스트로 변환 (동점+동일시간 공동 순위 처리)
     */
    private List<Map<String, Object>> convertToRankingResponse(List<Object[]> results) {
        List<Map<String, Object>> ranking = new ArrayList<>();

        int currentRank = 0;
        Integer prevScore = null;
        Long prevDuration = null;

        for (Object[] row : results) {
            Long memberId = ((Number) row[0]).longValue();
            String nickname = (String) row[1];
            Integer score = ((Number) row[2]).intValue();
            Long durationSeconds = ((Number) row[3]).longValue();
            LocalDateTime achievedAt = row[4] != null ?
                    ((java.sql.Timestamp) row[4]).toLocalDateTime() : null;

            // 동점 + 동일 소요시간이면 같은 순위, 아니면 순위 증가
            if (prevScore == null || !score.equals(prevScore) || !durationSeconds.equals(prevDuration)) {
                currentRank++;
                prevScore = score;
                prevDuration = durationSeconds;
            }

            Map<String, Object> entry = new HashMap<>();
            entry.put("rank", currentRank);
            entry.put("memberId", memberId);
            entry.put("nickname", nickname);
            entry.put("score", score);
            entry.put("durationSeconds", durationSeconds);
            entry.put("durationFormatted", formatDuration(durationSeconds));
            entry.put("achievedAt", achievedAt);

            ranking.add(entry);
        }

        return ranking;
    }

    /**
     * 소요 시간 포맷팅 (초 → "mm:ss" 또는 "hh:mm:ss")
     */
    private String formatDuration(Long seconds) {
        if (seconds == null) return "-";
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%d:%02d", minutes, secs);
    }

    /**
     * 특정 회원의 주간 30곡 최고 기록 조회
     */
    public Map<String, Object> getMemberWeeklyBest30Record(Long memberId) {
        List<Object[]> results = gameSessionRepository.findMemberBest30Record(memberId, getWeekStart());
        return extractMemberBestRecord(results);
    }

    /**
     * 특정 회원의 월간 30곡 최고 기록 조회
     */
    public Map<String, Object> getMemberMonthlyBest30Record(Long memberId) {
        List<Object[]> results = gameSessionRepository.findMemberBest30Record(memberId, getMonthStart());
        return extractMemberBestRecord(results);
    }

    /**
     * 특정 회원의 역대 30곡 최고 기록 조회
     */
    public Map<String, Object> getMemberAllTimeBest30Record(Long memberId) {
        List<Object[]> results = gameSessionRepository.findMemberAllTimeBest30Record(memberId);
        return extractMemberBestRecord(results);
    }

    private Map<String, Object> extractMemberBestRecord(List<Object[]> results) {
        if (results.isEmpty()) {
            return null;
        }
        Object[] row = results.get(0);
        Map<String, Object> record = new HashMap<>();
        record.put("score", ((Number) row[0]).intValue());
        record.put("durationSeconds", ((Number) row[1]).longValue());
        record.put("durationFormatted", formatDuration(((Number) row[1]).longValue()));
        record.put("achievedAt", row[2] != null ?
                ((java.sql.Timestamp) row[2]).toLocalDateTime() : null);
        return record;
    }

    /**
     * 내 주간 30곡 순위 조회
     */
    public Long getMyWeeklyBest30Rank(Long memberId) {
        Long higherCount = gameSessionRepository.countHigherRankedMembers(memberId, getWeekStart());
        return higherCount != null ? higherCount + 1 : null;
    }

    /**
     * 내 월간 30곡 순위 조회
     */
    public Long getMyMonthlyBest30Rank(Long memberId) {
        Long higherCount = gameSessionRepository.countHigherRankedMembers(memberId, getMonthStart());
        return higherCount != null ? higherCount + 1 : null;
    }

    /**
     * 주간 30곡 참여자 수
     */
    public Long getWeeklyBest30ParticipantCount() {
        return gameSessionRepository.countBest30Participants(getWeekStart());
    }

    /**
     * 월간 30곡 참여자 수
     */
    public Long getMonthlyBest30ParticipantCount() {
        return gameSessionRepository.countBest30Participants(getMonthStart());
    }

    /**
     * 역대 30곡 참여자 수
     */
    public Long getAllTimeBest30ParticipantCount() {
        return gameSessionRepository.countAllTimeBest30Participants();
    }
}