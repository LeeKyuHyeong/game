package com.kh.game.repository;

import com.kh.game.entity.Genre;
import com.kh.game.entity.Song;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SongRepository 테스트")
class SongRepositoryTest {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private GenreRepository genreRepository;

    private Genre kpopGenre;
    private Genre popGenre;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // 장르 생성
        kpopGenre = new Genre();
        kpopGenre.setCode("KPOP");
        kpopGenre.setName("K-POP");
        kpopGenre.setUseYn("Y");
        kpopGenre = genreRepository.save(kpopGenre);

        popGenre = new Genre();
        popGenre.setCode("POP");
        popGenre.setName("POP");
        popGenre.setUseYn("Y");
        popGenre = genreRepository.save(popGenre);

        // 테스트 데이터 생성
        createSong("Dynamite", "BTS", kpopGenre, "Y", true, 2020, "abc123");
        createSong("Butter", "BTS", kpopGenre, "Y", false, 2021, "def456");
        createSong("Next Level", "aespa", kpopGenre, "Y", false, 2021, "ghi789");
        createSong("Blinding Lights", "The Weeknd", popGenre, "Y", true, 2020, "jkl012");
        createSong("Old Song", "Old Artist", null, "N", true, 2010, null);

        pageable = PageRequest.of(0, 20);
    }

    private Song createSong(String title, String artist, Genre genre, String useYn,
                            Boolean isSolo, Integer releaseYear, String youtubeVideoId) {
        Song song = new Song();
        song.setTitle(title);
        song.setArtist(artist);
        song.setGenre(genre);
        song.setUseYn(useYn);
        song.setIsSolo(isSolo);
        song.setReleaseYear(releaseYear);
        song.setYoutubeVideoId(youtubeVideoId);
        return songRepository.save(song);
    }

    @Test
    @DisplayName("searchWithFilters - 모든 파라미터가 null이면 전체 조회")
    void searchWithFilters_allNull_returnsAll() {
        Page<Song> result = songRepository.searchWithFilters(null, null, null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("searchWithFilters - keyword로 제목 검색")
    void searchWithFilters_byKeyword_title() {
        Page<Song> result = songRepository.searchWithFilters("Dynamite", null, null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Dynamite");
    }

    @Test
    @DisplayName("searchWithFilters - keyword로 아티스트 검색")
    void searchWithFilters_byKeyword_artist() {
        Page<Song> result = songRepository.searchWithFilters("BTS", null, null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("searchWithFilters - 아티스트로 필터링")
    void searchWithFilters_byArtist() {
        Page<Song> result = songRepository.searchWithFilters(null, "BTS", null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(s -> s.getArtist().equals("BTS"));
    }

    @Test
    @DisplayName("searchWithFilters - 장르로 필터링")
    void searchWithFilters_byGenre() {
        Page<Song> result = songRepository.searchWithFilters(null, null, kpopGenre.getId(), null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).allMatch(s -> s.getGenre().getId().equals(kpopGenre.getId()));
    }

    @Test
    @DisplayName("searchWithFilters - 사용여부로 필터링")
    void searchWithFilters_byUseYn() {
        Page<Song> result = songRepository.searchWithFilters(null, null, null, "Y", null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent()).allMatch(s -> s.getUseYn().equals("Y"));
    }

    @Test
    @DisplayName("searchWithFilters - 솔로여부로 필터링")
    void searchWithFilters_byIsSolo() {
        Page<Song> result = songRepository.searchWithFilters(null, null, null, null, true, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).allMatch(s -> s.getIsSolo().equals(true));
    }

    @Test
    @DisplayName("searchWithFilters - 연도로 필터링")
    void searchWithFilters_byReleaseYear() {
        Page<Song> result = songRepository.searchWithFilters(null, null, null, null, null, 2021, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(s -> s.getReleaseYear().equals(2021));
    }

    @Test
    @DisplayName("searchWithFilters - 복합 조건 필터링")
    void searchWithFilters_multipleConditions() {
        Page<Song> result = songRepository.searchWithFilters(null, null, kpopGenre.getId(), "Y", false, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(s ->
            s.getGenre().getId().equals(kpopGenre.getId()) &&
            s.getUseYn().equals("Y") &&
            s.getIsSolo().equals(false)
        );
    }

    @Test
    @DisplayName("searchWithFiltersMultipleArtists - 다중 아티스트 필터링")
    void searchWithFiltersMultipleArtists_returnsMatchingArtists() {
        List<String> artists = Arrays.asList("BTS", "aespa");

        Page<Song> result = songRepository.searchWithFiltersMultipleArtists(null, artists, null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).allMatch(s -> artists.contains(s.getArtist()));
    }

    @Test
    @DisplayName("searchWithFiltersMultipleArtists - 다중 아티스트 + 다른 조건")
    void searchWithFiltersMultipleArtists_withOtherConditions() {
        List<String> artists = Arrays.asList("BTS", "aespa");

        Page<Song> result = songRepository.searchWithFiltersMultipleArtists(null, artists, null, "Y", false, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(s ->
            artists.contains(s.getArtist()) &&
            s.getUseYn().equals("Y") &&
            s.getIsSolo().equals(false)
        );
    }

    @Test
    @DisplayName("findAllDistinctYears - 전체 연도 목록 조회")
    void findAllDistinctYears() {
        List<Integer> result = songRepository.findAllDistinctYears();

        assertThat(result).isNotEmpty();
        assertThat(result).contains(2020, 2021, 2010);
        // 최신 연도가 먼저 오는지 확인
        assertThat(result.get(0)).isGreaterThanOrEqualTo(result.get(result.size() - 1));
    }

    @Test
    @DisplayName("findDistinctArtistsWithCount - 아티스트별 곡 수 조회")
    void findDistinctArtistsWithCount() {
        List<Object[]> result = songRepository.findDistinctArtistsWithCount();

        assertThat(result).isNotEmpty();
        // BTS는 2곡이 있어야 함
        boolean foundBTS = result.stream()
            .anyMatch(row -> "BTS".equals(row[0]) && ((Number) row[1]).intValue() == 2);
        assertThat(foundBTS).isTrue();
    }

    @Test
    @DisplayName("findByUseYnAndHasAudioSource - YouTube 또는 파일이 있는 곡만 조회")
    void findByUseYnAndHasAudioSource() {
        List<Song> result = songRepository.findByUseYnAndHasAudioSource("Y");

        assertThat(result).hasSize(4);
        assertThat(result).allMatch(s -> s.getYoutubeVideoId() != null || s.getFilePath() != null);
    }
}
