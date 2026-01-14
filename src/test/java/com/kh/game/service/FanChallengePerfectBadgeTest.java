package com.kh.game.service;

import com.kh.game.entity.*;
import com.kh.game.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("팬챌린지 퍼펙트 뱃지 테스트")
class FanChallengePerfectBadgeTest {

    @Autowired
    private BadgeService badgeService;

    @Autowired
    private FanChallengeRecordRepository fanChallengeRecordRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private MemberBadgeRepository memberBadgeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member testMember;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 정리
        memberBadgeRepository.deleteAll();
        fanChallengeRecordRepository.deleteAll();

        // 테스트 회원 생성
        testMember = new Member();
        testMember.setEmail("fan-badge-test@test.com");
        testMember.setPassword(passwordEncoder.encode("1234"));
        testMember.setNickname("뱃지테스터");
        testMember.setUsername("badgetester");
        testMember.setRole(Member.MemberRole.USER);
        testMember.setStatus(Member.MemberStatus.ACTIVE);
        testMember = memberRepository.save(testMember);
    }

    // ========== Step 1: 뱃지 초기 데이터 테스트 ==========

    @Test
    @DisplayName("팬챌린지 퍼펙트 뱃지 6개가 존재해야 함")
    void fanChallengePerfectBadges_shouldExist() {
        // Given & When
        List<String> badgeCodes = List.of(
            "FAN_FIRST_PERFECT",
            "FAN_PERFECT_5",
            "FAN_PERFECT_10",
            "FAN_HARDCORE_FIRST",
            "FAN_HARDCORE_5",
            "FAN_HARDCORE_10"
        );

        // Then
        for (String code : badgeCodes) {
            Badge badge = badgeRepository.findByCode(code).orElse(null);
            assertThat(badge)
                .as("뱃지 '%s'가 존재해야 함", code)
                .isNotNull();
            assertThat(badge.getIsActive()).isTrue();
        }
    }

    @Test
    @DisplayName("FAN_FIRST_PERFECT 뱃지는 RARE 희귀도여야 함")
    void fanFirstPerfect_shouldBeRare() {
        Badge badge = badgeRepository.findByCode("FAN_FIRST_PERFECT").orElse(null);
        assertThat(badge).isNotNull();
        assertThat(badge.getRarity()).isEqualTo(Badge.BadgeRarity.RARE);
        assertThat(badge.getCategory()).isEqualTo(Badge.BadgeCategory.SPECIAL);
    }

    @Test
    @DisplayName("하드코어 뱃지는 EPIC 또는 LEGENDARY 희귀도여야 함")
    void hardcoreBadges_shouldBeEpicOrLegendary() {
        Badge hardcoreFirst = badgeRepository.findByCode("FAN_HARDCORE_FIRST").orElse(null);
        Badge hardcore5 = badgeRepository.findByCode("FAN_HARDCORE_5").orElse(null);
        Badge hardcore10 = badgeRepository.findByCode("FAN_HARDCORE_10").orElse(null);

        assertThat(hardcoreFirst).isNotNull();
        assertThat(hardcoreFirst.getRarity()).isEqualTo(Badge.BadgeRarity.EPIC);

        assertThat(hardcore5).isNotNull();
        assertThat(hardcore5.getRarity()).isEqualTo(Badge.BadgeRarity.LEGENDARY);

        assertThat(hardcore10).isNotNull();
        assertThat(hardcore10.getRarity()).isEqualTo(Badge.BadgeRarity.LEGENDARY);
    }

    // ========== Step 2: Repository 쿼리 테스트 ==========

    @Test
    @DisplayName("퍼펙트 아티스트 수 카운트 - 중복 아티스트는 1회만")
    void countDistinctPerfectArtists_noDuplicates() {
        // Given: BTS 퍼펙트 클리어 (NORMAL, HARDCORE 둘 다)
        createPerfectRecord(testMember, "BTS", FanChallengeDifficulty.NORMAL);
        createPerfectRecord(testMember, "BTS", FanChallengeDifficulty.HARDCORE);
        createPerfectRecord(testMember, "aespa", FanChallengeDifficulty.NORMAL);

        // When
        long count = fanChallengeRecordRepository.countDistinctPerfectArtistsByMember(testMember);

        // Then: BTS(2 records) + aespa(1 record) = 2 distinct artists
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("하드코어 퍼펙트 아티스트 수 카운트")
    void countDistinctHardcorePerfectArtists() {
        // Given
        createPerfectRecord(testMember, "BTS", FanChallengeDifficulty.HARDCORE);
        createPerfectRecord(testMember, "aespa", FanChallengeDifficulty.NORMAL);
        createPerfectRecord(testMember, "IU", FanChallengeDifficulty.HARDCORE);

        // When
        long hardcoreCount = fanChallengeRecordRepository.countDistinctPerfectArtistsByMemberAndDifficulty(
            testMember, FanChallengeDifficulty.HARDCORE);

        // Then
        assertThat(hardcoreCount).isEqualTo(2); // BTS, IU
    }

    @Test
    @DisplayName("퍼펙트 클리어가 아닌 기록은 카운트에서 제외")
    void countDistinctPerfectArtists_excludeNonPerfect() {
        // Given
        createPerfectRecord(testMember, "BTS", FanChallengeDifficulty.HARDCORE);
        createNonPerfectRecord(testMember, "aespa", FanChallengeDifficulty.HARDCORE);

        // When
        long count = fanChallengeRecordRepository.countDistinctPerfectArtistsByMember(testMember);

        // Then
        assertThat(count).isEqualTo(1); // BTS만
    }

    // ========== Step 3: BadgeService 메서드 테스트 ==========

    @Test
    @DisplayName("첫 퍼펙트 달성 시 FAN_FIRST_PERFECT 뱃지 획득")
    void checkBadgesAfterFanChallengePerfect_firstPerfect() {
        // Given
        createPerfectRecord(testMember, "BTS", FanChallengeDifficulty.NORMAL);

        // When
        List<Badge> newBadges = badgeService.checkBadgesAfterFanChallengePerfect(
            testMember, FanChallengeDifficulty.NORMAL);

        // Then
        assertThat(newBadges).extracting(Badge::getCode).contains("FAN_FIRST_PERFECT");
    }

    @Test
    @DisplayName("5개 아티스트 퍼펙트 달성 시 FAN_PERFECT_5 뱃지 획득")
    void checkBadgesAfterFanChallengePerfect_5artists() {
        // Given: 5개 아티스트 퍼펙트
        String[] artists = {"BTS", "aespa", "IU", "NewJeans", "BLACKPINK"};
        for (String artist : artists) {
            createPerfectRecord(testMember, artist, FanChallengeDifficulty.NORMAL);
        }

        // When
        List<Badge> newBadges = badgeService.checkBadgesAfterFanChallengePerfect(
            testMember, FanChallengeDifficulty.NORMAL);

        // Then
        assertThat(newBadges).extracting(Badge::getCode).contains("FAN_PERFECT_5");
    }

    @Test
    @DisplayName("첫 하드코어 퍼펙트 달성 시 FAN_HARDCORE_FIRST 뱃지 획득")
    void checkBadgesAfterFanChallengePerfect_firstHardcore() {
        // Given
        createPerfectRecord(testMember, "BTS", FanChallengeDifficulty.HARDCORE);

        // When
        List<Badge> newBadges = badgeService.checkBadgesAfterFanChallengePerfect(
            testMember, FanChallengeDifficulty.HARDCORE);

        // Then
        assertThat(newBadges).extracting(Badge::getCode).contains("FAN_HARDCORE_FIRST");
    }

    @Test
    @DisplayName("NORMAL 난이도에서는 하드코어 뱃지 미획득")
    void checkBadgesAfterFanChallengePerfect_normalDoesNotAwardHardcore() {
        // Given
        createPerfectRecord(testMember, "BTS", FanChallengeDifficulty.NORMAL);

        // When
        List<Badge> newBadges = badgeService.checkBadgesAfterFanChallengePerfect(
            testMember, FanChallengeDifficulty.NORMAL);

        // Then
        assertThat(newBadges).extracting(Badge::getCode)
            .doesNotContain("FAN_HARDCORE_FIRST", "FAN_HARDCORE_5", "FAN_HARDCORE_10");
    }

    @Test
    @DisplayName("이미 획득한 뱃지는 중복 지급되지 않음")
    void checkBadgesAfterFanChallengePerfect_noDuplicateAward() {
        // Given: 첫 퍼펙트로 뱃지 획득
        createPerfectRecord(testMember, "BTS", FanChallengeDifficulty.NORMAL);
        badgeService.checkBadgesAfterFanChallengePerfect(testMember, FanChallengeDifficulty.NORMAL);

        // When: 두 번째 퍼펙트 달성
        createPerfectRecord(testMember, "aespa", FanChallengeDifficulty.NORMAL);
        List<Badge> newBadges = badgeService.checkBadgesAfterFanChallengePerfect(
            testMember, FanChallengeDifficulty.NORMAL);

        // Then: FAN_FIRST_PERFECT는 이미 있으므로 다시 지급되지 않음
        assertThat(newBadges).extracting(Badge::getCode).doesNotContain("FAN_FIRST_PERFECT");
    }

    // ========== 헬퍼 메서드 ==========

    private FanChallengeRecord createPerfectRecord(Member member, String artist, FanChallengeDifficulty difficulty) {
        FanChallengeRecord record = new FanChallengeRecord(member, artist, 10, difficulty);
        record.setCorrectCount(10);
        record.setIsPerfectClear(true);
        record.setBestTimeMs(30000L);
        record.setAchievedAt(LocalDateTime.now());
        return fanChallengeRecordRepository.save(record);
    }

    private FanChallengeRecord createNonPerfectRecord(Member member, String artist, FanChallengeDifficulty difficulty) {
        FanChallengeRecord record = new FanChallengeRecord(member, artist, 10, difficulty);
        record.setCorrectCount(8);
        record.setIsPerfectClear(false);
        record.setAchievedAt(LocalDateTime.now());
        return fanChallengeRecordRepository.save(record);
    }
}
