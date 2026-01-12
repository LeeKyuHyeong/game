package com.kh.game.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * YouTube 영상 유효성 검증 서비스
 *
 * 이중 체크 로직:
 * 1. oEmbed API: 영상 존재 및 임베드 가능 여부 확인
 * 2. 썸네일 체크: 삭제된 영상 placeholder 감지
 *
 * 둘 다 실패해야 비활성화
 */
@Slf4j
@Service
public class YouTubeValidationService {

    private static final String OEMBED_URL = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=%s&format=json";
    private static final String THUMBNAIL_URL = "https://img.youtube.com/vi/%s/mqdefault.jpg";

    // 삭제된 영상의 placeholder 썸네일 크기 임계값 (약 1KB 미만)
    private static final int DELETED_THUMBNAIL_SIZE_THRESHOLD = 1024;

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public YouTubeValidationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    // 테스트용 생성자
    public YouTubeValidationService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * YouTube 영상 유효성 검증
     *
     * @param videoId YouTube 영상 ID
     * @return 검증 결과
     */
    public ValidationResult validateVideo(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            return ValidationResult.invalid("videoId가 비어있음", "videoId가 비어있음");
        }

        // 1단계: oEmbed API 체크
        OEmbedResult oEmbedResult = checkOEmbed(videoId);

        // oEmbed 성공 시 바로 유효 판정
        if (oEmbedResult.isSuccess()) {
            return ValidationResult.valid();
        }

        // 임베드 불가 (401/403)인 경우 바로 실패
        if (oEmbedResult.isEmbedDisabled()) {
            return ValidationResult.embedDisabled();
        }

        // 2단계: oEmbed 실패 시 썸네일 체크로 재확인
        ThumbnailResult thumbnailResult = checkThumbnail(videoId);

        if (thumbnailResult.isValid()) {
            // 썸네일은 유효 → 영상 존재
            return ValidationResult.valid();
        }

        // 둘 다 실패 → 삭제된 영상
        return ValidationResult.invalid(oEmbedResult.getError(), thumbnailResult.getError());
    }

    /**
     * oEmbed API 체크
     */
    private OEmbedResult checkOEmbed(String videoId) {
        try {
            String url = String.format(OEMBED_URL, videoId);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 200) {
                return OEmbedResult.success();
            } else if (statusCode == 401 || statusCode == 403) {
                return OEmbedResult.embedDisabled();
            } else {
                return OEmbedResult.failed("HTTP " + statusCode);
            }
        } catch (Exception e) {
            log.debug("oEmbed 체크 실패 [{}]: {}", videoId, e.getMessage());
            return OEmbedResult.failed("네트워크 오류: " + e.getMessage());
        }
    }

    /**
     * 썸네일 체크
     * - 삭제된 영상은 매우 작은 placeholder 이미지를 반환함
     */
    private ThumbnailResult checkThumbnail(String videoId) {
        try {
            String url = String.format(THUMBNAIL_URL, videoId);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int statusCode = response.statusCode();

            if (statusCode != 200) {
                return ThumbnailResult.invalid("HTTP " + statusCode);
            }

            byte[] body = response.body();
            if (body == null || body.length < DELETED_THUMBNAIL_SIZE_THRESHOLD) {
                return ThumbnailResult.invalid("썸네일 크기 너무 작음 (" + (body != null ? body.length : 0) + " bytes)");
            }

            return ThumbnailResult.valid();
        } catch (Exception e) {
            log.debug("썸네일 체크 실패 [{}]: {}", videoId, e.getMessage());
            return ThumbnailResult.invalid("네트워크 오류: " + e.getMessage());
        }
    }

    // ===== 결과 클래스들 =====

    /**
     * 최종 검증 결과
     */
    public static class ValidationResult {
        private final boolean valid;
        private final boolean embedDisabled;
        private final String oEmbedError;
        private final String thumbnailError;

        private ValidationResult(boolean valid, boolean embedDisabled, String oEmbedError, String thumbnailError) {
            this.valid = valid;
            this.embedDisabled = embedDisabled;
            this.oEmbedError = oEmbedError;
            this.thumbnailError = thumbnailError;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, false, null, null);
        }

        public static ValidationResult invalid(String oEmbedError, String thumbnailError) {
            return new ValidationResult(false, false, oEmbedError, thumbnailError);
        }

        public static ValidationResult embedDisabled() {
            return new ValidationResult(false, true, "임베드 불가", null);
        }

        public boolean isValid() {
            return valid;
        }

        public boolean isEmbedDisabled() {
            return embedDisabled;
        }

        public String getOEmbedError() {
            return oEmbedError;
        }

        public String getThumbnailError() {
            return thumbnailError;
        }

        @Override
        public String toString() {
            if (valid) {
                return "ValidationResult[유효]";
            } else if (embedDisabled) {
                return "ValidationResult[임베드 불가]";
            } else {
                return String.format("ValidationResult[무효: oEmbed=%s, 썸네일=%s]", oEmbedError, thumbnailError);
            }
        }
    }

    /**
     * oEmbed 체크 결과 (내부용)
     */
    private static class OEmbedResult {
        private final boolean success;
        private final boolean embedDisabled;
        private final String error;

        private OEmbedResult(boolean success, boolean embedDisabled, String error) {
            this.success = success;
            this.embedDisabled = embedDisabled;
            this.error = error;
        }

        static OEmbedResult success() {
            return new OEmbedResult(true, false, null);
        }

        static OEmbedResult embedDisabled() {
            return new OEmbedResult(false, true, "임베드 불가");
        }

        static OEmbedResult failed(String error) {
            return new OEmbedResult(false, false, error);
        }

        boolean isSuccess() {
            return success;
        }

        boolean isEmbedDisabled() {
            return embedDisabled;
        }

        String getError() {
            return error;
        }
    }

    /**
     * 썸네일 체크 결과 (내부용)
     */
    private static class ThumbnailResult {
        private final boolean valid;
        private final String error;

        private ThumbnailResult(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }

        static ThumbnailResult valid() {
            return new ThumbnailResult(true, null);
        }

        static ThumbnailResult invalid(String error) {
            return new ThumbnailResult(false, error);
        }

        boolean isValid() {
            return valid;
        }

        String getError() {
            return error;
        }
    }
}
