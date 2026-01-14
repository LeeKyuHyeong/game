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

    // 1. 누적 총점 기준
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.guessGames > 0 ORDER BY m.guessScore DESC")
    List<Member> findTopGuessRankingByScore(Pageable pageable);

    // 2. 평균 정답률 기준
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.guessRounds > 0 ORDER BY (m.guessCorrect * 1.0 / m.guessRounds) DESC")
    List<Member> findTopGuessRankingByAccuracy(Pageable pageable);

    // 3. 평균 점수 기준 (게임당 평균)
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.guessGames > 0 ORDER BY (m.guessScore * 1.0 / m.guessGames) DESC")
    List<Member> findTopGuessRankingByAvgScore(Pageable pageable);

    // 4. 최다 정답 기준
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.guessCorrect > 0 ORDER BY m.guessCorrect DESC")
    List<Member> findTopGuessRankingByCorrect(Pageable pageable);

    // 5. 플레이왕 - 게임 수 기준
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.guessGames > 0 ORDER BY m.guessGames DESC")
    List<Member> findTopGuessRankingByGames(Pageable pageable);

    // 6. 도전왕 - 라운드 수 기준
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.guessRounds > 0 ORDER BY m.guessRounds DESC")
    List<Member> findTopGuessRankingByRounds(Pageable pageable);

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

    // ========== 내 순위 조회 ==========

    // 내가맞추기 총점 순위 (나보다 높은 점수 가진 사람 수)
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE' AND m.guessGames > 0 AND m.guessScore > :score")
    long countMembersWithHigherGuessScore(int score);

    // 내가맞추기 총 참여자 수
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE' AND m.guessGames > 0")
    long countGuessParticipants();

    // ========== 멀티게임 LP 티어 랭킹 조회 ==========

    // 멀티 티어 + LP 기준 (티어 내림차순, 같은 티어면 LP 내림차순)
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.multiGames > 0 ORDER BY m.multiTier DESC, m.multiLp DESC")
    List<Member> findTopMultiTierRanking(Pageable pageable);

    // 멀티게임 1등 횟수 기준
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.multiWins > 0 ORDER BY m.multiWins DESC")
    List<Member> findTopMultiWins(Pageable pageable);

    // 멀티게임 Top3 횟수 기준
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.multiTop3 > 0 ORDER BY m.multiTop3 DESC")
    List<Member> findTopMultiTop3(Pageable pageable);

    // 내 멀티 티어 순위 (나보다 높은 티어 + LP 가진 사람 수)
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE' AND m.multiGames > 0 " +
           "AND (m.multiTier > :tier OR (m.multiTier = :tier AND m.multiLp > :lp))")
    long countMembersWithHigherMultiTier(com.kh.game.entity.MultiTier tier, int lp);

    // 멀티 티어별 회원 수
    @Query("SELECT m.multiTier, COUNT(m) FROM Member m WHERE m.status = 'ACTIVE' AND m.multiGames > 0 GROUP BY m.multiTier ORDER BY m.multiTier")
    List<Object[]> countByMultiTier();

    // ========== 30곡 최고점 랭킹 조회 ==========

    // 주간 30곡 최고점 (점수 내림차순, 같은 점수면 먼저 달성한 사람 우선)
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.weeklyBest30Score IS NOT NULL " +
           "ORDER BY m.weeklyBest30Score DESC, m.weeklyBest30At ASC")
    List<Member> findWeeklyBest30Ranking(Pageable pageable);

    // 월간 30곡 최고점
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.monthlyBest30Score IS NOT NULL " +
           "ORDER BY m.monthlyBest30Score DESC, m.monthlyBest30At ASC")
    List<Member> findMonthlyBest30Ranking(Pageable pageable);

    // 역대 30곡 최고점 (명예의 전당)
    @Query("SELECT m FROM Member m WHERE m.status = 'ACTIVE' AND m.allTimeBest30Score IS NOT NULL " +
           "ORDER BY m.allTimeBest30Score DESC, m.allTimeBest30At ASC")
    List<Member> findAllTimeBest30Ranking(Pageable pageable);

    // 내 주간 30곡 순위 (나보다 높은 점수 가진 사람 수)
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE' AND m.weeklyBest30Score > :score")
    long countMembersWithHigherWeeklyBest30Score(int score);

    // 내 월간 30곡 순위
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE' AND m.monthlyBest30Score > :score")
    long countMembersWithHigherMonthlyBest30Score(int score);

    // 내 역대 30곡 순위
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE' AND m.allTimeBest30Score > :score")
    long countMembersWithHigherAllTimeBest30Score(int score);

    // 주간 30곡 참여자 수
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE' AND m.weeklyBest30Score IS NOT NULL")
    long countWeeklyBest30Participants();

    // 월간 30곡 참여자 수
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE' AND m.monthlyBest30Score IS NOT NULL")
    long countMonthlyBest30Participants();

    // 역대 30곡 참여자 수
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE' AND m.allTimeBest30Score IS NOT NULL")
    long countAllTimeBest30Participants();

    // ========== 티어별 조회 (통합) ==========

    // 통합 티어별 회원 수
    @Query("SELECT m.tier, COUNT(m) FROM Member m WHERE m.status = 'ACTIVE' GROUP BY m.tier ORDER BY m.tier")
    List<Object[]> countByTier();

    // ========== 관리자용 조회 ==========

    // 권한별 조회
    Page<Member> findByRole(Member.MemberRole role, Pageable pageable);

    // 상태별 회원 수
    long countByStatus(Member.MemberStatus status);

    // 권한별 회원 수
    long countByRole(Member.MemberRole role);
}
