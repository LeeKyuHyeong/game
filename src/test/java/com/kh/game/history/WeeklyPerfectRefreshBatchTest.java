package com.kh.game.history;

import com.kh.game.entity.*;
import com.kh.game.repository.*;
import com.kh.game.service.*;
import com.kh.game.batch.WeeklyPerfectRefreshBatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD: 주간 퍼펙트 갱신 배치 테스트
 *
 * 정책:
 * - 1주일마다 실행
 * - 곡 수 변경 시 무효화 (추가/삭제 모두)
 * - 달성시점 + 현재시점 둘 다 표시
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("주간 퍼펙트 갱신 배치 테스트")
class WeeklyPerfectRefreshBatchTest {

    @Autowired
    private WeeklyPerfectRefreshBatch weeklyPerfectRefreshBatch;

    @Autowired
    private FanChallengeRecordRepository fanChallengeRecordRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private SongHistoryRepository songHistoryRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SongService songService;

    private Member testMember;
    private Genre kpopGenre;
    private int songCounter = 0;

    @BeforeEach
    void setUp() {
        fanChallengeRecordRepository.deleteAll();
        songHistoryRepository.deleteAll();
        songRepository.deleteAll();

        testMember = memberRepository.findByEmail("weekly-batch-test@test.com").orElseGet(() -> {
            Member m = new Member();
            m.setEmail("weekly-batch-test@test.com");
            m.setPassword(passwordEncoder.encode("1234"));
            m.setNickname("주간배치테스터");
            m.setUsername("weeklybatchtester");
            m.setRole(Member.MemberRole.USER);
            m.setStatus(Member.MemberStatus.ACTIVE);
            return memberRepository.save(m);
        });

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
        return songService.addSongWithHistory(song);
    }

    private FanChallengeRecord createPerfectRecord(String artist, int totalSongs, LocalDateTime achievedAt) {
        FanChallengeRecord record = new FanChallengeRecord(testMember, artist, totalSongs, FanChallengeDifficulty.HARDCORE);
        record.setCorrectCount(totalSongs);
        record.setIsPerfectClear(true);
        record.setIsCurrentPerfect(true);  // 신규 필드: 현재 시점 퍼펙트 여부
        record.setBestTimeMs(30000L);
        record.setAchievedAt(achievedAt);
        record.setLastCheckedAt(achievedAt);
        return fanChallengeRecordRepository.save(record);
    }

    // =====================================================
    // 4. 주간 배치 갱신
    // =====================================================
    @Nested
    @DisplayName("4. 주간 배치 갱신")
    class WeeklyBatchTests {

        @Test
        @DisplayName("4.1 곡 추가 시 현재시점 퍼펙트 무효화")
        void weeklyBatch_addedSong_shouldInvalidateCurrentPerfect() {
            // Given: BTS 5곡, 퍼펙트 달성
            for (int i = 1; i <= 5; i++) {
                createSong("BTS Song " + i, "BTS");
            }
            FanChallengeRecord record = createPerfectRecord("BTS", 5, LocalDateTime.now().minusDays(7));

            // 신곡 1개 추가
            createSong("BTS Song 6", "BTS");

            // When
            WeeklyPerfectRefreshBatch.BatchResult result = weeklyPerfectRefreshBatch.execute();

            // Then
            FanChallengeRecord updated = fanChallengeRecordRepository.findById(record.getId()).orElseThrow();
            assertThat(updated.getIsPerfectClear()).isTrue();          // 달성시점 퍼펙트: 유지
            assertThat(updated.getIsCurrentPerfect()).isFalse();       // 현재시점 퍼펙트: 무효화
            assertThat(result.getInvalidatedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("4.2 곡 삭제 시 현재시점 퍼펙트 무효화")
        void weeklyBatch_deletedSong_shouldInvalidateCurrentPerfect() {
            // Given: BTS 5곡, 퍼펙트 달성
            Song song1 = createSong("BTS Song 1", "BTS");
            for (int i = 2; i <= 5; i++) {
                createSong("BTS Song " + i, "BTS");
            }
            FanChallengeRecord record = createPerfectRecord("BTS", 5, LocalDateTime.now().minusDays(7));

            // 1곡 삭제
            songService.deleteSongWithHistory(song1.getId());

            // When
            WeeklyPerfectRefreshBatch.BatchResult result = weeklyPerfectRefreshBatch.execute();

            // Then: 곡 수 변경 시 무효화 (정책)
            FanChallengeRecord updated = fanChallengeRecordRepository.findById(record.getId()).orElseThrow();
            assertThat(updated.getIsPerfectClear()).isTrue();          // 달성시점 퍼펙트: 유지
            assertThat(updated.getIsCurrentPerfect()).isFalse();       // 현재시점 퍼펙트: 무효화
        }

        @Test
        @DisplayName("4.3 곡 수 변경 없으면 현재시점 퍼펙트 유지")
        void weeklyBatch_noChange_shouldKeepCurrentPerfect() {
            // Given: BTS 5곡, 퍼펙트 달성
            for (int i = 1; i <= 5; i++) {
                createSong("BTS Song " + i, "BTS");
            }
            FanChallengeRecord record = createPerfectRecord("BTS", 5, LocalDateTime.now().minusDays(7));

            // When: 변경 없이 배치 실행
            WeeklyPerfectRefreshBatch.BatchResult result = weeklyPerfectRefreshBatch.execute();

            // Then
            FanChallengeRecord updated = fanChallengeRecordRepository.findById(record.getId()).orElseThrow();
            assertThat(updated.getIsPerfectClear()).isTrue();
            assertThat(updated.getIsCurrentPerfect()).isTrue();
            assertThat(result.getInvalidatedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("4.4 전곡 삭제 시 달성시점 퍼펙트도 무효화")
        void weeklyBatch_allDeleted_shouldInvalidateBoth() {
            // Given: BTS 3곡, 퍼펙트 달성
            Song song1 = createSong("BTS Song 1", "BTS");
            Song song2 = createSong("BTS Song 2", "BTS");
            Song song3 = createSong("BTS Song 3", "BTS");
            FanChallengeRecord record = createPerfectRecord("BTS", 3, LocalDateTime.now().minusDays(7));

            // 전곡 삭제
            songService.deleteSongWithHistory(song1.getId());
            songService.deleteSongWithHistory(song2.getId());
            songService.deleteSongWithHistory(song3.getId());

            // When
            weeklyPerfectRefreshBatch.execute();

            // Then: 아티스트가 없어지면 둘 다 무효화
            FanChallengeRecord updated = fanChallengeRecordRepository.findById(record.getId()).orElseThrow();
            assertThat(updated.getIsPerfectClear()).isFalse();
            assertThat(updated.getIsCurrentPerfect()).isFalse();
        }

        @Test
        @DisplayName("4.5 배치 실행 시 lastCheckedAt 갱신")
        void weeklyBatch_shouldUpdateLastCheckedAt() {
            // Given
            for (int i = 1; i <= 5; i++) {
                createSong("BTS Song " + i, "BTS");
            }
            LocalDateTime oldCheckedAt = LocalDateTime.now().minusDays(7);
            FanChallengeRecord record = createPerfectRecord("BTS", 5, oldCheckedAt);

            // When
            weeklyPerfectRefreshBatch.execute();

            // Then
            FanChallengeRecord updated = fanChallengeRecordRepository.findById(record.getId()).orElseThrow();
            assertThat(updated.getLastCheckedAt()).isAfter(oldCheckedAt);
        }

        @Test
        @DisplayName("4.6 여러 아티스트 동시 처리")
        void weeklyBatch_multipleArtists_shouldProcessAll() {
            // Given: BTS 5곡 퍼펙트 + IU 3곡 퍼펙트
            for (int i = 1; i <= 5; i++) {
                createSong("BTS Song " + i, "BTS");
            }
            for (int i = 1; i <= 3; i++) {
                createSong("IU Song " + i, "IU");
            }
            createPerfectRecord("BTS", 5, LocalDateTime.now().minusDays(7));
            createPerfectRecord("IU", 3, LocalDateTime.now().minusDays(7));

            // BTS에만 신곡 추가
            createSong("BTS Song 6", "BTS");

            // When
            WeeklyPerfectRefreshBatch.BatchResult result = weeklyPerfectRefreshBatch.execute();

            // Then: BTS만 무효화, IU는 유지
            assertThat(result.getInvalidatedCount()).isEqualTo(1);
            assertThat(result.getProcessedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("4.7 삭제 후 복구 시 (곡 수 동일) 현재시점 퍼펙트 유지")
        void weeklyBatch_deleteAndRestore_shouldKeepPerfect() {
            // Given
            Song song1 = createSong("BTS Song 1", "BTS");
            for (int i = 2; i <= 5; i++) {
                createSong("BTS Song " + i, "BTS");
            }
            FanChallengeRecord record = createPerfectRecord("BTS", 5, LocalDateTime.now().minusDays(7));

            // 삭제 후 복구 (결과적으로 5곡 유지)
            songService.deleteSongWithHistory(song1.getId());
            songService.restoreSongWithHistory(song1.getId());

            // When
            weeklyPerfectRefreshBatch.execute();

            // Then: 곡 수 동일하므로 유지
            FanChallengeRecord updated = fanChallengeRecordRepository.findById(record.getId()).orElseThrow();
            assertThat(updated.getIsCurrentPerfect()).isTrue();
        }
    }

    // =====================================================
    // 5. 달성시점 vs 현재시점 비교
    // =====================================================
    @Nested
    @DisplayName("5. 달성시점 vs 현재시점 비교")
    class AchievedVsCurrentTests {

        @Test
        @DisplayName("5.1 달성시점 기록은 영구 보존")
        void achievedRecord_shouldBePreserved() {
            // Given: 1달 전 5곡 퍼펙트
            for (int i = 1; i <= 5; i++) {
                createSong("BTS Song " + i, "BTS");
            }
            LocalDateTime achievedAt = LocalDateTime.now().minusMonths(1);
            FanChallengeRecord record = createPerfectRecord("BTS", 5, achievedAt);

            // 곡 3개 추가 (현재 8곡)
            createSong("BTS Song 6", "BTS");
            createSong("BTS Song 7", "BTS");
            createSong("BTS Song 8", "BTS");

            // When
            weeklyPerfectRefreshBatch.execute();

            // Then
            FanChallengeRecord updated = fanChallengeRecordRepository.findById(record.getId()).orElseThrow();
            assertThat(updated.getIsPerfectClear()).isTrue();                    // 달성시점: 5/5 = 100%
            assertThat(updated.getTotalSongs()).isEqualTo(5);                    // 달성 당시 곡 수
            assertThat(updated.getCorrectCount()).isEqualTo(5);                  // 맞춘 곡 수
            assertThat(updated.getAchievedAt()).isEqualTo(achievedAt);           // 달성 시점
        }

        @Test
        @DisplayName("5.2 현재시점 통계 별도 계산 가능")
        void currentStats_shouldBeCalculatedSeparately() {
            // Given: 5곡 퍼펙트 후 3곡 추가
            for (int i = 1; i <= 5; i++) {
                createSong("BTS Song " + i, "BTS");
            }
            FanChallengeRecord record = createPerfectRecord("BTS", 5, LocalDateTime.now().minusDays(7));

            createSong("BTS Song 6", "BTS");
            createSong("BTS Song 7", "BTS");
            createSong("BTS Song 8", "BTS");

            // When: 현재 곡 수 조회
            int currentSongCount = songService.countActiveSongsByArtist("BTS");

            // Then
            assertThat(record.getTotalSongs()).isEqualTo(5);          // 달성 당시
            assertThat(currentSongCount).isEqualTo(8);                // 현재

            // 현재 기준 클리어율: 5/8 = 62.5%
            double currentClearRate = (double) record.getCorrectCount() / currentSongCount * 100;
            assertThat(currentClearRate).isCloseTo(62.5, org.assertj.core.data.Offset.offset(0.1));
        }
    }
}
