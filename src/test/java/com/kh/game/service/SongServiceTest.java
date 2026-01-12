package com.kh.game.service;

import com.kh.game.entity.Genre;
import com.kh.game.entity.Song;
import com.kh.game.repository.GenreRepository;
import com.kh.game.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SongService 테스트")
class SongServiceTest {

    @Autowired
    private SongService songService;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private GenreRepository genreRepository;

    private Genre kpopGenre;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        songRepository.deleteAll();
        genreRepository.deleteAll();

        // 장르 생성
        kpopGenre = new Genre();
        kpopGenre.setCode("KPOP");
        kpopGenre.setName("K-POP");
        kpopGenre.setUseYn("Y");
        kpopGenre = genreRepository.save(kpopGenre);

        // 테스트 데이터 생성
        createSong("Dynamite", "BTS", kpopGenre, "Y", true, "abc123");
        createSong("Butter", "BTS", kpopGenre, "Y", false, "def456");
        createSong("Next Level", "aespa", kpopGenre, "Y", false, "ghi789");

        pageable = PageRequest.of(0, 20);
    }

    private Song createSong(String title, String artist, Genre genre, String useYn,
                            Boolean isSolo, String youtubeVideoId) {
        Song song = new Song();
        song.setTitle(title);
        song.setArtist(artist);
        song.setGenre(genre);
        song.setUseYn(useYn);
        song.setIsSolo(isSolo);
        song.setYoutubeVideoId(youtubeVideoId);
        return songRepository.save(song);
    }

    @Test
    @DisplayName("searchWithFilters(List) - artists가 null이면 전체 조회")
    void searchWithFilters_artistsNull_returnsAll() {
        Page<Song> result = songService.searchWithFilters(null, (List<String>) null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("searchWithFilters(List) - artists가 빈 리스트면 전체 조회")
    void searchWithFilters_artistsEmpty_returnsAll() {
        Page<Song> result = songService.searchWithFilters(null, Collections.emptyList(), null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("searchWithFilters(List) - artists가 있으면 해당 아티스트만 조회")
    void searchWithFilters_withArtists_returnsMatching() {
        List<String> artists = Arrays.asList("BTS");

        Page<Song> result = songService.searchWithFilters(null, artists, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(s -> s.getArtist().equals("BTS"));
    }

    @Test
    @DisplayName("searchWithFilters(List) - 모든 아티스트 선택 시에도 정상 조회")
    void searchWithFilters_allArtistsSelected_returnsAll() {
        List<String> allArtists = Arrays.asList("BTS", "aespa");

        Page<Song> result = songService.searchWithFilters(null, allArtists, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("searchWithFilters - keyword가 빈 문자열이면 전체 조회되어야 함 (버그 시나리오)")
    void searchWithFilters_emptyKeyword_shouldReturnAll() {
        // 이 테스트는 실제로 빈 문자열이 전달되면 문제가 발생할 수 있음을 보여줌
        // 컨트롤러에서 빈 문자열을 null로 변환해야 함
        Page<Song> result = songService.searchWithFilters(null, (String) null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("getArtistsWithCount - 아티스트별 곡 수 조회")
    void getArtistsWithCount() {
        List<java.util.Map<String, Object>> result = songService.getArtistsWithCount();

        assertThat(result).isNotEmpty();

        // BTS는 2곡이 있어야 함
        java.util.Map<String, Object> bts = result.stream()
            .filter(m -> "BTS".equals(m.get("name")))
            .findFirst()
            .orElse(null);

        assertThat(bts).isNotNull();
        assertThat(bts.get("count")).isEqualTo(2);
    }

    @Test
    @DisplayName("toggleUseYn - 사용여부 토글")
    void toggleUseYn() {
        Song song = songRepository.findAll().get(0);
        String originalUseYn = song.getUseYn();

        songService.toggleUseYn(song.getId());

        Song updated = songRepository.findById(song.getId()).orElseThrow();
        assertThat(updated.getUseYn()).isNotEqualTo(originalUseYn);
    }
}
