package com.kh.game.service;

import com.kh.game.service.YouTubeValidationService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * YouTubeValidationService TDD 테스트
 *
 * 이중 체크 로직:
 * 1. oEmbed API: https://www.youtube.com/oembed?url=...
 * 2. 썸네일 체크: https://img.youtube.com/vi/{videoId}/mqdefault.jpg
 */
@ExtendWith(MockitoExtension.class)
class YouTubeValidationServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> oEmbedResponse;

    @Mock
    private HttpResponse<byte[]> thumbnailResponse;

    private YouTubeValidationService service;

    // 삭제된 영상의 기본 썸네일 크기 (대략 1KB 미만)
    private static final int DELETED_THUMBNAIL_SIZE_THRESHOLD = 1024;

    @BeforeEach
    void setUp() {
        service = new YouTubeValidationService(httpClient);
    }

    @Test
    @DisplayName("oEmbed 200 OK → 유효한 영상")
    void shouldReturnValidWhenOEmbedReturns200() throws Exception {
        // given
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(oEmbedResponse);
        when(oEmbedResponse.statusCode()).thenReturn(200);

        // when
        ValidationResult result = service.validateVideo("dQw4w9WgXcQ");

        // then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("oEmbed 404 + 썸네일 정상 크기 → 유효한 영상 (하나라도 성공)")
    void shouldReturnValidWhenOEmbedFailsButThumbnailValid() throws Exception {
        // given
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(oEmbedResponse);
        when(oEmbedResponse.statusCode()).thenReturn(404);

        byte[] validThumbnail = new byte[5000]; // 5KB - 유효한 썸네일
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofByteArray())))
                .thenReturn(thumbnailResponse);
        when(thumbnailResponse.statusCode()).thenReturn(200);
        when(thumbnailResponse.body()).thenReturn(validThumbnail);

        // when
        ValidationResult result = service.validateVideo("someVideoId");

        // then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("oEmbed 404 + 썸네일 작은 크기 → 삭제된 영상")
    void shouldReturnInvalidWhenBothChecksFail() throws Exception {
        // given
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(oEmbedResponse);
        when(oEmbedResponse.statusCode()).thenReturn(404);

        byte[] deletedThumbnail = new byte[500]; // 500B - 삭제된 영상의 placeholder
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofByteArray())))
                .thenReturn(thumbnailResponse);
        when(thumbnailResponse.statusCode()).thenReturn(200);
        when(thumbnailResponse.body()).thenReturn(deletedThumbnail);

        // when
        ValidationResult result = service.validateVideo("deletedVideo");

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getOEmbedError()).isNotNull();
        assertThat(result.getThumbnailError()).isNotNull();
    }

    @Test
    @DisplayName("oEmbed 401/403 → 임베드 불가 영상")
    void shouldReturnEmbedDisabledWhen401Or403() throws Exception {
        // given
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(oEmbedResponse);
        when(oEmbedResponse.statusCode()).thenReturn(401);

        // when
        ValidationResult result = service.validateVideo("privateVideo");

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.isEmbedDisabled()).isTrue();
    }

    @Test
    @DisplayName("oEmbed 403 → 임베드 불가 영상")
    void shouldReturnEmbedDisabledWhen403() throws Exception {
        // given
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(oEmbedResponse);
        when(oEmbedResponse.statusCode()).thenReturn(403);

        // when
        ValidationResult result = service.validateVideo("noEmbedVideo");

        // then
        assertThat(result.isValid()).isFalse();
        assertThat(result.isEmbedDisabled()).isTrue();
    }

    @Test
    @DisplayName("네트워크 오류 시 썸네일 체크로 폴백")
    void shouldFallbackToThumbnailOnNetworkError() throws Exception {
        // given - oEmbed에서 IOException 발생
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenThrow(new IOException("Connection timeout"));

        byte[] validThumbnail = new byte[5000];
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofByteArray())))
                .thenReturn(thumbnailResponse);
        when(thumbnailResponse.statusCode()).thenReturn(200);
        when(thumbnailResponse.body()).thenReturn(validThumbnail);

        // when
        ValidationResult result = service.validateVideo("videoId");

        // then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("둘 다 네트워크 오류 시 검증 실패")
    void shouldReturnInvalidWhenBothNetworkErrors() throws Exception {
        // given
        when(httpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new IOException("Connection refused"));

        // when
        ValidationResult result = service.validateVideo("videoId");

        // then
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("빈 videoId → 무효")
    void shouldReturnInvalidForEmptyVideoId() {
        // when
        ValidationResult result1 = service.validateVideo("");
        ValidationResult result2 = service.validateVideo(null);

        // then
        assertThat(result1.isValid()).isFalse();
        assertThat(result2.isValid()).isFalse();
    }

    @Test
    @DisplayName("썸네일 404 응답 → 삭제된 영상")
    void shouldReturnInvalidWhenThumbnail404() throws Exception {
        // given
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(oEmbedResponse);
        when(oEmbedResponse.statusCode()).thenReturn(404);

        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofByteArray())))
                .thenReturn(thumbnailResponse);
        when(thumbnailResponse.statusCode()).thenReturn(404);

        // when
        ValidationResult result = service.validateVideo("deletedVideo");

        // then
        assertThat(result.isValid()).isFalse();
    }
}
