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

    // ========== Solo Guess (내가맞추기) 랭킹 조회 ==========

    // 총점 기준
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.guessGames > 0 ORDER BY m.guessScore DESC")
    List<Member> findTopGuessRankingByScore(Pageable pageable);

    // 정답률 기준
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.guessRounds > 0 ORDER BY (m.guessCorrect * 1.0 / m.guessRounds) DESC")
    List<Member> findTopGuessRankingByAccuracy(Pageable pageable);

    // 게임 수 기준
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.guessGames > 0 ORDER BY m.guessGames DESC")
    List<Member> findTopGuessRankingByGames(Pageable pageable);

    // ========== Multiplayer (멀티게임) 랭킹 조회 ==========

    // 총점 기준
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.multiGames > 0 ORDER BY m.multiScore DESC")
    List<Member> findTopMultiRankingByScore(Pageable pageable);

    // 정답률 기준
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.multiRounds > 0 ORDER BY (m.multiCorrect * 1.0 / m.multiRounds) DESC")
    List<Member> findTopMultiRankingByAccuracy(Pageable pageable);

    // 게임 수 기준
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.multiGames > 0 ORDER BY m.multiGames DESC")
    List<Member> findTopMultiRankingByGames(Pageable pageable);

    // 검색
    Page<Member> findByEmailContainingOrNicknameContaining(String email, String nickname, Pageable pageable);

    // 상태별 조회
    Page<Member> findByStatus(Member.MemberStatus status, Pageable pageable);

    // 상태별 조회 (List)
    List<Member> findByStatus(Member.MemberStatus status);

    // ========== 주간 랭킹 조회 ==========

    // 주간 내가맞추기 총점
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.weeklyGuessGames > 0 ORDER BY m.weeklyGuessScore DESC")
    List<Member> findTopWeeklyGuessRankingByScore(Pageable pageable);

    // 주간 멀티게임 총점
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.weeklyMultiGames > 0 ORDER BY m.weeklyMultiScore DESC")
    List<Member> findTopWeeklyMultiRankingByScore(Pageable pageable);

    // ========== 최고 기록 랭킹 조회 ==========

    // 내가맞추기 최고 점수
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.bestGuessScore > 0 ORDER BY m.bestGuessScore DESC")
    List<Member> findTopGuessBestScore(Pageable pageable);

    // 멀티게임 최고 점수
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.bestMultiScore > 0 ORDER BY m.bestMultiScore DESC")
    List<Member> findTopMultiBestScore(Pageable pageable);

    // ========== 티어별 조회 ==========

    // 티어별 회원 수
    @Query("SELECT m.tier, COUNT(m) FROM Member m WHERE m.status = 'ACTIVE' GROUP BY m.tier ORDER BY m.tier")
    List<Object[]> countByTier();
}