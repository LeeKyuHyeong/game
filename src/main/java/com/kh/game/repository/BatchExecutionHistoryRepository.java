package com.kh.game.repository;

import com.kh.game.entity.BatchExecutionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BatchExecutionHistoryRepository extends JpaRepository<BatchExecutionHistory, Long> {

    List<BatchExecutionHistory> findByBatchIdOrderByExecutedAtDesc(String batchId);

    List<BatchExecutionHistory> findTop10ByBatchIdOrderByExecutedAtDesc(String batchId);

    Page<BatchExecutionHistory> findAllByOrderByExecutedAtDesc(Pageable pageable);

    Page<BatchExecutionHistory> findByBatchIdOrderByExecutedAtDesc(String batchId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM BatchExecutionHistory h WHERE h.executedAt < :before")
    int deleteOldHistory(@Param("before") LocalDateTime before);

    @Query("SELECT COUNT(h) FROM BatchExecutionHistory h WHERE h.batchId = :batchId AND h.executedAt >= :after")
    long countRecentExecutions(@Param("batchId") String batchId, @Param("after") LocalDateTime after);
}
