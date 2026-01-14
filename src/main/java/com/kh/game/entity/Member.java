package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Column(name = "total_games")
    private Integer totalGames = 0;

    @Column(name = "total_score")
    private Integer totalScore = 0;

    @Column(name = "total_correct")
    private Integer totalCorrect = 0;

    @Column(name = "total_rounds")
    private Integer totalRounds = 0;

    @Column(name = "total_skip")
    private Integer totalSkip = 0;

    // Solo Guess (내가맞추기) 모드 통계
    @Column(name = "guess_games")
    private Integer guessGames = 0;

    @Column(name = "guess_score")
    private Integer guessScore = 0;

    @Column(name = "guess_correct")
    private Integer guessCorrect = 0;

    @Column(name = "guess_rounds")
    private Integer guessRounds = 0;

    @Column(name = "guess_skip")
    private Integer guessSkip = 0;

    // Multiplayer (멀티게임) 모드 통계
    @Column(name = "multi_games")
    private Integer multiGames = 0;

    @Column(name = "multi_score")
    private Integer multiScore = 0;

    @Column(name = "multi_correct")
    private Integer multiCorrect = 0;

    @Column(name = "multi_rounds")
    private Integer multiRounds = 0;

    // ========== 주간 통계 (Weekly) ==========

    // 주간 통계 (내가맞추기)
    @Column(name = "weekly_guess_games")
    private Integer weeklyGuessGames = 0;

    @Column(name = "weekly_guess_score")
    private Integer weeklyGuessScore = 0;

    @Column(name = "weekly_guess_correct")
    private Integer weeklyGuessCorrect = 0;

    @Column(name = "weekly_guess_rounds")
    private Integer weeklyGuessRounds = 0;

    // 주간 통계 (멀티게임)
    @Column(name = "weekly_multi_games")
    private Integer weeklyMultiGames = 0;

    @Column(name = "weekly_multi_score")
    private Integer weeklyMultiScore = 0;

    @Column(name = "weekly_multi_correct")
    private Integer weeklyMultiCorrect = 0;

    @Column(name = "weekly_multi_rounds")
    private Integer weeklyMultiRounds = 0;

    @Column(name = "weekly_reset_at")
    private LocalDateTime weeklyResetAt;

    // ========== 최고 기록 (Best Record) ==========

    // 최고 기록 (내가맞추기)
    @Column(name = "best_guess_score")
    private Integer bestGuessScore = 0;

    @Column(name = "best_guess_accuracy")
    private Double bestGuessAccuracy = 0.0;

    @Column(name = "best_guess_at")
    private LocalDateTime bestGuessAt;

    // 최고 기록 (멀티게임)
    @Column(name = "best_multi_score")
    private Integer bestMultiScore = 0;

    @Column(name = "best_multi_accuracy")
    private Double bestMultiAccuracy = 0.0;

    @Column(name = "best_multi_at")
    private LocalDateTime bestMultiAt;

    // ========== 30곡 최고점 랭킹 시스템 ==========

    // 30곡 랭킹 기준 라운드 수
    public static final int RANKING_ROUNDS = 30;

    // 주간 30곡 최고점
    @Column(name = "weekly_best_30_score")
    private Integer weeklyBest30Score;

    @Column(name = "weekly_best_30_at")
    private LocalDateTime weeklyBest30At;

    // 월간 30곡 최고점
    @Column(name = "monthly_best_30_score")
    private Integer monthlyBest30Score;

    @Column(name = "monthly_best_30_at")
    private LocalDateTime monthlyBest30At;

    @Column(name = "monthly_reset_at")
    private LocalDateTime monthlyResetAt;

    // 역대 30곡 최고점 (명예의 전당)
    @Column(name = "all_time_best_30_score")
    private Integer allTimeBest30Score;

    @Column(name = "all_time_best_30_at")
    private LocalDateTime allTimeBest30At;

    // ========== 멀티게임 전용 LP 티어 시스템 ==========

    @Enumerated(EnumType.STRING)
    @Column(name = "multi_tier", length = 20)
    private MultiTier multiTier = MultiTier.BRONZE;

    @Column(name = "multi_lp")
    private Integer multiLp = 0;

    @Column(name = "multi_tier_updated_at")
    private LocalDateTime multiTierUpdatedAt;

    @Column(name = "multi_wins")
    private Integer multiWins = 0;  // 1등 횟수

    @Column(name = "multi_top3")
    private Integer multiTop3 = 0;  // Top3 횟수

    // ========== 뱃지 시스템 ==========

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "selected_badge_id")
    private Badge selectedBadge;  // 현재 표시 중인 뱃지

    @Column(name = "current_correct_streak")
    private Integer currentCorrectStreak = 0;  // 현재 연속 정답 수

    @Column(name = "max_correct_streak")
    private Integer maxCorrectStreak = 0;  // 최대 연속 정답 기록

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role = MemberRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "session_token", length = 64)
    private String sessionToken;  // 현재 유효한 세션 토큰

    @Column(name = "session_created_at")
    private LocalDateTime sessionCreatedAt;  // 세션 생성 시간

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MemberRole {
        USER, ADMIN
    }

    public enum MemberStatus {
        ACTIVE, INACTIVE, BANNED
    }

    // ========== 전체 통계 메서드 ==========
    public double getAccuracyRate() {
        if (totalRounds == null || totalRounds == 0) return 0;
        return (double) totalCorrect / totalRounds * 100;
    }

    public double getAverageScore() {
        if (totalGames == null || totalGames == 0) return 0;
        return (double) totalScore / totalGames;
    }

    // ========== Solo Guess (내가맞추기) 통계 메서드 ==========
    public double getGuessAccuracyRate() {
        if (guessRounds == null || guessRounds == 0) return 0;
        return (double) guessCorrect / guessRounds * 100;
    }

    public double getGuessAverageScore() {
        if (guessGames == null || guessGames == 0) return 0;
        return (double) guessScore / guessGames;
    }

    // 라운드별 평균점수
    public double getGuessAverageScorePerRound() {
        if (guessRounds == null || guessRounds == 0) return 0;
        return (double) guessScore / guessRounds;
    }

    // ========== Multiplayer (멀티게임) 통계 메서드 ==========
    public double getMultiAccuracyRate() {
        if (multiRounds == null || multiRounds == 0) return 0;
        return (double) multiCorrect / multiRounds * 100;
    }

    public double getMultiAverageScore() {
        if (multiGames == null || multiGames == 0) return 0;
        return (double) multiScore / multiGames;
    }

    // ========== 게임 결과 반영 ==========

    // 기존 메서드 (하위 호환성 유지 - Solo Guess용으로 사용)
    public void addGameResult(int score, int correct, int rounds, int skip) {
        this.totalGames = (this.totalGames == null ? 0 : this.totalGames) + 1;
        this.totalScore = (this.totalScore == null ? 0 : this.totalScore) + score;
        this.totalCorrect = (this.totalCorrect == null ? 0 : this.totalCorrect) + correct;
        this.totalRounds = (this.totalRounds == null ? 0 : this.totalRounds) + rounds;
        this.totalSkip = (this.totalSkip == null ? 0 : this.totalSkip) + skip;
    }

    // 최고 기록 집계를 위한 최소 라운드 수
    public static final int MIN_ROUNDS_FOR_BEST_SCORE = 10;

    // Solo Guess (내가맞추기) 게임 결과 반영
    // isEligibleForBestScore: 최고기록 랭킹 대상 여부 (전체랜덤 + 필터없음 + 10라운드 이상)
    public void addGuessGameResult(int score, int correct, int rounds, int skip, boolean isEligibleForBestScore) {
        // 모드별 통계
        this.guessGames = (this.guessGames == null ? 0 : this.guessGames) + 1;
        this.guessScore = (this.guessScore == null ? 0 : this.guessScore) + score;
        this.guessCorrect = (this.guessCorrect == null ? 0 : this.guessCorrect) + correct;
        this.guessRounds = (this.guessRounds == null ? 0 : this.guessRounds) + rounds;
        this.guessSkip = (this.guessSkip == null ? 0 : this.guessSkip) + skip;

        // 주간 통계도 업데이트
        this.weeklyGuessGames = (this.weeklyGuessGames == null ? 0 : this.weeklyGuessGames) + 1;
        this.weeklyGuessScore = (this.weeklyGuessScore == null ? 0 : this.weeklyGuessScore) + score;
        this.weeklyGuessCorrect = (this.weeklyGuessCorrect == null ? 0 : this.weeklyGuessCorrect) + correct;
        this.weeklyGuessRounds = (this.weeklyGuessRounds == null ? 0 : this.weeklyGuessRounds) + rounds;

        // 최고 기록 갱신 체크 (전체랜덤 + 필터없음 + 10라운드 이상 게임만)
        if (isEligibleForBestScore && rounds >= MIN_ROUNDS_FOR_BEST_SCORE) {
            double accuracy = (double) correct / rounds * 100;
            if (this.bestGuessScore == null || score > this.bestGuessScore) {
                this.bestGuessScore = score;
                this.bestGuessAt = LocalDateTime.now();
            }
            if (this.bestGuessAccuracy == null || accuracy > this.bestGuessAccuracy) {
                this.bestGuessAccuracy = accuracy;
            }
        }

        // 전체 통계도 업데이트
        addGameResult(score, correct, rounds, skip);
    }

    // Multiplayer (멀티게임) 게임 결과 반영
    public void addMultiGameResult(int score, int correct, int rounds) {
        // 모드별 통계
        this.multiGames = (this.multiGames == null ? 0 : this.multiGames) + 1;
        this.multiScore = (this.multiScore == null ? 0 : this.multiScore) + score;
        this.multiCorrect = (this.multiCorrect == null ? 0 : this.multiCorrect) + correct;
        this.multiRounds = (this.multiRounds == null ? 0 : this.multiRounds) + rounds;

        // 주간 통계도 업데이트
        this.weeklyMultiGames = (this.weeklyMultiGames == null ? 0 : this.weeklyMultiGames) + 1;
        this.weeklyMultiScore = (this.weeklyMultiScore == null ? 0 : this.weeklyMultiScore) + score;
        this.weeklyMultiCorrect = (this.weeklyMultiCorrect == null ? 0 : this.weeklyMultiCorrect) + correct;
        this.weeklyMultiRounds = (this.weeklyMultiRounds == null ? 0 : this.weeklyMultiRounds) + rounds;

        // 최고 기록 갱신 체크 (10라운드 이상 게임만)
        if (rounds >= MIN_ROUNDS_FOR_BEST_SCORE) {
            double accuracy = (double) correct / rounds * 100;
            if (this.bestMultiScore == null || score > this.bestMultiScore) {
                this.bestMultiScore = score;
                this.bestMultiAt = LocalDateTime.now();
            }
            if (this.bestMultiAccuracy == null || accuracy > this.bestMultiAccuracy) {
                this.bestMultiAccuracy = accuracy;
            }
        }

        // 전체 통계도 업데이트 (멀티게임은 skip 없음)
        addGameResult(score, correct, rounds, 0);
    }

    // 주간 통계 리셋
    public void resetWeeklyStats() {
        this.weeklyGuessGames = 0;
        this.weeklyGuessScore = 0;
        this.weeklyGuessCorrect = 0;
        this.weeklyGuessRounds = 0;
        this.weeklyMultiGames = 0;
        this.weeklyMultiScore = 0;
        this.weeklyMultiCorrect = 0;
        this.weeklyMultiRounds = 0;
        // 주간 30곡 최고점도 리셋
        this.weeklyBest30Score = null;
        this.weeklyBest30At = null;
        this.weeklyResetAt = LocalDateTime.now();
    }

    // 월간 통계 리셋
    public void resetMonthlyStats() {
        this.monthlyBest30Score = null;
        this.monthlyBest30At = null;
        this.monthlyResetAt = LocalDateTime.now();
    }

    /**
     * 30곡 게임 완료 시 최고점 갱신 체크
     * @param score 30곡 게임에서 획득한 점수
     * @return true면 어느 하나라도 최고점 갱신됨
     */
    public boolean update30SongBestScore(int score) {
        boolean updated = false;
        LocalDateTime now = LocalDateTime.now();

        // 주간 최고점 갱신
        if (this.weeklyBest30Score == null || score > this.weeklyBest30Score) {
            this.weeklyBest30Score = score;
            this.weeklyBest30At = now;
            updated = true;
        }

        // 월간 최고점 갱신
        if (this.monthlyBest30Score == null || score > this.monthlyBest30Score) {
            this.monthlyBest30Score = score;
            this.monthlyBest30At = now;
            updated = true;
        }

        // 역대 최고점 갱신 (명예의 전당)
        if (this.allTimeBest30Score == null || score > this.allTimeBest30Score) {
            this.allTimeBest30Score = score;
            this.allTimeBest30At = now;
            updated = true;
        }

        return updated;
    }

    // ========== 주간 통계 메서드 ==========

    public double getWeeklyGuessAccuracyRate() {
        if (weeklyGuessRounds == null || weeklyGuessRounds == 0) return 0;
        return (double) weeklyGuessCorrect / weeklyGuessRounds * 100;
    }

    public double getWeeklyMultiAccuracyRate() {
        if (weeklyMultiRounds == null || weeklyMultiRounds == 0) return 0;
        return (double) weeklyMultiCorrect / weeklyMultiRounds * 100;
    }

    // ========== 멀티게임 LP 티어 메서드 ==========

    public String getMultiTierDisplayName() {
        return multiTier != null ? multiTier.getDisplayName() : MultiTier.BRONZE.getDisplayName();
    }

    public String getMultiTierColor() {
        return multiTier != null ? multiTier.getColor() : MultiTier.BRONZE.getColor();
    }

    /**
     * LP 적용 및 티어 변동 처리
     * @param lpChange LP 변화량 (양수: 획득, 음수: 차감)
     * @return 티어 변동 여부 ("PROMOTED", "DEMOTED", null)
     */
    public String applyLpChange(int lpChange) {
        if (this.multiTier == null) {
            this.multiTier = MultiTier.BRONZE;
        }
        if (this.multiLp == null) {
            this.multiLp = 0;
        }

        MultiTier oldTier = this.multiTier;
        int newLp = this.multiLp + lpChange;
        String tierChange = null;

        // LP가 100 이상 -> 승급 처리
        while (newLp >= MultiTier.MAX_LP && this.multiTier.canPromote()) {
            newLp -= MultiTier.MAX_LP;
            this.multiTier = this.multiTier.getNextTier();
            tierChange = "PROMOTED";
        }

        // LP가 0 미만 -> 강등 처리
        while (newLp < MultiTier.MIN_LP && this.multiTier.canDemote()) {
            this.multiTier = this.multiTier.getPreviousTier();
            newLp += MultiTier.MAX_LP;  // 이전 티어의 LP로 전환 (예: -20 -> 80)
            tierChange = "DEMOTED";
        }

        // 브론즈에서 LP가 음수면 0으로 고정
        if (newLp < 0 && !this.multiTier.canDemote()) {
            newLp = 0;
        }

        // 챌린저에서 LP가 100 이상이면 유지 (랭킹 구분용으로 무제한 허용)
        // 단, MAX_LP 이상이면 MAX_LP - 1로 고정 (승급 불가이므로)
        if (!this.multiTier.canPromote() && newLp >= MultiTier.MAX_LP) {
            // 챌린저는 LP 무제한 누적 가능 (랭킹 순위 구분용)
        }

        this.multiLp = newLp;

        // 티어가 변경되었으면 시간 기록
        if (tierChange != null) {
            this.multiTierUpdatedAt = LocalDateTime.now();
        }

        return tierChange;
    }

    /**
     * 멀티게임 순위 통계 업데이트
     * @param rank 게임에서의 순위 (1등, 2등, ...)
     */
    public void updateMultiRankStats(int rank) {
        if (rank == 1) {
            this.multiWins = (this.multiWins == null ? 0 : this.multiWins) + 1;
        }
        if (rank <= 3) {
            this.multiTop3 = (this.multiTop3 == null ? 0 : this.multiTop3) + 1;
        }
    }

    // ========== 뱃지 관련 메서드 ==========

    public String getSelectedBadgeEmoji() {
        return selectedBadge != null ? selectedBadge.getEmoji() : null;
    }

    public String getSelectedBadgeName() {
        return selectedBadge != null ? selectedBadge.getName() : null;
    }

    public String getSelectedBadgeColor() {
        return selectedBadge != null ? selectedBadge.getColor() : null;
    }

    /**
     * 연속 정답 업데이트
     * @param wasCorrect 정답 여부
     */
    public void updateCorrectStreak(boolean wasCorrect) {
        if (wasCorrect) {
            this.currentCorrectStreak = (this.currentCorrectStreak == null ? 0 : this.currentCorrectStreak) + 1;
            if (this.currentCorrectStreak > (this.maxCorrectStreak == null ? 0 : this.maxCorrectStreak)) {
                this.maxCorrectStreak = this.currentCorrectStreak;
            }
        } else {
            this.currentCorrectStreak = 0;
        }
    }
}