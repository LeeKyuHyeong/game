package com.kh.game.repository;

import com.kh.game.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    // 닉네임으로 시작하는 회원 목록 (동명이인 처리용)
    @Query("SELECT m.nickname FROM Member m WHERE m.nickname LIKE :nickname% ORDER BY m.nickname")
    List<String> findNicknamesStartingWith(String nickname);

    // 활성 회원만 조회
    Optional<Member> findByEmailAndStatus(String email, Member.MemberStatus status);

    // 랭킹 조회 (총점 기준)
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.totalGames > 0 ORDER BY m.totalScore DESC")
    List<Member> findTopByTotalScore(Pageable pageable);

    // 랭킹 조회 (정답률 기준)
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.totalRounds > 0 ORDER BY (m.totalCorrect * 1.0 / m.totalRounds) DESC")
    List<Member> findTopByAccuracy(Pageable pageable);

    // 랭킹 조회 (게임 수 기준)
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' ORDER BY m.totalGames DESC")
    List<Member> findTopByTotalGames(Pageable pageable);

    // 검색
    Page<Member> findByEmailContainingOrNicknameContaining(String email, String nickname, Pageable pageable);

    // 상태별 조회
    Page<Member> findByStatus(Member.MemberStatus status, Pageable pageable);
}