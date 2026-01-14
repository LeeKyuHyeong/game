package com.kh.game.repository;

import com.kh.game.entity.Badge;
import com.kh.game.entity.Member;
import com.kh.game.entity.MemberBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberBadgeRepository extends JpaRepository<MemberBadge, Long> {

    List<MemberBadge> findByMemberOrderByEarnedAtDesc(Member member);

    List<MemberBadge> findByMemberAndIsNewTrue(Member member);

    boolean existsByMemberAndBadge(Member member, Badge badge);

    Optional<MemberBadge> findByMemberAndBadge(Member member, Badge badge);

    @Query("SELECT mb FROM MemberBadge mb JOIN FETCH mb.badge WHERE mb.member = :member ORDER BY mb.earnedAt DESC")
    List<MemberBadge> findByMemberWithBadge(@Param("member") Member member);

    @Modifying
    @Query("UPDATE MemberBadge mb SET mb.isNew = false WHERE mb.member = :member")
    void markAllAsRead(@Param("member") Member member);

    long countByMember(Member member);
}
