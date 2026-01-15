package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.Member;
import com.kh.game.entity.RankingHistory;
import com.kh.game.entity.RankingHistory.PeriodType;
import com.kh.game.entity.RankingHistory.RankingType;
import com.kh.game.repository.MemberRepository;
import com.kh.game.repository.RankingHistoryRepository;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 랭킹 스냅샷 배치
 *
 * 주간/월간 랭킹 리셋 전에 Top 100 기록을 RankingHistory 테이블에 보관합니다.
 * 기존 RankingUpdateBatch를 대체하며, 실제로 데이터를 저장합니다.
 *
 * 저장하는 랭킹 타입:
 * - 주간: 내가맞추기 총점, 멀티게임 총점, 30곡 최고점, 멀티 티어, 멀티 1등횟수
 * - 월간: 30곡 최고점
 *
 * 권장 스케줄:
 * - 주간: 매주 월요일 05:50 (WeeklyRankingResetBatch 전)
 * - 월간: 매월 1일 00:00 전 (MonthlyRankingResetBatch 전)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingSnapshotBatch {

    private final MemberRepository memberRepository;
    private final RankingHistoryRepository rankingHistoryRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_RANKING_SNAPSHOT";
    private static final int TOP_LIMIT = 100;

    /**
     * 주간 랭킹 스냅샷 저장
     * WeeklyRankingResetBatch 실행 전에 호출해야 합니다.
     */
    @Transactional
    public int executeWeekly(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalSaved = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 주간 랭킹 스냅샷 배치 시작", BATCH_ID);

            // 주간 기간 계산 (이번 주 월요일 ~ 일요일)
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);

            // 중복 실행 체크
            if (isSnapshotExists(PeriodType.WEEKLY, RankingType.WEEKLY_GUESS_SCORE, weekStart, weekEnd)) {
                String msg = String.format("이미 %s ~ %s 기간의 주간 스냅샷이 존재합니다. 스킵합니다.", weekStart, weekEnd);
                log.info("[{}] {}", BATCH_ID, msg);

                recordExecution(executionType, BatchConfig.ExecutionResult.SUCCESS, msg, 0, startTime);
                return 0;
            }

            // 1. 주간 내가맞추기 총점 스냅샷
            int guessCount = saveWeeklyGuessScoreSnapshot(weekStart, weekEnd);
            totalSaved += guessCount;
            log.info("주간 내가맞추기 총점 스냅샷: {}명", guessCount);

            // 2. 주간 멀티게임 총점 스냅샷
            int multiCount = saveWeeklyMultiScoreSnapshot(weekStart, weekEnd);
            totalSaved += multiCount;
            log.info("주간 멀티게임 총점 스냅샷: {}명", multiCount);

            // 3. 주간 30곡 최고점 스냅샷
            int best30Count = saveWeeklyBest30Snapshot(weekStart, weekEnd);
            totalSaved += best30Count;
            log.info("주간 30곡 최고점 스냅샷: {}명", best30Count);

            // 4. 주간 멀티 티어 스냅샷
            int tierCount = saveWeeklyMultiTierSnapshot(weekStart, weekEnd);
            totalSaved += tierCount;
            log.info("주간 멀티 티어 스냅샷: {}명", tierCount);

            // 5. 주간 멀티 1등 횟수 스냅샷
            int winsCount = saveWeeklyMultiWinsSnapshot(weekStart, weekEnd);
            totalSaved += winsCount;
            log.info("주간 멀티 1등횟수 스냅샷: {}명", winsCount);

            resultMessage.append(String.format(
                    "주간 스냅샷 저장 완료. 내가맞추기: %d명, 멀티: %d명, 30곡: %d명, 티어: %d명, 1등: %d명 (총 %d건)",
                    guessCount, multiCount, best30Count, tierCount, winsCount, totalSaved
            ));

            recordExecution(executionType, BatchConfig.ExecutionResult.SUCCESS, resultMessage.toString(), totalSaved, startTime);
            log.info("[{}] 주간 스냅샷 배치 완료 - {}건 저장", BATCH_ID, totalSaved);

            return totalSaved;

        } catch (Exception e) {
            String errorMsg = "오류 발생: " + e.getMessage();
            recordExecution(executionType, BatchConfig.ExecutionResult.FAIL, errorMsg, totalSaved, startTime);
            log.error("[{}] 주간 스냅샷 배치 실패", BATCH_ID, e);
            throw new RuntimeException("배치 실행 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 월간 랭킹 스냅샷 저장
     * MonthlyRankingResetBatch 실행 전에 호출해야 합니다.
     */
    @Transactional
    public int executeMonthly(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalSaved = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 월간 랭킹 스냅샷 배치 시작", BATCH_ID);

            // 월간 기간 계산 (이번 달 1일 ~ 말일)
            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());

            // 중복 실행 체크
            if (isSnapshotExists(PeriodType.MONTHLY, RankingType.MONTHLY_BEST_30, monthStart, monthEnd)) {
                String msg = String.format("이미 %s ~ %s 기간의 월간 스냅샷이 존재합니다. 스킵합니다.", monthStart, monthEnd);
                log.info("[{}] {}", BATCH_ID, msg);

                recordExecution(executionType, BatchConfig.ExecutionResult.SUCCESS, msg, 0, startTime);
                return 0;
            }

            // 월간 30곡 최고점 스냅샷
            int best30Count = saveMonthlyBest30Snapshot(monthStart, monthEnd);
            totalSaved += best30Count;

            resultMessage.append(String.format(
                    "월간 스냅샷 저장 완료. 30곡 최고점: %d명 (총 %d건)",
                    best30Count, totalSaved
            ));

            recordExecution(executionType, BatchConfig.ExecutionResult.SUCCESS, resultMessage.toString(), totalSaved, startTime);
            log.info("[{}] 월간 스냅샷 배치 완료 - {}건 저장", BATCH_ID, totalSaved);

            return totalSaved;

        } catch (Exception e) {
            String errorMsg = "오류 발생: " + e.getMessage();
            recordExecution(executionType, BatchConfig.ExecutionResult.FAIL, errorMsg, totalSaved, startTime);
            log.error("[{}] 월간 스냅샷 배치 실패", BATCH_ID, e);
            throw new RuntimeException("배치 실행 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 통합 실행 (주간 + 월간)
     * BatchScheduler에서 호출하는 기본 메서드
     */
    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        LocalDate today = LocalDate.now();

        // 매주 월요일이면 주간 스냅샷
        if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
            return executeWeekly(executionType);
        }

        // 매월 1일이면 월간 스냅샷
        if (today.getDayOfMonth() == 1) {
            return executeMonthly(executionType);
        }

        // 그 외에는 수동 실행 시 주간 스냅샷
        return executeWeekly(executionType);
    }

    // ========== 주간 스냅샷 저장 메서드 ==========

    private int saveWeeklyGuessScoreSnapshot(LocalDate weekStart, LocalDate weekEnd) {
        List<Member> ranking = memberRepository.findTopWeeklyGuessRankingByScore(
                PageRequest.of(0, TOP_LIMIT));

        int rank = 1;
        for (Member member : ranking) {
            RankingHistory history = RankingHistory.createWeeklyGuessSnapshot(rank++, member, weekStart, weekEnd);
            rankingHistoryRepository.save(history);
        }

        return ranking.size();
    }

    private int saveWeeklyMultiScoreSnapshot(LocalDate weekStart, LocalDate weekEnd) {
        List<Member> ranking = memberRepository.findTopWeeklyMultiRankingByScore(
                PageRequest.of(0, TOP_LIMIT));

        int rank = 1;
        for (Member member : ranking) {
            RankingHistory history = RankingHistory.createWeeklyMultiSnapshot(rank++, member, weekStart, weekEnd);
            rankingHistoryRepository.save(history);
        }

        return ranking.size();
    }

    private int saveWeeklyBest30Snapshot(LocalDate weekStart, LocalDate weekEnd) {
        List<Member> ranking = memberRepository.findWeeklyBest30Ranking(
                PageRequest.of(0, TOP_LIMIT));

        int rank = 1;
        for (Member member : ranking) {
            RankingHistory history = RankingHistory.createWeeklyBest30Snapshot(rank++, member, weekStart, weekEnd);
            rankingHistoryRepository.save(history);
        }

        return ranking.size();
    }

    private int saveWeeklyMultiTierSnapshot(LocalDate weekStart, LocalDate weekEnd) {
        List<Member> ranking = memberRepository.findTopMultiTierRanking(
                PageRequest.of(0, TOP_LIMIT));

        int rank = 1;
        for (Member member : ranking) {
            RankingHistory history = RankingHistory.createWeeklyMultiTierSnapshot(rank++, member, weekStart, weekEnd);
            rankingHistoryRepository.save(history);
        }

        return ranking.size();
    }

    private int saveWeeklyMultiWinsSnapshot(LocalDate weekStart, LocalDate weekEnd) {
        List<Member> ranking = memberRepository.findTopMultiWins(
                PageRequest.of(0, TOP_LIMIT));

        int rank = 1;
        for (Member member : ranking) {
            RankingHistory history = RankingHistory.createWeeklyMultiWinsSnapshot(rank++, member, weekStart, weekEnd);
            rankingHistoryRepository.save(history);
        }

        return ranking.size();
    }

    // ========== 월간 스냅샷 저장 메서드 ==========

    private int saveMonthlyBest30Snapshot(LocalDate monthStart, LocalDate monthEnd) {
        List<Member> ranking = memberRepository.findMonthlyBest30Ranking(
                PageRequest.of(0, TOP_LIMIT));

        int rank = 1;
        for (Member member : ranking) {
            RankingHistory history = RankingHistory.createMonthlyBest30Snapshot(rank++, member, monthStart, monthEnd);
            rankingHistoryRepository.save(history);
        }

        return ranking.size();
    }

    // ========== 유틸리티 메서드 ==========

    private boolean isSnapshotExists(PeriodType periodType, RankingType rankingType,
                                     LocalDate periodStart, LocalDate periodEnd) {
        return rankingHistoryRepository.existsByPeriodTypeAndRankingTypeAndPeriodStartAndPeriodEnd(
                periodType, rankingType, periodStart, periodEnd);
    }

    private void recordExecution(BatchExecutionHistory.ExecutionType executionType,
                                 BatchConfig.ExecutionResult result,
                                 String message, int affectedCount, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        batchService.recordExecution(BATCH_ID, executionType, result, message, affectedCount, executionTime);
    }
}
