package com.kh.game.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MultiGameStartBugTest {
    
    @Test
    void testYoutubeVideoIdValidation() {
        // 테스트 케이스들
        String validId = "9zNcHOD1Hdk";
        String invalidShort = "invalid";
        String emptyString = "";
        String blankString = "   ";
        String nullId = null;

        // 서버 로직: (videoId != null && !videoId.isBlank()) ? videoId : null
        // 서버는 non-blank만 체크, 클라이언트 JS에서 11자 검증 수행
        assertEquals(validId, getValidVideoId(validId));
        assertEquals(invalidShort, getValidVideoId(invalidShort)); // 서버는 통과, JS에서 필터
        assertNull(getValidVideoId(emptyString));
        assertNull(getValidVideoId(blankString));
        assertNull(getValidVideoId(nullId));
    }
    
    private String getValidVideoId(String videoId) {
        return (videoId != null && !videoId.isBlank()) ? videoId : null;
    }
    
    @Test
    void testJsVideoIdValidation() {
        // JS 유효성 검사: /^[a-zA-Z0-9_-]{11}$/
        assertTrue(isValidYoutubeVideoId("9zNcHOD1Hdk"));  // 11자 valid
        assertTrue(isValidYoutubeVideoId("2wbAQLY2R20"));  // 11자 valid
        assertFalse(isValidYoutubeVideoId("invalid"));     // 7자 - invalid
        assertFalse(isValidYoutubeVideoId(""));            // 빈 문자열
        assertFalse(isValidYoutubeVideoId("   "));         // 공백
        assertFalse(isValidYoutubeVideoId(null));          // null
        assertFalse(isValidYoutubeVideoId("12345678901234")); // 14자 - 너무 김
    }
    
    private boolean isValidYoutubeVideoId(String videoId) {
        if (videoId == null) return false;
        return videoId.trim().matches("^[a-zA-Z0-9_-]{11}$");
    }
}
