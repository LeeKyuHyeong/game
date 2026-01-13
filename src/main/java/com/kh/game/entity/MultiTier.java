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
     * 인원수와 순위에 따른 LP 변화량 계산
     *
     * | 인원   | 1등  | 2등  | 3등  | 나머지 |
     * |--------|------|------|------|--------|
     * | 2명    | +100 | 0    | -    | -      |
     * | 3~4명  | +120 | +40  | 0    | -20    |
     * | 5~6명  | +150 | +60  | +20  | -10    |
     * | 7~10명 | +180 | +80  | +30  | 0      |
     */
    public static int calculateLpChange(int totalPlayers, int rank) {
        if (totalPlayers < 2) {
            return 0; // 최소 2명 필요
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
        } else { // 7~10명
            return switch (rank) {
                case 1 -> 180;
                case 2 -> 80;
                case 3 -> 30;
                default -> 0;
            };
        }
    }
}
