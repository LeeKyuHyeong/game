package com.kh.game.entity;

/**
 * 멀티게임 전용 LP 티어 시스템
 * 롤(LoL) 스타일의 LP 기반 승급/강등 시스템
 */
public enum MultiTier {
    BRONZE(0, "브론즈", "#CD7F32"),
    SILVER(1, "실버", "#C0C0C0"),
    GOLD(2, "골드", "#FFD700"),
    PLATINUM(3, "플래티넘", "#E5E4E2"),
    DIAMOND(4, "다이아", "#B9F2FF"),
    MASTER(5, "마스터", "#FF6B6B"),
    CHALLENGER(6, "챌린저", "#00BFFF");

    private final int order;
    private final String displayName;
    private final String color;

    // LP 범위 상수
    public static final int MAX_LP = 100;
    public static final int MIN_LP = 0;

    MultiTier(int order, String displayName, String color) {
        this.order = order;
        this.displayName = displayName;
        this.color = color;
    }

    public int getOrder() {
        return order;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    /**
     * 승급 가능 여부 (챌린저는 승급 불가)
     */
    public boolean canPromote() {
        return this != CHALLENGER;
    }

    /**
     * 강등 가능 여부 (브론즈는 강등 불가)
     */
    public boolean canDemote() {
        return this != BRONZE;
    }

    /**
     * 다음 티어 반환 (승급용)
     */
    public MultiTier getNextTier() {
        if (!canPromote()) {
            return this;
        }
        return values()[this.ordinal() + 1];
    }

    /**
     * 이전 티어 반환 (강등용)
     */
    public MultiTier getPreviousTier() {
        if (!canDemote()) {
            return this;
        }
        return values()[this.ordinal() - 1];
    }

    /**
     * 티어와 LP를 통합 레이팅으로 변환 (ELO 계산용)
     * BRONZE 0LP = 0, CHALLENGER 100LP = 700
     */
    public int toRating(int lp) {
        return this.order * 100 + lp;
    }

    /**
     * 레이팅을 티어로 변환
     */
    public static MultiTier fromRating(int rating) {
        int tierOrder = Math.min(rating / 100, CHALLENGER.order);
        tierOrder = Math.max(tierOrder, 0);
        return values()[tierOrder];
    }

    /**
     * 기본 LP 변화량 (인원수와 순위 기준)
     */
    public static int getBaseLpChange(int totalPlayers, int rank) {
        if (totalPlayers < 2) {
            return 0;
        }

        if (totalPlayers == 2) {
            return switch (rank) {
                case 1 -> 100;
                case 2 -> 0;
                default -> 0;
            };
        } else if (totalPlayers <= 4) {
            return switch (rank) {
                case 1 -> 120;
                case 2 -> 40;
                case 3 -> 0;
                default -> -20;
            };
        } else if (totalPlayers <= 6) {
            return switch (rank) {
                case 1 -> 150;
                case 2 -> 60;
                case 3 -> 20;
                default -> -10;
            };
        } else {
            return switch (rank) {
                case 1 -> 180;
                case 2 -> 80;
                case 3 -> 30;
                default -> 0;
            };
        }
    }

    /**
     * ELO 기반 LP 변화량 계산
     * 상대 티어를 고려하여 LP 변화량 조정
     *
     * @param myTier 내 티어
     * @param myLp 내 LP
     * @param avgOpponentRating 상대들의 평균 레이팅
     * @param totalPlayers 총 참가자 수
     * @param rank 내 순위
     * @return 조정된 LP 변화량
     */
    public static int calculateLpChange(MultiTier myTier, int myLp, double avgOpponentRating, int totalPlayers, int rank) {
        int baseLp = getBaseLpChange(totalPlayers, rank);
        if (baseLp == 0) {
            return 0;
        }

        int myRating = myTier.toRating(myLp);
        double ratingDiff = avgOpponentRating - myRating;

        // ELO 기대 승률: E = 1 / (1 + 10^((Rb - Ra) / 400))
        double expectedScore = 1.0 / (1.0 + Math.pow(10, -ratingDiff / 400));

        // 실제 성과 점수 (1등: 1.0, 2등: 0.75, 3등: 0.5, 그 외: 0.25)
        double actualScore = switch (rank) {
            case 1 -> 1.0;
            case 2 -> 0.75;
            case 3 -> 0.5;
            default -> 0.25;
        };

        // 성과 차이에 따른 배수 계산 (0.5 ~ 1.5 범위)
        // 기대보다 잘하면 보너스, 못하면 페널티
        double performanceMultiplier = 1.0 + (actualScore - expectedScore) * 0.5;
        performanceMultiplier = Math.max(0.5, Math.min(1.5, performanceMultiplier));

        int adjustedLp = (int) Math.round(baseLp * performanceMultiplier);

        return adjustedLp;
    }

    /**
     * 기존 메서드 (하위 호환용) - 상대 티어 정보 없을 때
     */
    public static int calculateLpChange(int totalPlayers, int rank) {
        return getBaseLpChange(totalPlayers, rank);
    }
}
