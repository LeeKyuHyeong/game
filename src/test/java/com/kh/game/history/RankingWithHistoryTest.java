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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD: 이력 기반 랭킹 표시 테스트
 *
 * 정책:
 * - 달성시점 + 현재시점 둘 다 표시
 * - 현재시점 클리어율 max 100%
 * - 달성시점은 항상 100% (퍼펙트인 경우)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("이력 기반 랭킹 표시 테스트")
class RankingWithHistoryTest {

    @Autowired
    private FanChallengeService fanChallengeService;

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

    private Member member1;
    private Member member2;
    private Member member3;
    private Genre kpopGenre;
    private int songCounter = 0;

    @BeforeEach
    void setUp() {
        fanChallengeRecordRepository.deleteAll();
        songHistoryRepository.deleteAll();
        songRepository.deleteAll();

        member1 = getOrCreateMember("ranking-test1@test.com", "랭킹테스터1", "rankingtester1");
        member2 = getOrCreateMember("ranking-test2@test.com", "랭킹테스터2", "rankingtester2");
        member3 = getOrCreateMember("ranking-test3@test.com", "랭킹테스터3", "rankingtester3");

        kpopGenre = genreRepository.findByCode("KPOP").orElseGet(() -> {
            Genre g = new Genre();
            g.setCode("KPOP");
            g.setName("K-POP");
            g.setUseYn("Y");
            return genreRepository.save(g);
        });
    }

    private Member getOrCreateMember(String email, String nickname, String username) {
        return memberRepository.findByEmail(email).orElseGet(() -> {
            Member m = new Member();
            m.setEmail(email);
            m.setPassword(passwordEncoder.encode("1234"));
            m.setNickname(nickname);
            m.setUsername(username);
            m.setRole(Member.MemberRole.USER);
            m.setStatus(Member.MemberStatus.ACTIVE);
            return memberRepository.save(m);
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

    private FanChallengeRecord createRecord(Member member, String artist, int totalSongs, int correctCount,
                                            boolean isPerfect, long bestTimeMs, LocalDateTime achievedAt) {
        FanChallengeRecord record = new FanChallengeRecord(member, artist, totalSongs, FanChallengeDifficulty.HARDCORE);
        record.setCorrectCount(correctCount);
        record.setIsPerfectClear(isPerfect);
        record.setIsCurrentPerfect(isPerfect);
        record.setBestTimeMs(bestTimeMs);
        record.setAchievedAt(achievedAt);
        return fanChallengeRecordRepository.save(record);
    }

    // =====================================================
    // 6. 랭킹 표시 (달성시점 + 현재시점)
    // =====================================================
    @Nested
    @DisplayName("6. 랭킹 표시")
    class RankingDisplayTests {

        @Test
        @DisplayName("6.1 달성시점과 현재시점 둘 다 표시")
        void ranking_shouldShowBothAchievedAndCurrent() {
            // Given: 50곡에서 퍼펙트, 현재 55곡
            for (int i = 1; i <= 50; i++) {
                createSong("BTS Song " + i, "BTS");
            }
            LocalDateTime achievedAt = LocalDateTime.now().minusDays(30);
            FanChallengeRecord record = createRecord(member1, "BTS", 50, 50, true, 120000L, achievedAt);

            // 5곡 추가
            for (int i = 51; i <= 55; i++) {
                createSong("BTS Song " + i, "BTS");
            }

            // When: 랭킹 데이터 조회
            Map<String, Object> rankingData = fanChallengeService.getRankingDataWithHistory(record);

            // Then
            assertThat(rankingData.get("achievedClearRate")).isEqualTo(100.0);    // 달성시점: 50/50
            assertThat((Double) rankingData.get("currentClearRate")).isCloseTo(90.9, org.assertj.core.data.Offset.offset(0.1));  // 현재: 50/55
            assertThat(rankingData.get("achievedTotalSongs")).isEqualTo(50);
            assertThat(rankingData.get("currentTotalSongs")).isEqualTo(55);
        }

        @Test
        @DisplayName("6.2 현재시점 클리어율 max 100% (곡 삭제 시)")
        void ranking_currentRate_shouldNotExceed100() {
            // Given: 50곡에서 퍼펙트, 현재 45곡 (5곡 삭제)
            List<Song> songs = new java.util.ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                songs.add(createSong("BTS Song " + i, "BTS"));
            }
            LocalDateTime achievedAt = LocalDateTime.now().minusDays(30);
            FanChallengeRecord record = createRecord(member1, "BTS", 50, 50, true, 120000L, achievedAt);

            // 5곡 삭제
            for (int i = 0; i < 5; i++) {
                songService.deleteSongWithHistory(songs.get(i).getId());
            }

            // When
            Map<String, Object> rankingData = fanChallengeService.getRankingDataWithHistory(record);

            // Then: 50/45 = 111% 이지만 max 100%로 표시
            assertThat((double) rankingData.get("currentClearRate")).isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("6.3 달성시점 퍼펙트는 항상 100%")
        void ranking_achievedRate_alwaysShowsOriginal() {
            // Given
            for (int i = 1; i <= 30; i++) {
                createSong("BTS Song " + i, "BTS");
            }
            FanChallengeRecord record = createRecord(member1, "BTS", 30, 30, true, 90000L, LocalDateTime.now());

            // When
            Map<String, Object> rankingData = fanChallengeService.getRankingDataWithHistory(record);

            // Then
            assertThat(rankingData.get("achievedClearRate")).isEqualTo(100.0);
        }

        @Test
        @DisplayName("6.4 비퍼펙트 기록도 정확히 표시")
        void ranking_nonPerfect_shouldShowAccurate() {
            // Given: 30곡 중 25곡 맞춤
            for (int i = 1; i <= 30; i++) {
                createSong("BTS Song " + i, "BTS");
            }
            FanChallengeRecord record = createRecord(member1, "BTS", 30, 25, false, 90000L, LocalDateTime.now());

            // When
            Map<String, Object> rankingData = fanChallengeService.getRankingDataWithHistory(record);

            // Then
            assertThat((Double) rankingData.get("achievedClearRate")).isCloseTo(83.3, org.assertj.core.data.Offset.offset(0.1)); // 25/30
        }
    }

    // =====================================================
    // 7. 랭킹 정렬
    // =====================================================
    @Nested
    @DisplayName("7. 랭킹 정렬")
    class RankingSortTests {

        @Test
        @DisplayName("7.1 현재시점 클리어율로 정렬")
        void ranking_shouldSortByCurrentClearRate() {
            // Given: BTS 10곡
            for (int i = 1; i <= 10; i++) {
                createSong("BTS Song " + i, "BTS");
            }

            // member1: 10곡에서 퍼펙트 (현재 100%)
            createRecord(member1, "BTS", 10, 10, true, 30000L, LocalDateTime.now().minusDays(1));

            // member2: 8곡에서 퍼펙트 (당시 100%, 현재 80%)
            createRecord(member2, "BTS", 8, 8, true, 25000L, LocalDateTime.now().minusDays(30));

            // member3: 10곡 중 9곡 (현재 90%)
            createRecord(member3, "BTS", 10, 9, false, 28000L, LocalDateTime.now());

            // When
            List<Map<String, Object>> ranking = fanChallengeService.getArtistRankingWithCurrentRate("BTS", 10);

            // Then: 현재 클리어율 순 (100% > 90% > 80%)
            assertThat(ranking.get(0).get("nickname")).isEqualTo("랭킹테스터1");  // 100%
            assertThat(ranking.get(1).get("nickname")).isEqualTo("랭킹테스터3");  // 90%
            assertThat(ranking.get(2).get("nickname")).isEqualTo("랭킹테스터2");  // 80%
        }

        @Test
        @DisplayName("7.2 동률 시 클리어 시간으로 정렬")
        void ranking_tiebreaker_shouldSortByTime() {
            // Given: BTS 10곡
            for (int i = 1; i <= 10; i++) {
                createSong("BTS Song " + i, "BTS");
            }

            // 모두 퍼펙트, 시간만 다름
            createRecord(member1, "BTS", 10, 10, true, 35000L, LocalDateTime.now());  // 35초
            createRecord(member2, "BTS", 10, 10, true, 30000L, LocalDateTime.now());  // 30초
            createRecord(member3, "BTS", 10, 10, true, 32000L, LocalDateTime.now());  // 32초

            // When
            List<Map<String, Object>> ranking = fanChallengeService.getArtistRankingWithCurrentRate("BTS", 10);

            // Then: 시간 순 (30초 < 32초 < 35초)
            assertThat(ranking.get(0).get("nickname")).isEqualTo("랭킹테스터2");  // 30초
            assertThat(ranking.get(1).get("nickname")).isEqualTo("랭킹테스터3");  // 32초
            assertThat(ranking.get(2).get("nickname")).isEqualTo("랭킹테스터1");  // 35초
        }

        @Test
        @DisplayName("7.3 isCurrentPerfect로 퍼펙트 뱃지 표시")
        void ranking_shouldShowCurrentPerfectBadge() {
            // Given: 10곡
            for (int i = 1; i <= 10; i++) {
                createSong("BTS Song " + i, "BTS");
            }

            // member1: 현재도 퍼펙트
            FanChallengeRecord record1 = createRecord(member1, "BTS", 10, 10, true, 30000L, LocalDateTime.now());
            record1.setIsCurrentPerfect(true);
            fanChallengeRecordRepository.save(record1);

            // member2: 달성시점 퍼펙트, 현재는 아님 (곡 추가됨)
            FanChallengeRecord record2 = createRecord(member2, "BTS", 8, 8, true, 25000L, LocalDateTime.now().minusDays(30));
            record2.setIsCurrentPerfect(false);
            fanChallengeRecordRepository.save(record2);

            // When
            List<Map<String, Object>> ranking = fanChallengeService.getArtistRankingWithCurrentRate("BTS", 10);

            // Then
            assertThat(ranking.get(0).get("isCurrentPerfect")).isEqualTo(true);
            assertThat(ranking.get(1).get("isCurrentPerfect")).isEqualTo(false);
        }
    }

    // =====================================================
    // 8. 엣지 케이스
    // =====================================================
    @Nested
    @DisplayName("8. 엣지 케이스")
    class EdgeCaseTests {

        @Test
        @DisplayName("8.1 아티스트 곡이 0개인 경우")
        void ranking_noSongs_shouldHandleGracefully() {
            // Given: 곡 없음, 과거 기록만 있음
            FanChallengeRecord record = createRecord(member1, "BTS", 5, 5, true, 30000L, LocalDateTime.now().minusDays(30));

            // When
            Map<String, Object> rankingData = fanChallengeService.getRankingDataWithHistory(record);

            // Then
            assertThat(rankingData.get("currentTotalSongs")).isEqualTo(0);
            assertThat(rankingData.get("currentClearRate")).isEqualTo(0.0);  // 또는 N/A
            assertThat(rankingData.get("achievedClearRate")).isEqualTo(100.0);  // 달성시점은 유지
        }

        @Test
        @DisplayName("8.2 매우 오래된 기록")
        void ranking_veryOldRecord_shouldStillWork() {
            // Given: 1년 전 기록
            for (int i = 1; i <= 10; i++) {
                createSong("BTS Song " + i, "BTS");
            }
            LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
            FanChallengeRecord record = createRecord(member1, "BTS", 5, 5, true, 30000L, oneYearAgo);

            // 5곡 추가 (현재 10곡)
            // (이미 10곡 생성됨)

            // When
            Map<String, Object> rankingData = fanChallengeService.getRankingDataWithHistory(record);

            // Then
            assertThat(rankingData.get("achievedClearRate")).isEqualTo(100.0);
            assertThat(rankingData.get("currentClearRate")).isEqualTo(50.0);  // 5/10
        }

        @Test
        @DisplayName("8.3 correctCount > currentSongCount (삭제로 인해)")
        void ranking_moreCorrectThanCurrent_shouldCap100() {
            // Given: 10곡 퍼펙트 후 5곡 삭제
            List<Song> songs = new java.util.ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                songs.add(createSong("BTS Song " + i, "BTS"));
            }
            FanChallengeRecord record = createRecord(member1, "BTS", 10, 10, true, 30000L, LocalDateTime.now());

            for (int i = 0; i < 5; i++) {
                songService.deleteSongWithHistory(songs.get(i).getId());
            }

            // When
            Map<String, Object> rankingData = fanChallengeService.getRankingDataWithHistory(record);

            // Then: 10/5 = 200% 이지만 100%로 cap
            assertThat((double) rankingData.get("currentClearRate")).isEqualTo(100.0);
        }
    }
}
