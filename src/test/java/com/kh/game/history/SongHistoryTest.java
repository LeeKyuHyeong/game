package com.kh.game.history;

import com.kh.game.entity.*;
import com.kh.game.repository.*;
import com.kh.game.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD: 이력 기반 곡 관리 테스트
 *
 * 정책:
 * - 퍼펙트: 달성시점 + 현재시점 둘 다 표시
 * - 배치: 곡 수 변경 시 무효화 (추가/삭제 모두)
 * - 이력 보관: 1년
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("이력 기반 곡 관리 테스트")
class SongHistoryTest {

    @Autowired
    private SongService songService;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private SongHistoryRepository songHistoryRepository;

    @Autowired
    private GenreRepository genreRepository;

    private Genre kpopGenre;
    private int songCounter = 0;

    @BeforeEach
    void setUp() {
        songHistoryRepository.deleteAll();
        songRepository.deleteAll();

        kpopGenre = genreRepository.findByCode("KPOP").orElseGet(() -> {
            Genre g = new Genre();
            g.setCode("KPOP");
            g.setName("K-POP");
            g.setUseYn("Y");
            return genreRepository.save(g);
        });
    }

    private Song createSongEntity(String title, String artist) {
        Song song = new Song();
        song.setTitle(title);
        song.setArtist(artist);
        song.setGenre(kpopGenre);
        song.setUseYn("Y");
        song.setIsSolo(false);
        song.setYoutubeVideoId("vid_" + (++songCounter));
        song.setStartTime(0);
        song.setPlayDuration(30);
        return song;
    }

    // =====================================================
    // 1. 이력 테이블 기본 동작
    // =====================================================
    @Nested
    @DisplayName("1. 이력 테이블 기본 동작")
    class BasicHistoryTests {

        @Test
        @DisplayName("1.1 곡 추가 시 ADDED 이력 생성")
        void addSong_shouldCreateHistory() {
            // When
            Song song = songService.addSongWithHistory(createSongEntity("Test Song", "BTS"));

            // Then
            List<SongHistory> histories = songHistoryRepository.findBySongIdOrderByActionAtDesc(song.getId());
            assertThat(histories).hasSize(1);
            assertThat(histories.get(0).getAction()).isEqualTo(SongHistory.Action.ADDED);
            assertThat(histories.get(0).getArtist()).isEqualTo("BTS");
            assertThat(histories.get(0).getActionAt()).isNotNull();
        }

        @Test
        @DisplayName("1.2 곡 삭제 시 DELETED 이력 생성")
        void deleteSong_shouldCreateHistory() {
            // Given
            Song song = songService.addSongWithHistory(createSongEntity("Test Song", "BTS"));

            // When
            songService.deleteSongWithHistory(song.getId());

            // Then
            List<SongHistory> histories = songHistoryRepository.findBySongIdOrderByActionAtDesc(song.getId());
            assertThat(histories).hasSize(2);
            assertThat(histories.get(0).getAction()).isEqualTo(SongHistory.Action.DELETED);
        }

        @Test
        @DisplayName("1.3 곡 복구 시 RESTORED 이력 생성")
        void restoreSong_shouldCreateHistory() {
            // Given
            Song song = songService.addSongWithHistory(createSongEntity("Test Song", "BTS"));
            songService.deleteSongWithHistory(song.getId());

            // When
            songService.restoreSongWithHistory(song.getId());

            // Then
            List<SongHistory> histories = songHistoryRepository.findBySongIdOrderByActionAtDesc(song.getId());
            assertThat(histories).hasSize(3);
            assertThat(histories.get(0).getAction()).isEqualTo(SongHistory.Action.RESTORED);
        }

        @Test
        @DisplayName("1.4 여러 변경 시 모두 추적")
        void multipleActions_shouldTrackAll() {
            // Given
            Song song = songService.addSongWithHistory(createSongEntity("Test Song", "BTS"));

            // When: 삭제 → 복구 → 삭제 → 복구
            songService.deleteSongWithHistory(song.getId());
            songService.restoreSongWithHistory(song.getId());
            songService.deleteSongWithHistory(song.getId());
            songService.restoreSongWithHistory(song.getId());

            // Then
            List<SongHistory> histories = songHistoryRepository.findBySongIdOrderByActionAtDesc(song.getId());
            assertThat(histories).hasSize(5);
            assertThat(histories).extracting(SongHistory::getAction)
                    .containsExactly(
                            SongHistory.Action.RESTORED,
                            SongHistory.Action.DELETED,
                            SongHistory.Action.RESTORED,
                            SongHistory.Action.DELETED,
                            SongHistory.Action.ADDED
                    );
        }
    }

    // =====================================================
    // 2. 시점 기준 곡 수 조회 (BETWEEN 쿼리)
    // =====================================================
    @Nested
    @DisplayName("2. 시점 기준 곡 수 조회")
    class PointInTimeQueryTests {

        @Test
        @DisplayName("2.1 과거 시점 곡 수 조회")
        void countSongsAt_pastDate_shouldReturnHistoricalCount() {
            // Given: 시점별 곡 추가
            LocalDateTime t1 = LocalDateTime.now().minusDays(10);
            LocalDateTime t2 = LocalDateTime.now().minusDays(5);
            LocalDateTime t3 = LocalDateTime.now();

            // t1에 2곡 추가 (시뮬레이션)
            Song song1 = songService.addSongWithHistoryAt(createSongEntity("Song 1", "BTS"), t1);
            Song song2 = songService.addSongWithHistoryAt(createSongEntity("Song 2", "BTS"), t1);
            // t2에 1곡 추가
            Song song3 = songService.addSongWithHistoryAt(createSongEntity("Song 3", "BTS"), t2);

            // When & Then
            assertThat(songService.countSongsAtTime("BTS", t1.plusHours(1))).isEqualTo(2);
            assertThat(songService.countSongsAtTime("BTS", t2.plusHours(1))).isEqualTo(3);
            assertThat(songService.countSongsAtTime("BTS", t3)).isEqualTo(3);
        }

        @Test
        @DisplayName("2.2 곡 추가 전 시점 조회 → 0")
        void countSongsAt_beforeAnyAdded_shouldReturnZero() {
            // Given
            LocalDateTime beforeAdd = LocalDateTime.now().minusDays(10);
            songService.addSongWithHistory(createSongEntity("Song 1", "BTS"));

            // When
            int count = songService.countSongsAtTime("BTS", beforeAdd);

            // Then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("2.3 삭제 후 시점 조회 → 삭제된 곡 제외")
        void countSongsAt_afterDelete_shouldExcludeDeleted() {
            // Given
            Song song1 = songService.addSongWithHistory(createSongEntity("Song 1", "BTS"));
            Song song2 = songService.addSongWithHistory(createSongEntity("Song 2", "BTS"));

            LocalDateTime beforeDelete = LocalDateTime.now();

            // 약간의 시간 차이를 두고 삭제
            songService.deleteSongWithHistory(song1.getId());

            LocalDateTime afterDelete = LocalDateTime.now().plusSeconds(1);

            // When & Then
            assertThat(songService.countSongsAtTime("BTS", beforeDelete)).isEqualTo(2);
            assertThat(songService.countSongsAtTime("BTS", afterDelete)).isEqualTo(1);
        }

        @Test
        @DisplayName("2.4 삭제~복구 사이 시점 조회 → 해당 곡 제외")
        void countSongsAt_betweenDeleteAndRestore_shouldExclude() {
            // Given: 명시적인 시간 지정으로 테스트
            LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 12, 0);
            LocalDateTime addTime = baseTime;
            LocalDateTime deleteTime = baseTime.plusHours(1);
            LocalDateTime betweenTime = baseTime.plusHours(2);
            LocalDateTime restoreTime = baseTime.plusHours(3);
            LocalDateTime afterRestoreTime = baseTime.plusHours(4);

            Song song1 = songService.addSongWithHistoryAt(createSongEntity("Song 1", "BTS"), addTime);
            songService.addSongWithHistoryAt(createSongEntity("Song 2", "BTS"), addTime);

            songService.deleteSongWithHistoryAt(song1.getId(), deleteTime);
            songService.restoreSongWithHistoryAt(song1.getId(), restoreTime);

            // When & Then
            assertThat(songService.countSongsAtTime("BTS", addTime)).isEqualTo(2);
            assertThat(songService.countSongsAtTime("BTS", betweenTime)).isEqualTo(1);  // 삭제~복구 사이
            assertThat(songService.countSongsAtTime("BTS", afterRestoreTime)).isEqualTo(2);
        }

        @Test
        @DisplayName("2.5 정확히 경계 시점 처리 (inclusive/exclusive)")
        void countSongsAt_exactBoundary_shouldHandleCorrectly() {
            // Given: 정확한 시점에 추가
            LocalDateTime exactTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
            songService.addSongWithHistoryAt(createSongEntity("Song 1", "BTS"), exactTime);

            // When & Then: 경계 시점 포함 여부 (<= 정책)
            assertThat(songService.countSongsAtTime("BTS", exactTime)).isEqualTo(1);
            assertThat(songService.countSongsAtTime("BTS", exactTime.plusSeconds(1))).isEqualTo(1);  // 이후도 포함
            assertThat(songService.countSongsAtTime("BTS", exactTime.minusSeconds(1))).isEqualTo(0); // 이전은 제외
        }
    }

    // =====================================================
    // 3. 이력 정리 (1년 보관)
    // =====================================================
    @Nested
    @DisplayName("3. 이력 정리 (1년 보관)")
    class HistoryCleanupTests {

        @Test
        @DisplayName("3.1 1년 이상 된 이력 삭제")
        void cleanup_shouldDeleteOldHistory() {
            // Given: 2년 전 이력
            LocalDateTime twoYearsAgo = LocalDateTime.now().minusYears(2);
            Song song = songService.addSongWithHistoryAt(createSongEntity("Old Song", "BTS"), twoYearsAgo);

            // When
            int deletedCount = songService.cleanupOldHistory();

            // Then
            assertThat(deletedCount).isGreaterThan(0);
            List<SongHistory> remaining = songHistoryRepository.findBySongIdOrderByActionAtDesc(song.getId());
            assertThat(remaining).isEmpty();
        }

        @Test
        @DisplayName("3.2 1년 미만 이력 유지")
        void cleanup_shouldKeepRecentHistory() {
            // Given: 6개월 전 이력
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
            Song song = songService.addSongWithHistoryAt(createSongEntity("Recent Song", "BTS"), sixMonthsAgo);

            // When
            songService.cleanupOldHistory();

            // Then
            List<SongHistory> remaining = songHistoryRepository.findBySongIdOrderByActionAtDesc(song.getId());
            assertThat(remaining).hasSize(1);
        }

        @Test
        @DisplayName("3.3 이력 삭제 후에도 곡 데이터 유지")
        void cleanup_shouldNotAffectSongData() {
            // Given
            LocalDateTime twoYearsAgo = LocalDateTime.now().minusYears(2);
            Song song = songService.addSongWithHistoryAt(createSongEntity("Old Song", "BTS"), twoYearsAgo);
            Long songId = song.getId();

            // When
            songService.cleanupOldHistory();

            // Then: 곡 데이터는 유지
            assertThat(songRepository.findById(songId)).isPresent();
        }
    }
}
