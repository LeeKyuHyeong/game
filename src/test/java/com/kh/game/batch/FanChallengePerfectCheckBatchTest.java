package com.kh.game.batch;

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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("FanChallengePerfectCheckBatch 테스트")
class FanChallengePerfectCheckBatchTest {

    @Autowired
    private FanChallengePerfectCheckBatch fanChallengePerfectCheckBatch;

    @Autowired
    private FanChallengeRecordRepository fanChallengeRecordRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member testMember;
    private Genre kpopGenre;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        fanChallengeRecordRepository.deleteAll();
        songRepository.deleteAll();

        // 테스트 회원 생성
        testMember = new Member();
        testMember.setEmail("batch-test@test.com");
        testMember.setPassword(passwordEncoder.encode("1234"));
        testMember.setNickname("배치테스터");
        testMember.setUsername("batchtester");
        testMember.setRole(Member.MemberRole.USER);
        testMember.setStatus(Member.MemberStatus.ACTIVE);
        testMember = memberRepository.save(testMember);

        // 장르 생성
        kpopGenre = genreRepository.findByCode("KPOP").orElseGet(() -> {
            Genre g = new Genre();
            g.setCode("KPOP");
            g.setName("K-POP");
            g.setUseYn("Y");
            return genreRepository.save(g);
        });
    }

    @Test
    @DisplayName("곡이 추가되면 퍼펙트가 무효화되어야 함")
    void execute_shouldInvalidatePerfectWhenSongAdded() {
        // Given: BTS 곡 5개
        for (int i = 1; i <= 5; i++) {
            createSong("BTS Song " + i, "BTS");
        }

        // 퍼펙트 기록 생성 (5곡 모두 맞춤)
        FanChallengeRecord record = new FanChallengeRecord(testMember, "BTS", 5, FanChallengeDifficulty.HARDCORE);
        record.setCorrectCount(5);
        record.setIsPerfectClear(true);
        record.setBestTimeMs(30000L);
        record.setAchievedAt(LocalDateTime.now());
        record = fanChallengeRecordRepository.save(record);

        // 새 곡 추가 (총 6곡)
        createSong("BTS Song 6", "BTS");

        // When
        int invalidatedCount = fanChallengePerfectCheckBatch.execute(BatchExecutionHistory.ExecutionType.MANUAL);

        // Then
        assertThat(invalidatedCount).isEqualTo(1);
        FanChallengeRecord updated = fanChallengeRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(updated.getIsPerfectClear()).isFalse();
    }

    @Test
    @DisplayName("곡 수가 같으면 퍼펙트가 유지되어야 함")
    void execute_shouldKeepPerfectWhenSameCount() {
        // Given: BTS 곡 5개
        for (int i = 1; i <= 5; i++) {
            createSong("BTS Song " + i, "BTS");
        }

        // 퍼펙트 기록 생성
        FanChallengeRecord record = new FanChallengeRecord(testMember, "BTS", 5, FanChallengeDifficulty.HARDCORE);
        record.setCorrectCount(5);
        record.setIsPerfectClear(true);
        record.setBestTimeMs(30000L);
        record.setAchievedAt(LocalDateTime.now());
        record = fanChallengeRecordRepository.save(record);

        // When: 곡 추가 없이 배치 실행
        int invalidatedCount = fanChallengePerfectCheckBatch.execute(BatchExecutionHistory.ExecutionType.MANUAL);

        // Then
        assertThat(invalidatedCount).isEqualTo(0);
        FanChallengeRecord updated = fanChallengeRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(updated.getIsPerfectClear()).isTrue();
    }

    @Test
    @DisplayName("아티스트의 모든 곡이 삭제되면 퍼펙트가 무효화되어야 함")
    void execute_shouldInvalidateWhenAllSongsDeleted() {
        // Given: 다른 아티스트 곡만 존재
        createSong("IU Song 1", "IU");

        // BTS 퍼펙트 기록 (하지만 BTS 곡은 없음)
        FanChallengeRecord record = new FanChallengeRecord(testMember, "BTS", 5, FanChallengeDifficulty.HARDCORE);
        record.setCorrectCount(5);
        record.setIsPerfectClear(true);
        record.setBestTimeMs(30000L);
        record.setAchievedAt(LocalDateTime.now());
        record = fanChallengeRecordRepository.save(record);

        // When
        int invalidatedCount = fanChallengePerfectCheckBatch.execute(BatchExecutionHistory.ExecutionType.MANUAL);

        // Then
        assertThat(invalidatedCount).isEqualTo(1);
        FanChallengeRecord updated = fanChallengeRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(updated.getIsPerfectClear()).isFalse();
    }

    private int songCounter = 0;

    private Song createSong(String title, String artist) {
        Song song = new Song();
        song.setTitle(title);
        song.setArtist(artist);
        song.setGenre(kpopGenre);
        song.setUseYn("Y");
        song.setIsSolo(false);
        song.setYoutubeVideoId("vid_" + (++songCounter));  // 짧은 ID (VARCHAR(20) 제한)
        return songRepository.save(song);
    }

    // =====================================================
    // Soft Delete 정책 테스트
    // 정책: 삭제 시 퍼펙트 유지, 추가 시만 무효화
    // =====================================================

    @Test
    @DisplayName("Soft Delete 시 퍼펙트가 유지되어야 함 (삭제는 관대)")
    void execute_softDelete_shouldKeepPerfect() {
        // Given: BTS 곡 5개
        Song song1 = createSong("BTS Song 1", "BTS");
        Song song2 = createSong("BTS Song 2", "BTS");
        createSong("BTS Song 3", "BTS");
        createSong("BTS Song 4", "BTS");
        createSong("BTS Song 5", "BTS");

        // 퍼펙트 기록 생성 (5곡 모두 맞춤)
        FanChallengeRecord record = new FanChallengeRecord(testMember, "BTS", 5, FanChallengeDifficulty.HARDCORE);
        record.setCorrectCount(5);
        record.setIsPerfectClear(true);
        record.setBestTimeMs(30000L);
        record.setAchievedAt(LocalDateTime.now());
        record = fanChallengeRecordRepository.save(record);

        // Soft delete 2곡 (총 3곡 남음)
        song1.setUseYn("N");
        song2.setUseYn("N");
        songRepository.save(song1);
        songRepository.save(song2);

        // When
        int invalidatedCount = fanChallengePerfectCheckBatch.execute(BatchExecutionHistory.ExecutionType.MANUAL);

        // Then: 삭제는 퍼펙트에 영향 없음
        assertThat(invalidatedCount).isEqualTo(0);
        FanChallengeRecord updated = fanChallengeRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(updated.getIsPerfectClear()).isTrue();
    }

    @Test
    @DisplayName("Soft Delete 후 곡 추가 시 퍼펙트가 무효화되어야 함")
    void execute_softDeleteThenAdd_shouldInvalidatePerfect() {
        // Given: BTS 곡 5개
        Song song1 = createSong("BTS Song 1", "BTS");
        createSong("BTS Song 2", "BTS");
        createSong("BTS Song 3", "BTS");
        createSong("BTS Song 4", "BTS");
        createSong("BTS Song 5", "BTS");

        // 퍼펙트 기록 생성 (5곡 모두 맞춤)
        FanChallengeRecord record = new FanChallengeRecord(testMember, "BTS", 5, FanChallengeDifficulty.HARDCORE);
        record.setCorrectCount(5);
        record.setIsPerfectClear(true);
        record.setBestTimeMs(30000L);
        record.setAchievedAt(LocalDateTime.now());
        record = fanChallengeRecordRepository.save(record);

        // Soft delete 1곡 (4곡)
        song1.setUseYn("N");
        songRepository.save(song1);

        // 새 곡 2개 추가 (총 6곡)
        createSong("BTS Song 6", "BTS");
        createSong("BTS Song 7", "BTS");

        // When
        int invalidatedCount = fanChallengePerfectCheckBatch.execute(BatchExecutionHistory.ExecutionType.MANUAL);

        // Then: 곡이 추가되어 퍼펙트 무효화 (현재 6곡 > 기록 5곡)
        assertThat(invalidatedCount).isEqualTo(1);
        FanChallengeRecord updated = fanChallengeRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(updated.getIsPerfectClear()).isFalse();
    }

    @Test
    @DisplayName("전곡 Soft Delete 시 퍼펙트가 무효화되어야 함")
    void execute_allSoftDeleted_shouldInvalidatePerfect() {
        // Given: BTS 곡 3개
        Song song1 = createSong("BTS Song 1", "BTS");
        Song song2 = createSong("BTS Song 2", "BTS");
        Song song3 = createSong("BTS Song 3", "BTS");

        // 퍼펙트 기록 생성
        FanChallengeRecord record = new FanChallengeRecord(testMember, "BTS", 3, FanChallengeDifficulty.HARDCORE);
        record.setCorrectCount(3);
        record.setIsPerfectClear(true);
        record.setBestTimeMs(20000L);
        record.setAchievedAt(LocalDateTime.now());
        record = fanChallengeRecordRepository.save(record);

        // When: 전곡 soft delete
        song1.setUseYn("N");
        song2.setUseYn("N");
        song3.setUseYn("N");
        songRepository.save(song1);
        songRepository.save(song2);
        songRepository.save(song3);

        int invalidatedCount = fanChallengePerfectCheckBatch.execute(BatchExecutionHistory.ExecutionType.MANUAL);

        // Then: 아티스트가 없어짐 → 퍼펙트 무효화
        assertThat(invalidatedCount).isEqualTo(1);
        FanChallengeRecord updated = fanChallengeRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(updated.getIsPerfectClear()).isFalse();
    }

    @Test
    @DisplayName("Soft Delete로 곡 수가 같아지면 퍼펙트 유지")
    void execute_softDeleteToSameCount_shouldKeepPerfect() {
        // Given: BTS 곡 7개 (기록은 5곡에서 달성)
        createSong("BTS Song 1", "BTS");
        createSong("BTS Song 2", "BTS");
        createSong("BTS Song 3", "BTS");
        createSong("BTS Song 4", "BTS");
        createSong("BTS Song 5", "BTS");
        Song song6 = createSong("BTS Song 6", "BTS");
        Song song7 = createSong("BTS Song 7", "BTS");

        // 퍼펙트 기록 (5곡 기준)
        FanChallengeRecord record = new FanChallengeRecord(testMember, "BTS", 5, FanChallengeDifficulty.HARDCORE);
        record.setCorrectCount(5);
        record.setIsPerfectClear(true);
        record.setBestTimeMs(30000L);
        record.setAchievedAt(LocalDateTime.now());
        record = fanChallengeRecordRepository.save(record);

        // 새로 추가된 2곡 soft delete (다시 5곡)
        song6.setUseYn("N");
        song7.setUseYn("N");
        songRepository.save(song6);
        songRepository.save(song7);

        // When
        int invalidatedCount = fanChallengePerfectCheckBatch.execute(BatchExecutionHistory.ExecutionType.MANUAL);

        // Then: 곡 수가 같으므로 퍼펙트 유지
        assertThat(invalidatedCount).isEqualTo(0);
        FanChallengeRecord updated = fanChallengeRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(updated.getIsPerfectClear()).isTrue();
    }

    @Test
    @DisplayName("다중 난이도 기록에서 Soft Delete 처리")
    void execute_multiDifficulty_softDelete() {
        // Given: BTS 곡 5개
        createSong("BTS Song 1", "BTS");
        createSong("BTS Song 2", "BTS");
        createSong("BTS Song 3", "BTS");
        createSong("BTS Song 4", "BTS");
        createSong("BTS Song 5", "BTS");

        // BEGINNER 퍼펙트
        FanChallengeRecord beginnerRecord = new FanChallengeRecord(testMember, "BTS", 5, FanChallengeDifficulty.BEGINNER);
        beginnerRecord.setCorrectCount(5);
        beginnerRecord.setIsPerfectClear(true);
        beginnerRecord.setBestTimeMs(40000L);
        beginnerRecord.setAchievedAt(LocalDateTime.now());
        fanChallengeRecordRepository.save(beginnerRecord);

        // HARDCORE 퍼펙트
        FanChallengeRecord hardcoreRecord = new FanChallengeRecord(testMember, "BTS", 5, FanChallengeDifficulty.HARDCORE);
        hardcoreRecord.setCorrectCount(5);
        hardcoreRecord.setIsPerfectClear(true);
        hardcoreRecord.setBestTimeMs(30000L);
        hardcoreRecord.setAchievedAt(LocalDateTime.now());
        fanChallengeRecordRepository.save(hardcoreRecord);

        // 신곡 1개 추가 (총 6곡)
        createSong("BTS Song 6", "BTS");

        // When
        int invalidatedCount = fanChallengePerfectCheckBatch.execute(BatchExecutionHistory.ExecutionType.MANUAL);

        // Then: 두 난이도 모두 무효화
        assertThat(invalidatedCount).isEqualTo(2);
    }
}
