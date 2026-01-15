package com.kh.game.service;

import com.kh.game.entity.*;
import com.kh.game.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * TDD Phase 1: Soft Delete 기본 동작 테스트
 *
 * 정책:
 * - 곡 삭제 시 useYn='N'으로 변경 (물리적 삭제 X)
 * - 삭제된 곡은 게임/랭킹에서 제외
 * - 퍼펙트: 삭제 시 유지, 추가 시만 무효화
 * - 랭킹: 현재 곡 수 기준 (max 100%)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Soft Delete 기본 동작 테스트")
class SongSoftDeleteTest {

    @Autowired
    private SongService songService;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private SongAnswerRepository songAnswerRepository;

    @Autowired
    private GenreRepository genreRepository;

    @MockitoBean
    private YouTubeValidationService youTubeValidationService;

    private Genre kpopGenre;
    private int songCounter = 0;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        songAnswerRepository.deleteAll();
        songRepository.deleteAll();

        // YouTube 검증 Mock
        YouTubeValidationService.ValidationResult validResult = YouTubeValidationService.ValidationResult.valid();
        Mockito.when(youTubeValidationService.validateVideo(anyString())).thenReturn(validResult);

        // 장르 생성/조회
        kpopGenre = genreRepository.findByCode("KPOP").orElseGet(() -> {
            Genre g = new Genre();
            g.setCode("KPOP");
            g.setName("K-POP");
            g.setUseYn("Y");
            return genreRepository.save(g);
        });
    }

    private Song createSong(String title, String artist) {
        Song song = new Song();
        song.setTitle(title);
        song.setArtist(artist);
        song.setGenre(kpopGenre);
        song.setUseYn("Y");
        song.setIsSolo(false);
        song.setYoutubeVideoId("vid_" + (++songCounter));
        song.setStartTime(0);
        song.setPlayDuration(30);
        song.setIsYoutubeValid(true);
        Song saved = songRepository.save(song);

        // 정답 추가
        SongAnswer answer = new SongAnswer();
        answer.setSong(saved);
        answer.setAnswer(title);
        answer.setIsPrimary(true);
        songAnswerRepository.save(answer);

        return saved;
    }

    // =====================================================
    // 1. 기본 Soft Delete 동작
    // =====================================================
    @Nested
    @DisplayName("1. 기본 Soft Delete 동작")
    class BasicSoftDeleteTests {

        @Test
        @DisplayName("1.1 soft delete 시 useYn='N'으로 변경되어야 함")
        void softDelete_shouldSetUseYnToN() {
            // Given
            Song song = createSong("Test Song", "Test Artist");
            Long songId = song.getId();

            // When: soft delete 실행
            songService.softDeleteSong(songId);

            // Then
            Song deleted = songRepository.findById(songId).orElseThrow();
            assertThat(deleted.getUseYn()).isEqualTo("N");
        }

        @Test
        @DisplayName("1.2 soft delete된 곡은 활성 목록에서 제외되어야 함")
        void softDeletedSong_shouldNotAppearInActiveList() {
            // Given
            createSong("Song 1", "BTS");
            Song song2 = createSong("Song 2", "BTS");
            createSong("Song 3", "BTS");

            // When: Song 2 soft delete
            songService.softDeleteSong(song2.getId());

            // Then: 활성 목록에서 제외
            List<Song> activeSongs = songService.getAllValidatedSongsByArtist("BTS");
            assertThat(activeSongs).hasSize(2);
            assertThat(activeSongs).extracting(Song::getTitle)
                    .containsExactlyInAnyOrder("Song 1", "Song 3");
        }

        @Test
        @DisplayName("1.3 soft delete 후에도 DB에 데이터가 존재해야 함")
        void softDeletedSong_shouldStillExistInDatabase() {
            // Given
            Song song = createSong("Test Song", "Test Artist");
            Long songId = song.getId();

            // When
            songService.softDeleteSong(songId);

            // Then: findById로 조회 가능
            Optional<Song> found = songRepository.findById(songId);
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Test Song");
        }

        @Test
        @DisplayName("1.4 복구 시 useYn='Y'로 변경되어야 함")
        void restore_shouldSetUseYnToY() {
            // Given
            Song song = createSong("Test Song", "Test Artist");
            songService.softDeleteSong(song.getId());

            // When: 복구
            songService.restoreSong(song.getId());

            // Then
            Song restored = songRepository.findById(song.getId()).orElseThrow();
            assertThat(restored.getUseYn()).isEqualTo("Y");
        }
    }

    // =====================================================
    // 2. 아티스트 곡 수 조회
    // =====================================================
    @Nested
    @DisplayName("2. 아티스트 곡 수 조회")
    class ArtistSongCountTests {

        @Test
        @DisplayName("2.1 곡 수 조회 시 soft delete된 곡은 제외되어야 함")
        void getArtistsWithCount_shouldExcludeSoftDeleted() {
            // Given: BTS 5곡 생성
            for (int i = 1; i <= 5; i++) {
                createSong("BTS Song " + i, "BTS");
            }

            // soft delete 1곡
            List<Song> btsSongs = songRepository.findByArtistAndUseYn("BTS", "Y");
            songService.softDeleteSong(btsSongs.get(0).getId());

            // When
            List<Map<String, Object>> artistsWithCount = songService.getArtistsWithCount();

            // Then
            Optional<Map<String, Object>> bts = artistsWithCount.stream()
                    .filter(m -> "BTS".equals(m.get("name")))
                    .findFirst();
            assertThat(bts).isPresent();
            assertThat(((Number) bts.get().get("count")).intValue()).isEqualTo(4);
        }

        @Test
        @DisplayName("2.2 전곡 soft delete 시 아티스트가 목록에서 제외되어야 함")
        void getArtistsWithCount_allSoftDeleted_shouldNotShowArtist() {
            // Given: BTS 3곡 생성
            Song song1 = createSong("BTS Song 1", "BTS");
            Song song2 = createSong("BTS Song 2", "BTS");
            Song song3 = createSong("BTS Song 3", "BTS");

            // IU 2곡 생성 (비교용)
            createSong("IU Song 1", "IU");
            createSong("IU Song 2", "IU");

            // When: BTS 전곡 soft delete
            songService.softDeleteSong(song1.getId());
            songService.softDeleteSong(song2.getId());
            songService.softDeleteSong(song3.getId());

            // Then
            List<Map<String, Object>> artistsWithCount = songService.getArtistsWithCount();
            boolean hasBTS = artistsWithCount.stream()
                    .anyMatch(m -> "BTS".equals(m.get("name")));
            assertThat(hasBTS).isFalse();

            // IU는 존재
            boolean hasIU = artistsWithCount.stream()
                    .anyMatch(m -> "IU".equals(m.get("name")));
            assertThat(hasIU).isTrue();
        }

        @Test
        @DisplayName("2.3 게임용 곡 조회 시 soft delete된 곡은 제외되어야 함")
        void getAllValidatedSongsByArtist_shouldExcludeSoftDeleted() {
            // Given
            createSong("Song 1", "BTS");
            Song deletedSong = createSong("Song 2", "BTS");
            createSong("Song 3", "BTS");

            songService.softDeleteSong(deletedSong.getId());

            // When
            List<Song> songs = songService.getAllValidatedSongsByArtist("BTS");

            // Then
            assertThat(songs).hasSize(2);
            assertThat(songs).extracting(Song::getId)
                    .doesNotContain(deletedSong.getId());
        }
    }

    // =====================================================
    // 3. 엣지 케이스
    // =====================================================
    @Nested
    @DisplayName("3. 엣지 케이스")
    class EdgeCaseTests {

        @Test
        @DisplayName("3.1 이미 삭제된 곡을 다시 삭제해도 안전해야 함")
        void softDelete_alreadyDeleted_shouldBeSafe() {
            // Given
            Song song = createSong("Test Song", "Test Artist");
            songService.softDeleteSong(song.getId());

            // When: 다시 삭제 시도
            songService.softDeleteSong(song.getId());

            // Then: 여전히 N
            Song deleted = songRepository.findById(song.getId()).orElseThrow();
            assertThat(deleted.getUseYn()).isEqualTo("N");
        }

        @Test
        @DisplayName("3.2 이미 활성화된 곡을 다시 복구해도 안전해야 함")
        void restore_alreadyActive_shouldBeSafe() {
            // Given
            Song song = createSong("Test Song", "Test Artist");

            // When: 활성 상태에서 복구 시도
            songService.restoreSong(song.getId());

            // Then: 여전히 Y
            Song restored = songRepository.findById(song.getId()).orElseThrow();
            assertThat(restored.getUseYn()).isEqualTo("Y");
        }

        @Test
        @DisplayName("3.3 존재하지 않는 곡 삭제 시 예외 발생")
        void softDelete_nonExistent_shouldThrowException() {
            // When & Then
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> songService.softDeleteSong(999999L)
            );
        }

        @Test
        @DisplayName("3.4 마지막 1곡 삭제 시 아티스트 목록에서 제외")
        void softDelete_lastSong_shouldRemoveArtistFromList() {
            // Given
            Song onlySong = createSong("Only Song", "Solo Artist");

            // When
            songService.softDeleteSong(onlySong.getId());

            // Then
            List<Map<String, Object>> artists = songService.getArtistsWithCount();
            boolean hasSoloArtist = artists.stream()
                    .anyMatch(m -> "Solo Artist".equals(m.get("name")));
            assertThat(hasSoloArtist).isFalse();
        }
    }
}
