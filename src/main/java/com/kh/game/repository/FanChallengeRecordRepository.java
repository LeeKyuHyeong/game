package com.kh.game.repository;

import com.kh.game.entity.FanChallengeDifficulty;
import com.kh.game.entity.FanChallengeRecord;
import com.kh.game.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FanChallengeRecordRepository extends JpaRepository<FanChallengeRecord, Long> {

    // 기존 호환성 유지 (하드코어 기본)
    Optional<FanChallengeRecord> findByMemberAndArtist(Member member, String artist);

    // 난이도별 회원 기록 조회
    Optional<FanChallengeRecord> findByMemberAndArtistAndDifficulty(Member member, String artist, FanChallengeDifficulty difficulty);

    // 아티스트별 퍼펙트 클리어 랭킹 (시간순) - 하드코어만
    @Query("SELECT r FROM FanChallengeRecord r WHERE r.artist = :artist AND r.isPerfectClear = true AND r.difficulty = 'HARDCORE' ORDER BY r.bestTimeMs ASC")
    List<FanChallengeRecord> findPerfectClearsByArtist(@Param("artist") String artist, Pageable pageable);

    // 아티스트별 전체 랭킹 (정답 수 > 시간순) - 하드코어만 (공식 랭킹)
    // COALESCE: 시간 미기록(NULL)은 뒤로 밀림
    @Query("SELECT r FROM FanChallengeRecord r WHERE r.artist = :artist AND r.difficulty = 'HARDCORE' ORDER BY r.correctCount DESC, COALESCE(r.bestTimeMs, 999999999) ASC")
    List<FanChallengeRecord> findTopByArtist(@Param("artist") String artist, Pageable pageable);

    // 난이도별 아티스트 랭킹
    @Query("SELECT r FROM FanChallengeRecord r WHERE r.artist = :artist AND r.difficulty = :difficulty ORDER BY r.correctCount DESC, COALESCE(r.bestTimeMs, 999999999) ASC")
    List<FanChallengeRecord> findTopByArtistAndDifficulty(@Param("artist") String artist, @Param("difficulty") FanChallengeDifficulty difficulty, Pageable pageable);

    // 회원의 모든 기록 조회
    List<FanChallengeRecord> findByMemberOrderByAchievedAtDesc(Member member);

    // 회원의 특정 아티스트 퍼펙트 클리어 뱃지 조회 (전 난이도)
    @Query("SELECT r FROM FanChallengeRecord r WHERE r.member = :member AND r.artist = :artist AND r.isPerfectClear = true ORDER BY r.difficulty DESC")
    List<FanChallengeRecord> findPerfectBadgesByMemberAndArtist(@Param("member") Member member, @Param("artist") String artist);

    // 회원의 퍼펙트 클리어 수 (전체 난이도)
    @Query("SELECT COUNT(r) FROM FanChallengeRecord r WHERE r.member = :member AND r.isPerfectClear = true")
    long countPerfectClearsByMember(@Param("member") Member member);

    // 회원의 하드코어 퍼펙트 클리어 수
    @Query("SELECT COUNT(r) FROM FanChallengeRecord r WHERE r.member = :member AND r.isPerfectClear = true AND r.difficulty = 'HARDCORE'")
    long countHardcorePerfectClearsByMember(@Param("member") Member member);

    // 아티스트별 도전자 수 (하드코어만)
    @Query("SELECT COUNT(r) FROM FanChallengeRecord r WHERE r.artist = :artist AND r.difficulty = 'HARDCORE'")
    long countByArtist(@Param("artist") String artist);

    // 기록이 있는 아티스트 목록 (기록 수 기준 인기순, 하드코어만)
    @Query("SELECT r.artist, COUNT(r) as cnt FROM FanChallengeRecord r WHERE r.difficulty = 'HARDCORE' GROUP BY r.artist ORDER BY cnt DESC")
    List<Object[]> findPopularArtists(Pageable pageable);

    // 회원의 퍼펙트 클리어 고유 아티스트 수 (전체 난이도)
    @Query("SELECT COUNT(DISTINCT r.artist) FROM FanChallengeRecord r WHERE r.member = :member AND r.isPerfectClear = true")
    long countDistinctPerfectArtistsByMember(@Param("member") Member member);

    // 회원의 특정 난이도 퍼펙트 클리어 고유 아티스트 수
    @Query("SELECT COUNT(DISTINCT r.artist) FROM FanChallengeRecord r WHERE r.member = :member AND r.isPerfectClear = true AND r.difficulty = :difficulty")
    long countDistinctPerfectArtistsByMemberAndDifficulty(@Param("member") Member member, @Param("difficulty") FanChallengeDifficulty difficulty);

    // 모든 퍼펙트 기록 조회 (배치용)
    @Query("SELECT r FROM FanChallengeRecord r WHERE r.isPerfectClear = true")
    List<FanChallengeRecord> findAllPerfectRecords();

    // ========== 관리자 페이지용 쿼리 ==========

    // 전체 기록 조회 (페이징, Member JOIN)
    @Query("SELECT r FROM FanChallengeRecord r JOIN FETCH r.member")
    Page<FanChallengeRecord> findAllWithMember(Pageable pageable);

    // 아티스트 검색 필터 (페이징)
    @Query("SELECT r FROM FanChallengeRecord r JOIN FETCH r.member WHERE r.artist LIKE %:keyword%")
    Page<FanChallengeRecord> findByArtistContaining(@Param("keyword") String keyword, Pageable pageable);

    // 난이도 필터 (페이징)
    @Query("SELECT r FROM FanChallengeRecord r JOIN FETCH r.member WHERE r.difficulty = :difficulty")
    Page<FanChallengeRecord> findByDifficulty(@Param("difficulty") FanChallengeDifficulty difficulty, Pageable pageable);

    // 퍼펙트 클리어만 필터 (페이징)
    @Query("SELECT r FROM FanChallengeRecord r JOIN FETCH r.member WHERE r.isPerfectClear = :isPerfectClear")
    Page<FanChallengeRecord> findByIsPerfectClear(@Param("isPerfectClear") Boolean isPerfectClear, Pageable pageable);

    // 회원별 기록 조회 (페이징)
    @Query("SELECT r FROM FanChallengeRecord r WHERE r.member = :member ORDER BY r.achievedAt DESC")
    Page<FanChallengeRecord> findByMember(@Param("member") Member member, Pageable pageable);

    // 회원 ID로 기록 조회 (페이징)
    @Query("SELECT r FROM FanChallengeRecord r WHERE r.member.id = :memberId ORDER BY r.achievedAt DESC")
    Page<FanChallengeRecord> findByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    // 복합 필터 (아티스트 + 난이도)
    @Query("SELECT r FROM FanChallengeRecord r JOIN FETCH r.member WHERE r.artist LIKE %:keyword% AND r.difficulty = :difficulty")
    Page<FanChallengeRecord> findByArtistContainingAndDifficulty(@Param("keyword") String keyword, @Param("difficulty") FanChallengeDifficulty difficulty, Pageable pageable);

    // 복합 필터 (아티스트 + 퍼펙트)
    @Query("SELECT r FROM FanChallengeRecord r JOIN FETCH r.member WHERE r.artist LIKE %:keyword% AND r.isPerfectClear = :isPerfectClear")
    Page<FanChallengeRecord> findByArtistContainingAndIsPerfectClear(@Param("keyword") String keyword, @Param("isPerfectClear") Boolean isPerfectClear, Pageable pageable);

    // 복합 필터 (난이도 + 퍼펙트)
    @Query("SELECT r FROM FanChallengeRecord r JOIN FETCH r.member WHERE r.difficulty = :difficulty AND r.isPerfectClear = :isPerfectClear")
    Page<FanChallengeRecord> findByDifficultyAndIsPerfectClear(@Param("difficulty") FanChallengeDifficulty difficulty, @Param("isPerfectClear") Boolean isPerfectClear, Pageable pageable);

    // 복합 필터 (아티스트 + 난이도 + 퍼펙트)
    @Query("SELECT r FROM FanChallengeRecord r JOIN FETCH r.member WHERE r.artist LIKE %:keyword% AND r.difficulty = :difficulty AND r.isPerfectClear = :isPerfectClear")
    Page<FanChallengeRecord> findByArtistContainingAndDifficultyAndIsPerfectClear(@Param("keyword") String keyword, @Param("difficulty") FanChallengeDifficulty difficulty, @Param("isPerfectClear") Boolean isPerfectClear, Pageable pageable);

    // 아티스트별 통계 집계 (도전 수, 퍼펙트 수)
    @Query("SELECT r.artist, COUNT(r), SUM(CASE WHEN r.isPerfectClear = true THEN 1 ELSE 0 END) " +
           "FROM FanChallengeRecord r GROUP BY r.artist ORDER BY COUNT(r) DESC")
    List<Object[]> getArtistStatistics();

    // 오늘 도전 기록 수
    @Query("SELECT COUNT(r) FROM FanChallengeRecord r WHERE DATE(r.achievedAt) = CURRENT_DATE")
    long countTodayRecords();

    // 고유 아티스트 수
    @Query("SELECT COUNT(DISTINCT r.artist) FROM FanChallengeRecord r")
    long countDistinctArtists();

    // 전체 퍼펙트 클리어 수
    @Query("SELECT COUNT(r) FROM FanChallengeRecord r WHERE r.isPerfectClear = true")
    long countPerfectClears();
}
