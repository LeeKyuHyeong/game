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

    // ========== 티어 시스템 ==========

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", length = 20)
    private MemberTier tier = MemberTier.BRONZE;

    @Column(name = "tier_updated_at")
    private LocalDateTime tierUpdatedAt;

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

    // 티어 시스템
    public enum MemberTier {
        BRONZE(0, "브론즈", "#CD7F32"),
        SILVER(1000, "실버", "#C0C0C0"),
        GOLD(3000, "골드", "#FFD700"),
        PLATINUM(6000, "플래티넘", "#E5E4E2"),
        DIAMOND(10000, "다이아", "#B9F2FF"),
        MASTER(20000, "마스터", "#FF6B6B");

        private final int requiredScore;
        private final String displayName;
        private final String color;

        MemberTier(int requiredScore, String displayName, String color) {
            this.requiredScore = requiredScore;
            this.displayName = displayName;
            this.color = color;
        }

        public int getRequiredScore() { return requiredScore; }
        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }

        // 점수에 따른 티어 계산 (강등 없음)
        public static MemberTier calculateTier(int totalScore) {
            MemberTier result = BRONZE;
            for (MemberTier tier : values()) {
                if (totalScore >= tier.requiredScore) {
                    result = tier;
                }
            }
            return result;
        }
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

        // 티어 업데이트 (강등 없음)
        updateTier();
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

        // 티어 업데이트 (강등 없음)
        updateTier();
    }

    // 티어 업데이트 (강등 없음 - 현재 티어보다 높을 때만 승급)
    public void updateTier() {
        int combinedScore = (this.guessScore == null ? 0 : this.guessScore)
                          + (this.multiScore == null ? 0 : this.multiScore);
        MemberTier newTier = MemberTier.calculateTier(combinedScore);

        // 강등 없음: 새 티어가 현재 티어보다 높을 때만 업데이트
        if (this.tier == null || newTier.ordinal() > this.tier.ordinal()) {
            this.tier = newTier;
            this.tierUpdatedAt = LocalDateTime.now();
        }
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
        this.weeklyResetAt = LocalDateTime.now();
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

    // 티어 표시용 정보
    public String getTierDisplayName() {
        return tier != null ? tier.getDisplayName() : MemberTier.BRONZE.getDisplayName();
    }

    public String getTierColor() {
        return tier != null ? tier.getColor() : MemberTier.BRONZE.getColor();
    }
}