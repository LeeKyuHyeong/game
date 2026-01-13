package com.kh.game.repository;

import com.kh.game.entity.GameSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    List<GameSession> findBySessionUuidOrderByCreatedAtDesc(String sessionUuid);

    List<GameSession> findByNicknameOrderByCreatedAtDesc(String nickname);

    Page<GameSession> findByNicknameContaining(String nickname, Pageable pageable);

    Page<GameSession> findByStatus(GameSession.GameStatus status, Pageable pageable);

    Page<GameSession> findByGameType(GameSession.GameType gameType, Pageable pageable);

    Page<GameSession> findByGameTypeAndStatus(GameSession.GameType gameType, GameSession.GameStatus status, Pageable pageable);

    @Query("SELECT gs FROM GameSession gs WHERE gs.status = 'COMPLETED' ORDER BY gs.totalScore DESC")
    List<GameSession> findTopScores(Pageable pageable);

    @Query("SELECT gs FROM GameSession gs WHERE gs.createdAt >= :startDate AND gs.status = 'COMPLETED' ORDER BY gs.totalScore DESC")
    List<GameSession> findTopScoresSince(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    // 게임 타입별 랭킹 조회
    @Query("SELECT gs FROM GameSession gs WHERE gs.gameType = :gameType AND gs.status = 'COMPLETED' ORDER BY gs.totalScore DESC")
    List<GameSession> findTopScoresByGameType(@Param("gameType") GameSession.GameType gameType, Pageable pageable);

    @Query("SELECT gs FROM GameSession gs WHERE gs.gameType = :gameType AND gs.createdAt >= :startDate AND gs.status = 'COMPLETED' ORDER BY gs.totalScore DESC")
    List<GameSession> findTopScoresByGameTypeSince(@Param("gameType") GameSession.GameType gameType, @Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query("SELECT COUNT(gs) FROM GameSession gs WHERE gs.createdAt >= :startDate")
    Long countGamesSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT AVG(gs.totalScore) FROM GameSession gs WHERE gs.status = 'COMPLETED'")
    Double getAverageScore();

    @Query("SELECT gs.gameType, COUNT(gs) FROM GameSession gs GROUP BY gs.gameType")
    List<Object[]> countByGameType();

    @Query("SELECT DATE(gs.createdAt), COUNT(gs) FROM GameSession gs WHERE gs.createdAt >= :startDate GROUP BY DATE(gs.createdAt) ORDER BY DATE(gs.createdAt)")
    List<Object[]> countByDateSince(@Param("startDate") LocalDateTime startDate);

    // 좀비 세션 처리: 특정 시간 이상 PLAYING 상태인 세션을 ABANDONED로 변경
    @Modifying
    @Query("UPDATE GameSession gs SET gs.status = :newStatus, gs.endedAt = CURRENT_TIMESTAMP WHERE gs.status = :currentStatus AND gs.startedAt < :threshold")
    int markZombieSessionsAsAbandoned(@Param("currentStatus") GameSession.GameStatus currentStatus,
                                      @Param("newStatus") GameSession.GameStatus newStatus,
                                      @Param("threshold") LocalDateTime threshold);

    // 오래된 세션 삭제 (상태별)
    @Modifying
    @Query("DELETE FROM GameSession gs WHERE gs.status = :status AND gs.endedAt < :threshold")
    int deleteOldSessionsByStatus(@Param("status") GameSession.GameStatus status,
                                  @Param("threshold") LocalDateTime threshold);
}