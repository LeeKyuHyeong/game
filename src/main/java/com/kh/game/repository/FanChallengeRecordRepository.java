package com.kh.game.repository;

import com.kh.game.entity.FanChallengeDifficulty;
import com.kh.game.entity.FanChallengeRecord;
import com.kh.game.entity.Member;
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
    @Query("SELECT r FROM FanChallengeRecord r WHERE r.artist = :artist AND r.difficulty = 'HARDCORE' ORDER BY r.correctCount DESC, r.bestTimeMs ASC")
    List<FanChallengeRecord> findTopByArtist(@Param("artist") String artist, Pageable pageable);

    // 난이도별 아티스트 랭킹
    @Query("SELECT r FROM FanChallengeRecord r WHERE r.artist = :artist AND r.difficulty = :difficulty ORDER BY r.correctCount DESC, r.bestTimeMs ASC")
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
}
