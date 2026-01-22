package com.kh.game.entity;

/**
 * Genre Challenge (장르 챌린지) 난이도 설정
 * - 듣기 시간 + 입력 시간 분리 방식 (아티스트 챌린지와 동일)
 * - 라이프 5개로 팬 챌린지보다 여유로움 (장르당 곡 수가 많음)
 */
public enum GenreChallengeDifficulty {
    NORMAL("노말", 7000, 6000, 5, false, "⭐"),
    HARDCORE("하드코어", 5000, 5000, 5, true, "🔥");

    private final String displayName;
    private final int playTimeMs;       // 듣기 시간 (ms)
    private final int answerTimeMs;     // 입력 시간 (ms)
    private final int initialLives;     // 초기 라이프 개수
    private final boolean isRanked;     // 공식 랭킹 대상 여부
    private final String badgeEmoji;    // 뱃지 이모지

    GenreChallengeDifficulty(String displayName, int playTimeMs, int answerTimeMs,
                             int initialLives, boolean isRanked, String badgeEmoji) {
        this.displayName = displayName;
        this.playTimeMs = playTimeMs;
        this.answerTimeMs = answerTimeMs;
        this.initialLives = initialLives;
        this.isRanked = isRanked;
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

    /**
     * 총 제한 시간 (듣기 + 입력)
     */
    public int getTotalTimeMs() {
        return playTimeMs + answerTimeMs;
    }

    public int getInitialLives() {
        return initialLives;
    }

    public boolean isRanked() {
        return isRanked;
    }

    public String getBadgeEmoji() {
        return badgeEmoji;
    }

    /**
     * 문자열로부터 난이도 파싱 (기본값: NORMAL)
     */
    public static GenreChallengeDifficulty fromString(String value) {
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
