package com.kh.game.repository;

import com.kh.game.entity.Genre;
import com.kh.game.entity.Song;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SongRepository - 레트로 게임 쿼리 테스트")
class RetroSongRepositoryTest {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private GenreRepository genreRepository;

    private Genre kpopGenre;
    private Genre retroGenre;
    private Genre popGenre;

    @BeforeEach
    void setUp() {
        // 장르 생성
        kpopGenre = createGenre("KPOP", "K-POP");
        retroGenre = createGenre("RETRO", "Retro/Oldies");
        popGenre = createGenre("POP", "POP");
    }

    private Genre createGenre(String code, String name) {
        Genre genre = new Genre();
        genre.setCode(code);
        genre.setName(name);
        genre.setUseYn("Y");
        return genreRepository.save(genre);
    }

    private Song createSong(String title, String artist, Genre genre, Integer releaseYear, String youtubeVideoId) {
        Song song = new Song();
        song.setTitle(title);
        song.setArtist(artist);
        song.setGenre(genre);
        song.setReleaseYear(releaseYear);
        song.setUseYn("Y");
        song.setYoutubeVideoId(youtubeVideoId);
        song.setIsPopular(true);
        return songRepository.save(song);
    }

    private Song createSongWithDetails(String title, String artist, Genre genre, Integer releaseYear,
                                        String youtubeVideoId, String filePath, String useYn, Boolean isPopular) {
        Song song = new Song();
        song.setTitle(title);
        song.setArtist(artist);
        song.setGenre(genre);
        song.setReleaseYear(releaseYear);
        song.setYoutubeVideoId(youtubeVideoId);
        song.setFilePath(filePath);
        song.setUseYn(useYn);
        song.setIsPopular(isPopular);
        return songRepository.save(song);
    }

    // =====================================================
    // Scenario 1: 기본 레트로 곡 쿼리
    // =====================================================
    @Nested
    @DisplayName("기본 레트로 곡 쿼리 - releaseYear < 2000 OR genre = RETRO")
    class BasicRetroSongQueryScenarios {

        @Test
        @DisplayName("releaseYear < 2000인 곡은 포함되어야 함")
        void songsBeforeYear2000_shouldBeIncluded() {
            // Given: 1999년 발매 KPOP 곡
            Song song1999 = createSong("1999년 히트곡", "90년대 가수", kpopGenre, 1999, "video001");
            // 2023년 발매 곡 (레트로 아님)
            createSong("최신곡", "현대 가수", kpopGenre, 2023, "video002");

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs).contains(song1999);
        }

        @Test
        @DisplayName("releaseYear = 2000인 곡은 포함되지 않아야 함 (경계값 테스트)")
        void songsInYear2000_shouldNotBeIncludedByYear() {
            // Given: 2000년 발매 KPOP 곡 (RETRO 장르 아님)
            Song song2000 = createSong("2000년 곡", "아티스트", kpopGenre, 2000, "video002");

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then: 2000은 < 2000이 아니므로 제외
            assertThat(retroSongs).doesNotContain(song2000);
        }

        @Test
        @DisplayName("RETRO 장르 곡은 연도와 관계없이 포함되어야 함")
        void retroGenreSongs_shouldBeIncludedRegardlessOfYear() {
            // Given: 2023년 RETRO 장르 곡
            Song retroSong2023 = createSong("뉴트로 곡", "아티스트", retroGenre, 2023, "video003");

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then: RETRO 장르이므로 포함
            assertThat(retroSongs).contains(retroSong2023);
        }

        @Test
        @DisplayName("두 조건이 OR 로직으로 동작해야 함")
        void bothConditions_shouldWorkWithOrLogic() {
            // Given
            Song oldSong = createSong("1990년 곡", "아티스트1", kpopGenre, 1990, "video004");
            Song retroGenreSong = createSong("레트로 장르 2020", "아티스트2", retroGenre, 2020, "video005");
            Song modernKpop = createSong("현대 KPOP", "아티스트3", kpopGenre, 2023, "video006");

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs)
                .contains(oldSong)           // releaseYear < 2000
                .contains(retroGenreSong)    // genre = RETRO
                .doesNotContain(modernKpop); // 둘 다 해당 안됨
        }
    }

    // =====================================================
    // Scenario 2: 발매 연도 엣지 케이스
    // =====================================================
    @Nested
    @DisplayName("발매 연도 엣지 케이스")
    class ReleaseYearEdgeCases {

        @Test
        @DisplayName("releaseYear = 1999는 포함되어야 함 (경계값)")
        void releaseYear1999_shouldBeIncluded() {
            // Given
            Song song1999 = createSong("1999 히트", "아티스트", kpopGenre, 1999, "video010");

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs).contains(song1999);
        }

        @Test
        @DisplayName("releaseYear = null이고 RETRO 장르가 아니면 포함되지 않아야 함")
        void releaseYearNull_shouldNotBeIncluded() {
            // Given: releaseYear가 null이고 RETRO 장르가 아닌 곡
            Song nullYearSong = createSong("연도 미상 곡", "아티스트", kpopGenre, null, "video011");

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs).doesNotContain(nullYearSong);
        }

        @Test
        @DisplayName("releaseYear = null이지만 RETRO 장르면 포함되어야 함")
        void releaseYearNullWithRetroGenre_shouldBeIncluded() {
            // Given
            Song nullYearRetro = createSong("연도 미상 레트로", "아티스트", retroGenre, null, "video012");

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs).contains(nullYearRetro);
        }

        @Test
        @DisplayName("매우 오래된 곡 (1980년)도 포함되어야 함")
        void veryOldSongs_shouldBeIncluded() {
            // Given
            Song song1980 = createSong("1980년대 클래식", "아티스트", popGenre, 1980, "video013");

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs).contains(song1980);
        }

        @Test
        @DisplayName("1970년 이전 곡도 포함되어야 함")
        void veryVeryOldSongs_shouldBeIncluded() {
            // Given: 1965년 곡 - 연도 조건으로 레트로에 포함
            Song song1965 = createSong("1965년 클래식", "아티스트", kpopGenre, 1965, "video014");

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs).contains(song1965);
        }
    }

    // =====================================================
    // Scenario 3: UseYn 및 오디오 소스 필터링
    // =====================================================
    @Nested
    @DisplayName("UseYn 및 오디오 소스 필터링")
    class UseYnAndAudioSourceFiltering {

        @Test
        @DisplayName("useYn = 'N'인 곡은 제외되어야 함")
        void inactiveSongs_shouldBeExcluded() {
            // Given
            Song inactiveSong = createSongWithDetails("비활성 레트로", "아티스트",
                retroGenre, 1990, "video020", null, "N", true);

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs).doesNotContain(inactiveSong);
        }

        @Test
        @DisplayName("오디오 소스가 없는 곡은 제외되어야 함")
        void songsWithoutAudioSource_shouldBeExcluded() {
            // Given: YouTube ID도 없고 filePath도 없는 곡
            Song noAudioSong = createSongWithDetails("오디오 없는 레트로", "아티스트",
                retroGenre, 1990, null, null, "Y", true);

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs).doesNotContain(noAudioSong);
        }

        @Test
        @DisplayName("filePath만 있는 곡도 포함되어야 함")
        void songsWithFilePathOnly_shouldBeIncluded() {
            // Given
            Song filePathSong = createSongWithDetails("MP3 레트로", "아티스트",
                retroGenre, 1990, null, "/songs/retro.mp3", "Y", true);

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs).contains(filePathSong);
        }

        @Test
        @DisplayName("YouTube ID만 있는 곡도 포함되어야 함")
        void songsWithYoutubeOnly_shouldBeIncluded() {
            // Given
            Song youtubeSong = createSongWithDetails("YouTube 레트로", "아티스트",
                retroGenre, 1990, "video021", null, "Y", true);

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs).contains(youtubeSong);
        }
    }

    // =====================================================
    // Scenario 4: isPopular 필터링 (매니악 곡)
    // =====================================================
    @Nested
    @DisplayName("isPopular 필터링 (매니악 곡 제외)")
    class IsPopularFiltering {

        @Test
        @DisplayName("isPopular = false (매니악) 곡은 대중곡 전용 쿼리에서 제외되어야 함")
        void maniacSongs_shouldBeExcludedFromPopularQuery() {
            // Given
            Song maniacRetro = createSongWithDetails("매니악 레트로", "아티스트",
                retroGenre, 1995, "video030", null, "Y", false);

            // When
            List<Song> retroSongs = songRepository.findPopularRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs).doesNotContain(maniacRetro);
        }

        @Test
        @DisplayName("isPopular = true인 곡은 포함되어야 함")
        void popularSongs_shouldBeIncluded() {
            // Given
            Song popularRetro = createSongWithDetails("대중 레트로", "아티스트",
                retroGenre, 1995, "video031", null, "Y", true);

            // When
            List<Song> retroSongs = songRepository.findPopularRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs).contains(popularRetro);
        }

        @Test
        @DisplayName("isPopular = null인 곡은 대중곡으로 취급되어 포함되어야 함")
        void nullPopularSongs_shouldBeIncluded() {
            // Given
            Song nullPopularRetro = createSongWithDetails("null popular 레트로", "아티스트",
                retroGenre, 1995, "video032", null, "Y", null);

            // When
            List<Song> retroSongs = songRepository.findPopularRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs).contains(nullPopularRetro);
        }

        @Test
        @DisplayName("매니악 곡도 전체 레트로 쿼리에서는 포함되어야 함")
        void maniacSongs_shouldBeIncludedInAllRetroQuery() {
            // Given
            Song maniacRetro = createSongWithDetails("매니악 레트로", "아티스트",
                retroGenre, 1995, "video033", null, "Y", false);

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then
            assertThat(retroSongs).contains(maniacRetro);
        }
    }

    // =====================================================
    // Scenario 5: 제외 목록 지원 (이미 플레이한 곡)
    // =====================================================
    @Nested
    @DisplayName("제외 목록 지원 (이미 플레이한 곡)")
    class ExclusionSetSupport {

        @Test
        @DisplayName("이미 플레이한 곡은 제외되어야 함")
        void alreadyPlayedSongs_shouldBeExcluded() {
            // Given
            Song playedSong = createSong("플레이한 레트로", "아티스트", retroGenre, 1995, "video040");
            Song newSong = createSong("새 레트로", "아티스트", retroGenre, 1996, "video041");
            Set<Long> excludeIds = Set.of(playedSong.getId());

            // When
            List<Song> retroSongs = songRepository.findRetroSongsExcluding("Y", excludeIds);

            // Then
            assertThat(retroSongs)
                .doesNotContain(playedSong)
                .contains(newSong);
        }

        @Test
        @DisplayName("빈 제외 목록은 모든 레트로 곡을 반환해야 함")
        void emptyExclusionSet_shouldReturnAllRetroSongs() {
            // Given
            Song song1 = createSong("레트로 1", "아티스트", retroGenre, 1995, "video042");
            Song song2 = createSong("레트로 2", "아티스트", retroGenre, 1996, "video043");

            // When
            List<Song> retroSongs = songRepository.findRetroSongsExcluding("Y", Set.of());

            // Then
            assertThat(retroSongs).contains(song1, song2);
        }

        @Test
        @DisplayName("모든 곡이 제외되면 빈 목록 반환")
        void allSongsExcluded_shouldReturnEmptyList() {
            // Given
            Song song1 = createSong("레트로 1", "아티스트", retroGenre, 1995, "video044");
            Song song2 = createSong("레트로 2", "아티스트", retroGenre, 1996, "video045");
            Set<Long> excludeIds = Set.of(song1.getId(), song2.getId());

            // When
            List<Song> retroSongs = songRepository.findRetroSongsExcluding("Y", excludeIds);

            // Then
            assertThat(retroSongs).isEmpty();
        }
    }

    // =====================================================
    // Scenario 6: 레트로 곡 개수 조회
    // =====================================================
    @Nested
    @DisplayName("레트로 곡 개수 조회")
    class RetroSongCount {

        @Test
        @DisplayName("레트로 곡 총 개수 반환")
        void countRetroSongs_shouldReturnCorrectCount() {
            // Given
            createSong("레트로 1", "아티스트", retroGenre, 2020, "video050");
            createSong("레트로 2", "아티스트", kpopGenre, 1995, "video051");
            createSong("현대 곡", "아티스트", kpopGenre, 2023, "video052");

            // When
            long count = songRepository.countRetroSongsForGame("Y");

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("레트로 곡이 없으면 0 반환")
        void noRetroSongs_shouldReturnZero() {
            // Given: 모두 현대 곡
            createSong("현대 1", "아티스트", kpopGenre, 2020, "video053");
            createSong("현대 2", "아티스트", popGenre, 2021, "video054");

            // When
            long count = songRepository.countRetroSongsForGame("Y");

            // Then
            assertThat(count).isEqualTo(0);
        }
    }

    // =====================================================
    // Scenario 7: 복합 조건 (두 조건 모두 만족)
    // =====================================================
    @Nested
    @DisplayName("복합 조건 - 두 조건 모두 만족하는 곡")
    class BothConditionsSongs {

        @Test
        @DisplayName("두 조건 모두 만족하는 곡은 한 번만 반환되어야 함")
        void songWithBothConditions_shouldAppearOnce() {
            // Given: 1995년 + RETRO 장르 (두 조건 모두 만족)
            Song bothConditions = createSong("두 조건 모두", "아티스트", retroGenre, 1995, "video060");

            // When
            List<Song> retroSongs = songRepository.findRetroSongsForGame("Y");

            // Then: 중복 없이 한 번만 나와야 함
            long count = retroSongs.stream()
                .filter(s -> s.getId().equals(bothConditions.getId()))
                .count();
            assertThat(count).isEqualTo(1);
        }
    }
}
