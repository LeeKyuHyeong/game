package com.kh.game.repository;

import com.kh.game.entity.BatchAffectedSong;
import com.kh.game.entity.BatchAffectedSong.AffectedReason;
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
public interface BatchAffectedSongRepository extends JpaRepository<BatchAffectedSong, Long> {

    /**
     * 특정 배치 실행 이력의 영향받은 곡 목록 조회
     */
    List<BatchAffectedSong> findByHistoryIdOrderByIdDesc(Long historyId);

    /**
     * 특정 배치 실행 이력의 영향받은 곡 목록 조회 (페이징)
     */
    Page<BatchAffectedSong> findByHistoryIdOrderByIdDesc(Long historyId, Pageable pageable);

    /**
     * 특정 배치 ID의 영향받은 곡 목록 조회 (생성일 역순)
     */
    Page<BatchAffectedSong> findByBatchIdOrderByCreatedAtDesc(String batchId, Pageable pageable);

    /**
     * 전체 영향받은 곡 목록 조회 (생성일 역순)
     */
    Page<BatchAffectedSong> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 복구되지 않은 영향받은 곡 목록 조회
     */
    @Query("SELECT b FROM BatchAffectedSong b WHERE b.batchId = :batchId AND b.isRestored = false ORDER BY b.createdAt DESC")
    List<BatchAffectedSong> findUnrestoredByBatchId(@Param("batchId") String batchId);

    /**
     * 특정 배치 실행 이력의 사유별 통계
     */
    @Query("SELECT b.reason, COUNT(b) FROM BatchAffectedSong b WHERE b.history.id = :historyId GROUP BY b.reason")
    List<Object[]> countByReasonForHistory(@Param("historyId") Long historyId);

    /**
     * 배치 ID별 사유별 통계
     */
    @Query("SELECT b.reason, COUNT(b) FROM BatchAffectedSong b WHERE b.batchId = :batchId GROUP BY b.reason")
    List<Object[]> countByReasonForBatch(@Param("batchId") String batchId);

    /**
     * 복구 상태별 개수
     */
    @Query("SELECT b.isRestored, COUNT(b) FROM BatchAffectedSong b WHERE b.batchId = :batchId GROUP BY b.isRestored")
    List<Object[]> countByRestoredStatusForBatch(@Param("batchId") String batchId);

    /**
     * 전체 통계
     */
    @Query("SELECT b.isRestored, COUNT(b) FROM BatchAffectedSong b GROUP BY b.isRestored")
    List<Object[]> countByRestoredStatus();

    /**
     * 오래된 기록 삭제
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM BatchAffectedSong b WHERE b.createdAt < :before")
    int deleteOldRecords(@Param("before") LocalDateTime before);

    /**
     * 필터 조건으로 검색 (배치 ID, 복구 상태)
     */
    @Query("SELECT b FROM BatchAffectedSong b " +
           "WHERE (:batchId IS NULL OR b.batchId = :batchId) " +
           "AND (:isRestored IS NULL OR b.isRestored = :isRestored) " +
           "ORDER BY b.createdAt DESC")
    Page<BatchAffectedSong> findWithFilters(
            @Param("batchId") String batchId,
            @Param("isRestored") Boolean isRestored,
            Pageable pageable);

    /**
     * 검색 키워드 포함 필터
     */
    @Query("SELECT b FROM BatchAffectedSong b " +
           "WHERE (:batchId IS NULL OR b.batchId = :batchId) " +
           "AND (:isRestored IS NULL OR b.isRestored = :isRestored) " +
           "AND (:keyword IS NULL OR LOWER(b.song.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(b.song.artist) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY b.createdAt DESC")
    Page<BatchAffectedSong> searchWithFilters(
            @Param("batchId") String batchId,
            @Param("isRestored") Boolean isRestored,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 특정 곡이 영향받은 기록이 있는지 확인
     */
    boolean existsBySongIdAndIsRestoredFalse(Long songId);

    /**
     * 특정 배치 실행 이력의 전체 개수
     */
    long countByHistoryId(Long historyId);

    /**
     * 특정 배치 ID의 전체 개수
     */
    long countByBatchId(String batchId);

    /**
     * 특정 배치 ID의 미복구 개수
     */
    long countByBatchIdAndIsRestoredFalse(String batchId);

    /**
     * 전체 미복구 개수
     */
    long countByIsRestoredFalse();

    /**
     * 전체 복구 완료 개수
     */
    long countByIsRestoredTrue();
}
