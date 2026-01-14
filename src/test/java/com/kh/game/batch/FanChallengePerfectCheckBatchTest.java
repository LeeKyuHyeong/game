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
}
