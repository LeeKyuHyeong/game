package com.kh.game.repository;

import com.kh.game.entity.DailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyStatsRepository extends JpaRepository<DailyStats, Long> {

    Optional<DailyStats> findByStatDate(LocalDate statDate);

    @Query("SELECT d FROM DailyStats d WHERE d.statDate >= :startDate ORDER BY d.statDate DESC")
    List<DailyStats> findRecentStats(@Param("startDate") LocalDate startDate);

    @Query("SELECT d FROM DailyStats d ORDER BY d.statDate DESC")
    List<DailyStats> findAllOrderByDateDesc();
}
