package com.kh.game.repository;

import com.kh.game.entity.SongHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SongHistoryRepository extends JpaRepository<SongHistory, Long> {

    /**
     * 특정 곡의 이력 조회 (최신순)
     */
    List<SongHistory> findBySongIdOrderByActionAtDesc(Long songId);

    /**
     * 특정 아티스트의 이력 조회 (최신순)
     */
    List<SongHistory> findByArtistOrderByActionAtDesc(String artist);

    /**
     * 특정 시점의 유효 곡 수 계산
     * - ADDED 이력이 있고
     * - 해당 시점 이전에 추가되었으며
     * - 해당 시점에 삭제되지 않은 곡 (DELETED 이력이 없거나, DELETED 후 RESTORED 된 곡)
     */
    @Query(value = """
            SELECT COUNT(DISTINCT h1.song_id)
            FROM song_history h1
            WHERE h1.artist = :artist
              AND h1.action = 'ADDED'
              AND h1.action_at <= :targetTime
              AND NOT EXISTS (
                  SELECT 1 FROM song_history h2
                  WHERE h2.song_id = h1.song_id
                    AND h2.action = 'DELETED'
                    AND h2.action_at <= :targetTime
                    AND NOT EXISTS (
                        SELECT 1 FROM song_history h3
                        WHERE h3.song_id = h2.song_id
                          AND h3.action = 'RESTORED'
                          AND h3.action_at > h2.action_at
                          AND h3.action_at <= :targetTime
                    )
              )
            """, nativeQuery = true)
    int countActiveSongsAtTime(@Param("artist") String artist, @Param("targetTime") LocalDateTime targetTime);

    /**
     * 1년 이상 된 이력 삭제
     */
    @Modifying
    @Query("DELETE FROM SongHistory h WHERE h.actionAt < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 특정 아티스트의 특정 기간 이력 조회
     */
    @Query("SELECT h FROM SongHistory h WHERE h.artist = :artist AND h.actionAt BETWEEN :startTime AND :endTime ORDER BY h.actionAt")
    List<SongHistory> findByArtistAndPeriod(
            @Param("artist") String artist,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
