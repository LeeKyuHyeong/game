package com.kh.game.repository;

import com.kh.game.entity.MemberLoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MemberLoginHistoryRepository extends JpaRepository<MemberLoginHistory, Long> {

    // 전체 이력 (최신순)
    Page<MemberLoginHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 회원별 이력
    Page<MemberLoginHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    // 이메일로 검색
    Page<MemberLoginHistory> findByEmailContainingOrderByCreatedAtDesc(String email, Pageable pageable);

    // 결과별 조회
    Page<MemberLoginHistory> findByResultOrderByCreatedAtDesc(MemberLoginHistory.LoginResult result, Pageable pageable);

    // 기간별 조회
    Page<MemberLoginHistory> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end, Pageable pageable);

    // 실패 로그인만 조회
    @Query("SELECT h FROM MemberLoginHistory h WHERE h.result != 'SUCCESS' ORDER BY h.createdAt DESC")
    Page<MemberLoginHistory> findFailedLogins(Pageable pageable);

    // 특정 IP의 최근 실패 횟수
    @Query("SELECT COUNT(h) FROM MemberLoginHistory h WHERE h.ipAddress = :ip AND h.result != 'SUCCESS' AND h.createdAt > :since")
    long countRecentFailsByIp(@Param("ip") String ipAddress, @Param("since") LocalDateTime since);

    // 특정 이메일의 최근 실패 횟수
    @Query("SELECT COUNT(h) FROM MemberLoginHistory h WHERE h.email = :email AND h.result != 'SUCCESS' AND h.createdAt > :since")
    long countRecentFailsByEmail(@Param("email") String email, @Param("since") LocalDateTime since);

    // 일별 로그인 통계
    @Query("SELECT DATE(h.createdAt), h.result, COUNT(h) FROM MemberLoginHistory h " +
            "WHERE h.createdAt >= :since GROUP BY DATE(h.createdAt), h.result ORDER BY DATE(h.createdAt) DESC")
    List<Object[]> getDailyLoginStats(@Param("since") LocalDateTime since);
}