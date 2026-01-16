package com.kh.game.entity;

/**
 * Fan Challenge (아티스트 챌린지) 난이도 설정
 */
public enum FanChallengeDifficulty {
    NORMAL("노말", 7000, 6000, 3, false, "⭐"),
    HARDCORE("하드코어", 5000, 5000, 3, false, "🔥");

    private final String displayName;
    private final int playTimeMs;      // 듣기 시간 (ms)
    private final int answerTimeMs;    // 입력 시간 (ms)
    private final int initialLives;    // 초기 라이프 개수
    private final boolean showChosungHint;  // 초성 힌트 표시 여부
    private final String badgeEmoji;   // 퍼펙트 클리어 뱃지 이모지

    FanChallengeDifficulty(String displayName, int playTimeMs, int answerTimeMs,
                           int initialLives, boolean showChosungHint, String badgeEmoji) {
        this.displayName = displayName;
        this.playTimeMs = playTimeMs;
        this.answerTimeMs = answerTimeMs;
        this.initialLives = initialLives;
        this.showChosungHint = showChosungHint;
        this.badgeEmoji = badgeEmoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPlayTimeMs() {
        return playTimeMs;
    }

    public int getAnswerTimeMs() {
        return answerTimeMs;
    }

    public int getInitialLives() {
        return initialLives;
    }

    public boolean isShowChosungHint() {
        return showChosungHint;
    }

    public String getBadgeEmoji() {
        return badgeEmoji;
    }

    /**
     * 총 제한 시간 (듣기 + 입력)
     */
    public int getTotalTimeMs() {
        return playTimeMs + answerTimeMs;
    }

    /**
     * 공식 랭킹 대상 여부 (하드코어만 랭킹 반영)
     */
    public boolean isRanked() {
        return this == HARDCORE;
    }

    /**
     * 문자열로부터 난이도 파싱 (기본값: NORMAL)
     */
    public static FanChallengeDifficulty fromString(String value) {
        if (value == null || value.isEmpty()) {
            return NORMAL;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
