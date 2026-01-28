package com.kh.game.repository;

import com.kh.game.entity.RankingHistory;
import com.kh.game.entity.RankingHistory.PeriodType;
import com.kh.game.entity.RankingHistory.RankingType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RankingHistoryRepository extends JpaRepository<RankingHistory, Long> {

    // ========== 기본 조회 ==========

    /**
     * 기간 유형과 랭킹 유형으로 조회
     */
    List<RankingHistory> findByPeriodTypeAndRankingTypeOrderByRankPositionAsc(
            PeriodType periodType, RankingType rankingType);

    /**
     * 특정 기간의 랭킹 조회
     */
    List<RankingHistory> findByPeriodTypeAndRankingTypeAndPeriodStartAndPeriodEndOrderByRankPositionAsc(
            PeriodType periodType, RankingType rankingType, LocalDate periodStart, LocalDate periodEnd);

    /**
     * 특정 기간에 스냅샷이 이미 존재하는지 확인
     */
    boolean existsByPeriodTypeAndRankingTypeAndPeriodStartAndPeriodEnd(
            PeriodType periodType, RankingType rankingType, LocalDate periodStart, LocalDate periodEnd);

    /**
     * 특정 기간의 스냅샷 개수 조회
     */
    long countByPeriodTypeAndRankingTypeAndPeriodStartAndPeriodEnd(
            PeriodType periodType, RankingType rankingType, LocalDate periodStart, LocalDate periodEnd);

    // ========== 회원별 조회 ==========

    /**
     * 특정 회원의 랭킹 히스토리 조회 (최신순)
     */
    List<RankingHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 특정 회원의 랭킹 히스토리 조회 (페이징)
     */
    List<RankingHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /**
     * 특정 회원의 특정 랭킹 타입 히스토리 조회
     */
    List<RankingHistory> findByMemberIdAndRankingTypeOrderByCreatedAtDesc(
            Long memberId, RankingType rankingType);

    /**
     * 특정 회원의 최고 순위 조회
     */
    @Query("SELECT MIN(rh.rankPosition) FROM RankingHistory rh WHERE rh.memberId = :memberId")
    Optional<Integer> findBestRankByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 회원의 특정 랭킹 타입 최고 순위 조회
     */
    @Query("SELECT MIN(rh.rankPosition) FROM RankingHistory rh " +
           "WHERE rh.memberId = :memberId AND rh.rankingType = :rankingType")
    Optional<Integer> findBestRankByMemberIdAndRankingType(
            @Param("memberId") Long memberId, @Param("rankingType") RankingType rankingType);

    // ========== 명예의 전당 (역대 1위 목록) ==========

    /**
     * 특정 랭킹 타입의 역대 1위 목록 조회
     */
    @Query("SELECT rh FROM RankingHistory rh WHERE rh.rankPosition = 1 AND rh.rankingType = :rankingType " +
           "ORDER BY rh.periodStart DESC")
    List<RankingHistory> findAllFirstPlaceByRankingType(@Param("rankingType") RankingType rankingType);

    /**
     * 특정 랭킹 타입의 역대 1위 목록 조회 (페이징)
     */
    @Query("SELECT rh FROM RankingHistory rh WHERE rh.rankPosition = 1 AND rh.rankingType = :rankingType " +
           "ORDER BY rh.periodStart DESC")
    List<RankingHistory> findAllFirstPlaceByRankingType(
            @Param("rankingType") RankingType rankingType, Pageable pageable);

    /**
     * 1위 횟수가 가장 많은 회원 조회
     */
    @Query("SELECT rh.memberId, rh.nickname, COUNT(rh) as winCount FROM RankingHistory rh " +
           "WHERE rh.rankPosition = 1 AND rh.rankingType = :rankingType " +
           "GROUP BY rh.memberId, rh.nickname ORDER BY winCount DESC")
    List<Object[]> findMostFirstPlacesByRankingType(
            @Param("rankingType") RankingType rankingType, Pageable pageable);

    // ========== 통계 조회 ==========

    /**
     * 특정 기간 유형의 총 스냅샷 수
     */
    long countByPeriodType(PeriodType periodType);

    /**
     * 특정 랭킹 타입의 총 스냅샷 수
     */
    long countByRankingType(RankingType rankingType);

    /**
     * 가장 최근 스냅샷 날짜 조회
     */
    @Query("SELECT MAX(rh.periodEnd) FROM RankingHistory rh WHERE rh.periodType = :periodType")
    Optional<LocalDate> findLatestPeriodEndByPeriodType(@Param("periodType") PeriodType periodType);

    /**
     * 특정 랭킹 타입의 가장 최근 스냅샷 조회
     */
    @Query("SELECT rh FROM RankingHistory rh WHERE rh.rankingType = :rankingType " +
           "ORDER BY rh.periodEnd DESC, rh.rankPosition ASC")
    List<RankingHistory> findLatestByRankingType(@Param("rankingType") RankingType rankingType, Pageable pageable);

    // ========== 기간 목록 조회 ==========

    /**
     * 조회 가능한 기간 목록 (기간 유형별 고유한 기간 시작/종료 목록)
     */
    @Query("SELECT DISTINCT rh.periodType, rh.periodStart, rh.periodEnd FROM RankingHistory rh " +
           "ORDER BY rh.periodEnd DESC")
    List<Object[]> findDistinctPeriods();

    /**
     * 특정 기간의 랭킹 데이터 조회
     */
    @Query("SELECT rh FROM RankingHistory rh WHERE rh.periodStart = :periodStart AND rh.periodEnd = :periodEnd " +
           "ORDER BY rh.rankingType ASC, rh.rankPosition ASC")
    List<RankingHistory> findByPeriod(@Param("periodStart") LocalDate periodStart,
                                      @Param("periodEnd") LocalDate periodEnd);

    // ========== 삭제 ==========

    /**
     * 특정 기간 이전의 스냅샷 삭제 (데이터 정리용)
     */
    void deleteByPeriodEndBefore(LocalDate date);

    /**
     * 특정 기간의 스냅샷 삭제 (재생성용)
     */
    void deleteByPeriodTypeAndRankingTypeAndPeriodStartAndPeriodEnd(
            PeriodType periodType, RankingType rankingType, LocalDate periodStart, LocalDate periodEnd);
}
