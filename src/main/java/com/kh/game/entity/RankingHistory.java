package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 랭킹 스냅샷 히스토리
 * 주간/월간 랭킹 리셋 전에 상위 100명의 기록을 보관합니다.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ranking_history", indexes = {
    @Index(name = "idx_ranking_history_period_type", columnList = "period_type, ranking_type"),
    @Index(name = "idx_ranking_history_member", columnList = "member_id"),
    @Index(name = "idx_ranking_history_period", columnList = "period_start, period_end")
})
public class RankingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 기간 유형 (주간/월간)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private PeriodType periodType;

    /**
     * 랭킹 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ranking_type", nullable = false, length = 30)
    private RankingType rankingType;

    /**
     * 순위 (1~100)
     */
    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    /**
     * 회원 ID (FK 아님 - 탈퇴해도 기록 유지)
     */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /**
     * 당시 닉네임 (스냅샷)
     */
    @Column(nullable = false, length = 50)
    private String nickname;

    /**
     * 점수
     */
    @Column(nullable = false)
    private Integer score;

    /**
     * 정답률 (선택적)
     */
    @Column
    private Double accuracy;

    /**
     * 추가 정보 (게임 수, 정답 수 등)
     */
    @Column(name = "games_played")
    private Integer gamesPlayed;

    @Column(name = "correct_count")
    private Integer correctCount;

    /**
     * 티어 정보 (멀티게임 티어 랭킹용)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "multi_tier", length = 20)
    private MultiTier multiTier;

    @Column(name = "multi_lp")
    private Integer multiLp;

    /**
     * 기간 정보
     */
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ========== Enum 정의 ==========

    /**
     * 기간 유형
     */
    public enum PeriodType {
        WEEKLY("주간"),
        MONTHLY("월간");

        private final String displayName;

        PeriodType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 랭킹 유형
     */
    public enum RankingType {
        // 주간 랭킹
        WEEKLY_GUESS_SCORE("주간 내가맞추기 총점"),
        WEEKLY_GUESS_ACCURACY("주간 내가맞추기 정답률"),
        WEEKLY_MULTI_SCORE("주간 멀티게임 총점"),
        WEEKLY_MULTI_TIER("주간 멀티게임 티어"),
        WEEKLY_MULTI_WINS("주간 멀티게임 1등횟수"),
        WEEKLY_BEST_30("주간 30곡 최고점"),

        // 월간 랭킹
        MONTHLY_BEST_30("월간 30곡 최고점");

        private final String displayName;

        RankingType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isWeekly() {
            return this.name().startsWith("WEEKLY_");
        }

        public boolean isMonthly() {
            return this.name().startsWith("MONTHLY_");
        }
    }

    // ========== Builder 패턴 ==========

    @Builder
    public RankingHistory(PeriodType periodType, RankingType rankingType, Integer rankPosition,
                          Long memberId, String nickname, Integer score, Double accuracy,
                          Integer gamesPlayed, Integer correctCount,
                          MultiTier multiTier, Integer multiLp,
                          LocalDate periodStart, LocalDate periodEnd) {
        this.periodType = periodType;
        this.rankingType = rankingType;
        this.rankPosition = rankPosition;
        this.memberId = memberId;
        this.nickname = nickname;
        this.score = score;
        this.accuracy = accuracy;
        this.gamesPlayed = gamesPlayed;
        this.correctCount = correctCount;
        this.multiTier = multiTier;
        this.multiLp = multiLp;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    // ========== 편의 메서드 ==========

    /**
     * 주간 내가맞추기 스냅샷 생성
     */
    public static RankingHistory createWeeklyGuessSnapshot(
            int rank, Member member, LocalDate weekStart, LocalDate weekEnd) {
        return RankingHistory.builder()
                .periodType(PeriodType.WEEKLY)
                .rankingType(RankingType.WEEKLY_GUESS_SCORE)
                .rankPosition(rank)
                .memberId(member.getId())
                .nickname(member.getNickname())
                .score(member.getWeeklyGuessScore() != null ? member.getWeeklyGuessScore() : 0)
                .accuracy(member.getWeeklyGuessAccuracyRate())
                .gamesPlayed(member.getWeeklyGuessGames())
                .correctCount(member.getWeeklyGuessCorrect())
                .periodStart(weekStart)
                .periodEnd(weekEnd)
                .build();
    }

    /**
     * 주간 멀티게임 스냅샷 생성
     */
    public static RankingHistory createWeeklyMultiSnapshot(
            int rank, Member member, LocalDate weekStart, LocalDate weekEnd) {
        return RankingHistory.builder()
                .periodType(PeriodType.WEEKLY)
                .rankingType(RankingType.WEEKLY_MULTI_SCORE)
                .rankPosition(rank)
                .memberId(member.getId())
                .nickname(member.getNickname())
                .score(member.getWeeklyMultiScore() != null ? member.getWeeklyMultiScore() : 0)
                .accuracy(member.getWeeklyMultiAccuracyRate())
                .gamesPlayed(member.getWeeklyMultiGames())
                .correctCount(member.getWeeklyMultiCorrect())
                .periodStart(weekStart)
                .periodEnd(weekEnd)
                .build();
    }

    /**
     * 주간 30곡 최고점 스냅샷 생성
     */
    public static RankingHistory createWeeklyBest30Snapshot(
            int rank, Member member, LocalDate weekStart, LocalDate weekEnd) {
        return RankingHistory.builder()
                .periodType(PeriodType.WEEKLY)
                .rankingType(RankingType.WEEKLY_BEST_30)
                .rankPosition(rank)
                .memberId(member.getId())
                .nickname(member.getNickname())
                .score(member.getWeeklyBest30Score() != null ? member.getWeeklyBest30Score() : 0)
                .periodStart(weekStart)
                .periodEnd(weekEnd)
                .build();
    }

    /**
     * 월간 30곡 최고점 스냅샷 생성
     */
    public static RankingHistory createMonthlyBest30Snapshot(
            int rank, Member member, LocalDate monthStart, LocalDate monthEnd) {
        return RankingHistory.builder()
                .periodType(PeriodType.MONTHLY)
                .rankingType(RankingType.MONTHLY_BEST_30)
                .rankPosition(rank)
                .memberId(member.getId())
                .nickname(member.getNickname())
                .score(member.getMonthlyBest30Score() != null ? member.getMonthlyBest30Score() : 0)
                .periodStart(monthStart)
                .periodEnd(monthEnd)
                .build();
    }

    /**
     * 주간 멀티 티어 스냅샷 생성
     */
    public static RankingHistory createWeeklyMultiTierSnapshot(
            int rank, Member member, LocalDate weekStart, LocalDate weekEnd) {
        return RankingHistory.builder()
                .periodType(PeriodType.WEEKLY)
                .rankingType(RankingType.WEEKLY_MULTI_TIER)
                .rankPosition(rank)
                .memberId(member.getId())
                .nickname(member.getNickname())
                .score(member.getMultiLp() != null ? member.getMultiLp() : 0)
                .multiTier(member.getMultiTier())
                .multiLp(member.getMultiLp())
                .gamesPlayed(member.getMultiGames())
                .periodStart(weekStart)
                .periodEnd(weekEnd)
                .build();
    }

    /**
     * 주간 멀티 1등 횟수 스냅샷 생성
     */
    public static RankingHistory createWeeklyMultiWinsSnapshot(
            int rank, Member member, LocalDate weekStart, LocalDate weekEnd) {
        return RankingHistory.builder()
                .periodType(PeriodType.WEEKLY)
                .rankingType(RankingType.WEEKLY_MULTI_WINS)
                .rankPosition(rank)
                .memberId(member.getId())
                .nickname(member.getNickname())
                .score(member.getMultiWins() != null ? member.getMultiWins() : 0)
                .gamesPlayed(member.getMultiGames())
                .periodStart(weekStart)
                .periodEnd(weekEnd)
                .build();
    }
}
