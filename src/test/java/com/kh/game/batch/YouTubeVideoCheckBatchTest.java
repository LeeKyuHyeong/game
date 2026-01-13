package com.kh.game.batch;

import com.kh.game.entity.Song;
import com.kh.game.repository.SongRepository;
import com.kh.game.service.BatchService;
import com.kh.game.service.YouTubeValidationService;
import com.kh.game.service.YouTubeValidationService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * YouTubeVideoCheckBatch TDD 테스트
 *
 * 검증 로직:
 * - oEmbed API + 썸네일 이중 체크
 * - 둘 다 실패해야 비활성화
 */
@ExtendWith(MockitoExtension.class)
class YouTubeVideoCheckBatchTest {

    @Mock
    private SongRepository songRepository;

    @Mock
    private BatchService batchService;

    @Mock
    private YouTubeValidationService youTubeValidationService;

    private YouTubeVideoCheckBatch batch;

    @BeforeEach
    void setUp() {
        batch = new YouTubeVideoCheckBatch(songRepository, batchService, youTubeValidationService);
    }

    @Test
    @DisplayName("oEmbed 성공 + 썸네일 성공 → 활성 상태 유지")
    void shouldKeepActiveWhenBothChecksPass() {
        // given
        Song song = createSong(1L, "dQw4w9WgXcQ", "Y");
        when(songRepository.findByUseYnAndYoutubeVideoIdIsNotNull("Y"))
                .thenReturn(Collections.singletonList(song));
        when(youTubeValidationService.validateVideo("dQw4w9WgXcQ"))
                .thenReturn(ValidationResult.valid());

        // when
        int disabledCount = batch.execute(null);

        // then
        assertThat(disabledCount).isZero();
        assertThat(song.getUseYn()).isEqualTo("Y");
        verify(songRepository, never()).save(any(Song.class));
    }

    @Test
    @DisplayName("oEmbed 실패 + 썸네일 성공 → 활성 상태 유지 (하나만 성공해도 OK)")
    void shouldKeepActiveWhenOnlyThumbnailPasses() {
        // given
        Song song = createSong(1L, "validVideo123", "Y");
        when(songRepository.findByUseYnAndYoutubeVideoIdIsNotNull("Y"))
                .thenReturn(Collections.singletonList(song));
        when(youTubeValidationService.validateVideo("validVideo123"))
                .thenReturn(ValidationResult.valid()); // 썸네일은 성공

        // when
        int disabledCount = batch.execute(null);

        // then
        assertThat(disabledCount).isZero();
        assertThat(song.getUseYn()).isEqualTo("Y");
    }

    @Test
    @DisplayName("oEmbed 실패 + 썸네일 실패 → 비활성화")
    void shouldDisableWhenBothChecksFail() {
        // given
        Song song = createSong(1L, "deletedVideo", "Y");
        when(songRepository.findByUseYnAndYoutubeVideoIdIsNotNull("Y"))
                .thenReturn(Collections.singletonList(song));
        when(youTubeValidationService.validateVideo("deletedVideo"))
                .thenReturn(ValidationResult.invalid("oEmbed 실패", "썸네일 실패"));

        // when
        int disabledCount = batch.execute(null);

        // then
        assertThat(disabledCount).isEqualTo(1);
        assertThat(song.getUseYn()).isEqualTo("N");
        verify(songRepository).save(song);
    }

    @Test
    @DisplayName("YouTube ID가 없는 노래는 스킵")
    void shouldSkipSongsWithoutYoutubeId() {
        // given
        when(songRepository.findByUseYnAndYoutubeVideoIdIsNotNull("Y"))
                .thenReturn(Collections.emptyList());

        // when
        int disabledCount = batch.execute(null);

        // then
        assertThat(disabledCount).isZero();
        verify(youTubeValidationService, never()).validateVideo(any());
    }

    @Test
    @DisplayName("여러 노래 중 일부만 비활성화")
    void shouldDisableOnlyInvalidVideos() {
        // given
        Song validSong1 = createSong(1L, "valid1", "Y");
        Song invalidSong = createSong(2L, "invalid", "Y");
        Song validSong2 = createSong(3L, "valid2", "Y");

        when(songRepository.findByUseYnAndYoutubeVideoIdIsNotNull("Y"))
                .thenReturn(Arrays.asList(validSong1, invalidSong, validSong2));

        when(youTubeValidationService.validateVideo("valid1"))
                .thenReturn(ValidationResult.valid());
        when(youTubeValidationService.validateVideo("invalid"))
                .thenReturn(ValidationResult.invalid("삭제된 영상", "썸네일 없음"));
        when(youTubeValidationService.validateVideo("valid2"))
                .thenReturn(ValidationResult.valid());

        // when
        int disabledCount = batch.execute(null);

        // then
        assertThat(disabledCount).isEqualTo(1);
        assertThat(validSong1.getUseYn()).isEqualTo("Y");
        assertThat(invalidSong.getUseYn()).isEqualTo("N");
        assertThat(validSong2.getUseYn()).isEqualTo("Y");

        // invalidSong만 저장됨
        ArgumentCaptor<Song> songCaptor = ArgumentCaptor.forClass(Song.class);
        verify(songRepository, times(1)).save(songCaptor.capture());
        assertThat(songCaptor.getValue().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("임베드 불가 영상 비활성화")
    void shouldDisableEmbedDisabledVideos() {
        // given
        Song song = createSong(1L, "noEmbedVideo", "Y");
        when(songRepository.findByUseYnAndYoutubeVideoIdIsNotNull("Y"))
                .thenReturn(Collections.singletonList(song));
        when(youTubeValidationService.validateVideo("noEmbedVideo"))
                .thenReturn(ValidationResult.embedDisabled());

        // when
        int disabledCount = batch.execute(null);

        // then
        assertThat(disabledCount).isEqualTo(1);
        assertThat(song.getUseYn()).isEqualTo("N");
    }

    private Song createSong(Long id, String youtubeVideoId, String useYn) {
        Song song = new Song();
        song.setId(id);
        song.setYoutubeVideoId(youtubeVideoId);
        song.setUseYn(useYn);
        song.setTitle("Test Song " + id);
        song.setArtist("Test Artist");
        return song;
    }
}
