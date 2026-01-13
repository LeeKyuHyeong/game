package com.kh.game.service;

import com.kh.game.entity.Genre;
import com.kh.game.entity.Song;
import com.kh.game.repository.GenreRepository;
import com.kh.game.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("GenreMigrationService 테스트 - 장르 체계 재설계 마이그레이션")
class GenreMigrationServiceTest {

    @Autowired
    private GenreMigrationService genreMigrationService;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private SongRepository songRepository;

    // 새 장르 체계 정의 (방안 C)
    private static final Map<String, String> NEW_GENRE_SYSTEM = Map.of(
        "IDOL", "아이돌",
        "BALLAD", "발라드",
        "HIPHOP", "힙합/랩",
        "RNB", "R&B/소울",
        "INDIE", "인디/어쿠스틱",
        "TROT", "트로트",
        "BAND", "밴드/록",
        "OST", "OST",
        "EDM", "EDM/댄스",
        "RETRO", "레트로/가요"
    );

    // 기존 genre_id → 새 code 매핑
    private static final Map<Long, String> MIGRATION_MAPPING = Map.of(
        2L, "IDOL",      // K-POP 아이돌 → 아이돌
        5L, "BALLAD",    // K-POP 발라드 → 발라드
        6L, "INDIE",     // K-POP 인디 → 인디/어쿠스틱
        13L, "HIPHOP",   // 힙합 → 힙합/랩
        7L, "TROT",      // 트로트 → 트로트
        29L, "OST",      // OST → OST
        31L, "BAND"      // 밴드 → 밴드/록
    );

    private Genre oldIdolGenre;
    private Genre oldBalladGenre;
    private Genre oldIndieGenre;
    private Genre oldHiphopGenre;

    @BeforeEach
    void setUp() {
        songRepository.deleteAll();
        genreRepository.deleteAll();

        // 기존 장르 체계 생성 (마이그레이션 전 상태)
        oldIdolGenre = createOldGenre(2L, "KPOP_IDOL", "K-POP 아이돌");
        oldBalladGenre = createOldGenre(5L, "KPOP_BALLAD", "K-POP 발라드");
        oldIndieGenre = createOldGenre(6L, "KPOP_INDIE", "K-POP 인디");
        oldHiphopGenre = createOldGenre(13L, "HIPHOP", "힙합");

        // 테스트 곡 데이터 생성
        createSong("Kill This Love", "블랙핑크", oldIdolGenre);
        createSong("How You Like That", "블랙핑크", oldIdolGenre);
        createSong("시간을 달려서", "여자친구", oldIdolGenre);

        createSong("Fine", "태연", oldBalladGenre);
        createSong("두 사람", "성시경", oldBalladGenre);
        createSong("부디", "VOS", oldBalladGenre);

        createSong("단발머리", "악동뮤지션", oldIndieGenre);
        createSong("후리지아", "악동뮤지션", oldIndieGenre);

        createSong("붕붕", "김하온", oldHiphopGenre);
    }

    private Genre createOldGenre(Long id, String code, String name) {
        Genre genre = new Genre();
        genre.setCode(code);
        genre.setName(name);
        genre.setUseYn("Y");
        genre.setDisplayOrder(id.intValue());
        return genreRepository.save(genre);
    }

    private Song createSong(String title, String artist, Genre genre) {
        Song song = new Song();
        song.setTitle(title);
        song.setArtist(artist);
        song.setGenre(genre);
        song.setUseYn("Y");
        song.setYoutubeVideoId("test" + System.nanoTime());
        return songRepository.save(song);
    }

    @Nested
    @DisplayName("1. 새 장르 체계 생성 테스트")
    class NewGenreSystemTest {

        @Test
        @DisplayName("새 장르 10개가 정확히 생성되어야 한다")
        void shouldCreate10NewGenres() {
            // when
            genreMigrationService.migrateToNewGenreSystem();

            // then
            List<Genre> activeGenres = genreRepository.findByUseYnOrderByDisplayOrderAsc("Y");
            assertThat(activeGenres).hasSize(10);
        }

        @Test
        @DisplayName("각 새 장르의 code와 name이 정확해야 한다")
        void shouldHaveCorrectCodeAndName() {
            // when
            genreMigrationService.migrateToNewGenreSystem();

            // then
            for (Map.Entry<String, String> entry : NEW_GENRE_SYSTEM.entrySet()) {
                Genre genre = genreRepository.findByCode(entry.getKey()).orElse(null);
                assertThat(genre).isNotNull();
                assertThat(genre.getName()).isEqualTo(entry.getValue());
                assertThat(genre.getUseYn()).isEqualTo("Y");
            }
        }

        @Test
        @DisplayName("새 장르의 displayOrder가 1부터 순차적으로 설정되어야 한다")
        void shouldHaveSequentialDisplayOrder() {
            // when
            genreMigrationService.migrateToNewGenreSystem();

            // then
            List<Genre> genres = genreRepository.findByUseYnOrderByDisplayOrderAsc("Y");
            for (int i = 0; i < genres.size(); i++) {
                assertThat(genres.get(i).getDisplayOrder()).isEqualTo(i + 1);
            }
        }
    }

    @Nested
    @DisplayName("2. 곡 마이그레이션 매핑 테스트")
    class SongMigrationTest {

        @Test
        @DisplayName("K-POP 아이돌 곡이 IDOL 장르로 매핑되어야 한다")
        void shouldMapIdolSongsCorrectly() {
            // when
            genreMigrationService.migrateToNewGenreSystem();

            // then
            List<Song> songs = songRepository.findAll();
            Genre idolGenre = genreRepository.findByCode("IDOL").orElseThrow();

            long idolCount = songs.stream()
                .filter(s -> s.getArtist().equals("블랙핑크") || s.getArtist().equals("여자친구"))
                .filter(s -> s.getGenre().getId().equals(idolGenre.getId()))
                .count();

            assertThat(idolCount).isEqualTo(3);
        }

        @Test
        @DisplayName("K-POP 발라드 곡이 BALLAD 장르로 매핑되어야 한다")
        void shouldMapBalladSongsCorrectly() {
            // when
            genreMigrationService.migrateToNewGenreSystem();

            // then
            List<Song> songs = songRepository.findAll();
            Genre balladGenre = genreRepository.findByCode("BALLAD").orElseThrow();

            long balladCount = songs.stream()
                .filter(s -> s.getArtist().equals("태연") ||
                           s.getArtist().equals("성시경") ||
                           s.getArtist().equals("VOS"))
                .filter(s -> s.getGenre().getId().equals(balladGenre.getId()))
                .count();

            assertThat(balladCount).isEqualTo(3);
        }

        @Test
        @DisplayName("K-POP 인디 곡이 INDIE 장르로 매핑되어야 한다")
        void shouldMapIndieSongsCorrectly() {
            // when
            genreMigrationService.migrateToNewGenreSystem();

            // then
            List<Song> songs = songRepository.findAll();
            Genre indieGenre = genreRepository.findByCode("INDIE").orElseThrow();

            long indieCount = songs.stream()
                .filter(s -> s.getArtist().equals("악동뮤지션"))
                .filter(s -> s.getGenre().getId().equals(indieGenre.getId()))
                .count();

            assertThat(indieCount).isEqualTo(2);
        }

        @Test
        @DisplayName("힙합 곡이 HIPHOP 장르로 매핑되어야 한다")
        void shouldMapHiphopSongsCorrectly() {
            // when
            genreMigrationService.migrateToNewGenreSystem();

            // then
            List<Song> songs = songRepository.findAll();
            Genre hiphopGenre = genreRepository.findByCode("HIPHOP").orElseThrow();

            long hiphopCount = songs.stream()
                .filter(s -> s.getArtist().equals("김하온"))
                .filter(s -> s.getGenre().getId().equals(hiphopGenre.getId()))
                .count();

            assertThat(hiphopCount).isEqualTo(1);
        }

        @Test
        @DisplayName("모든 곡이 마이그레이션 후에도 유효한 장르를 참조해야 한다")
        void allSongsShouldHaveValidGenre() {
            // when
            genreMigrationService.migrateToNewGenreSystem();

            // then
            List<Song> songs = songRepository.findAll();
            for (Song song : songs) {
                assertThat(song.getGenre()).isNotNull();
                assertThat(song.getGenre().getUseYn()).isEqualTo("Y");
            }
        }
    }

    @Nested
    @DisplayName("3. 기존 장르 비활성화 테스트")
    class OldGenreDeactivationTest {

        @Test
        @DisplayName("기존 K-POP 접두어 장르가 비활성화되어야 한다")
        void shouldDeactivateOldKpopGenres() {
            // when
            genreMigrationService.migrateToNewGenreSystem();

            // then
            // 기존 장르 코드가 존재한다면 use_yn='N'이어야 함
            genreRepository.findByCode("KPOP_IDOL").ifPresent(g ->
                assertThat(g.getUseYn()).isEqualTo("N"));
            genreRepository.findByCode("KPOP_BALLAD").ifPresent(g ->
                assertThat(g.getUseYn()).isEqualTo("N"));
            genreRepository.findByCode("KPOP_INDIE").ifPresent(g ->
                assertThat(g.getUseYn()).isEqualTo("N"));
        }

        @Test
        @DisplayName("활성화된 장르는 새 장르 체계만 있어야 한다")
        void onlyNewGenresShouldBeActive() {
            // when
            genreMigrationService.migrateToNewGenreSystem();

            // then
            List<Genre> activeGenres = genreRepository.findByUseYnOrderByDisplayOrderAsc("Y");

            assertThat(activeGenres).hasSize(10);
            assertThat(activeGenres)
                .extracting(Genre::getCode)
                .containsExactlyInAnyOrder(
                    "IDOL", "BALLAD", "HIPHOP", "RNB", "INDIE",
                    "TROT", "BAND", "OST", "EDM", "RETRO"
                );
        }
    }

    @Nested
    @DisplayName("4. 데이터 무결성 테스트")
    class DataIntegrityTest {

        @Test
        @DisplayName("마이그레이션 전후 곡 수가 동일해야 한다")
        void songCountShouldRemainSame() {
            // given
            long beforeCount = songRepository.count();

            // when
            genreMigrationService.migrateToNewGenreSystem();

            // then
            long afterCount = songRepository.count();
            assertThat(afterCount).isEqualTo(beforeCount);
        }

        @Test
        @DisplayName("마이그레이션 후 orphan 곡이 없어야 한다 (장르가 null인 곡)")
        void noOrphanSongsAfterMigration() {
            // when
            genreMigrationService.migrateToNewGenreSystem();

            // then
            List<Song> songs = songRepository.findAll();
            long orphanCount = songs.stream()
                .filter(s -> s.getGenre() == null)
                .count();

            assertThat(orphanCount).isZero();
        }

        @Test
        @DisplayName("곡의 제목, 아티스트 등 다른 필드는 변경되지 않아야 한다")
        void otherFieldsShouldNotChange() {
            // given
            Song beforeSong = songRepository.findAll().get(0);
            String originalTitle = beforeSong.getTitle();
            String originalArtist = beforeSong.getArtist();
            Long songId = beforeSong.getId();

            // when
            genreMigrationService.migrateToNewGenreSystem();

            // then
            Song afterSong = songRepository.findById(songId).orElseThrow();
            assertThat(afterSong.getTitle()).isEqualTo(originalTitle);
            assertThat(afterSong.getArtist()).isEqualTo(originalArtist);
        }
    }

    @Nested
    @DisplayName("5. 마이그레이션 매핑 정보 조회 테스트")
    class MigrationMappingTest {

        @Test
        @DisplayName("마이그레이션 매핑 정보를 조회할 수 있어야 한다")
        void shouldProvideMigrationMapping() {
            // when
            Map<String, String> mapping = genreMigrationService.getMigrationMapping();

            // then
            assertThat(mapping).isNotEmpty();
            assertThat(mapping).containsEntry("KPOP_IDOL", "IDOL");
            assertThat(mapping).containsEntry("KPOP_BALLAD", "BALLAD");
            assertThat(mapping).containsEntry("KPOP_INDIE", "INDIE");
        }

        @Test
        @DisplayName("새 장르 목록을 조회할 수 있어야 한다")
        void shouldProvideNewGenreList() {
            // when
            List<Map<String, String>> newGenres = genreMigrationService.getNewGenreDefinitions();

            // then
            assertThat(newGenres).hasSize(10);
        }
    }
}
