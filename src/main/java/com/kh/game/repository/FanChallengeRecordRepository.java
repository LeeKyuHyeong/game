package com.kh.game.repository;

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

    Optional<FanChallengeRecord> findByMemberAndArtist(Member member, String artist);

    // 아티스트별 퍼펙트 클리어 랭킹 (시간순)
    @Query("SELECT r FROM FanChallengeRecord r WHERE r.artist = :artist AND r.isPerfectClear = true ORDER BY r.bestTimeMs ASC")
    List<FanChallengeRecord> findPerfectClearsByArtist(@Param("artist") String artist, Pageable pageable);

    // 아티스트별 전체 랭킹 (정답 수 > 시간순)
    @Query("SELECT r FROM FanChallengeRecord r WHERE r.artist = :artist ORDER BY r.correctCount DESC, r.bestTimeMs ASC")
    List<FanChallengeRecord> findTopByArtist(@Param("artist") String artist, Pageable pageable);

    // 회원의 모든 기록 조회
    List<FanChallengeRecord> findByMemberOrderByAchievedAtDesc(Member member);

    // 회원의 퍼펙트 클리어 수
    @Query("SELECT COUNT(r) FROM FanChallengeRecord r WHERE r.member = :member AND r.isPerfectClear = true")
    long countPerfectClearsByMember(@Param("member") Member member);

    // 아티스트별 도전자 수
    @Query("SELECT COUNT(r) FROM FanChallengeRecord r WHERE r.artist = :artist")
    long countByArtist(@Param("artist") String artist);
}
