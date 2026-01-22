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
import java.util.Optional;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    @Query("SELECT DISTINCT gs FROM GameSession gs LEFT JOIN FETCH gs.rounds r LEFT JOIN FETCH r.song WHERE gs.id = :id")
    Optional<GameSession> findByIdWithRounds(@Param("id") Long id);

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

    // ========== 30곡 챌린지 랭킹 (점수 → 소요시간 순) ==========

    /**
     * 주간 30곡 최고 기록 랭킹
     * - 각 회원의 최고 기록만 선택 (점수 높고, 소요시간 짧은 것)
     * - 점수 내림차순, 소요시간 오름차순 정렬
     */
    @Query(value = """
        WITH ranked AS (
            SELECT
                gs.member_id,
                m.nickname,
                gs.total_score,
                TIMESTAMPDIFF(SECOND, gs.started_at, gs.ended_at) as duration_seconds,
                gs.ended_at,
                ROW_NUMBER() OVER (
                    PARTITION BY gs.member_id
                    ORDER BY gs.total_score DESC,
                             TIMESTAMPDIFF(SECOND, gs.started_at, gs.ended_at) ASC
                ) as rn
            FROM game_session gs
            JOIN member m ON gs.member_id = m.id
            WHERE gs.total_rounds = 30
              AND gs.game_type = 'SOLO_GUESS'
              AND gs.status = 'COMPLETED'
              AND gs.member_id IS NOT NULL
              AND m.status = 'ACTIVE'
              AND gs.started_at >= :periodStart
        )
        SELECT member_id, nickname, total_score, duration_seconds, ended_at
        FROM ranked
        WHERE rn = 1
        ORDER BY total_score DESC, duration_seconds ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findWeeklyBest30RankingByDuration(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("limit") int limit);

    /**
     * 월간 30곡 최고 기록 랭킹
     */
    @Query(value = """
        WITH ranked AS (
            SELECT
                gs.member_id,
                m.nickname,
                gs.total_score,
                TIMESTAMPDIFF(SECOND, gs.started_at, gs.ended_at) as duration_seconds,
                gs.ended_at,
                ROW_NUMBER() OVER (
                    PARTITION BY gs.member_id
                    ORDER BY gs.total_score DESC,
                             TIMESTAMPDIFF(SECOND, gs.started_at, gs.ended_at) ASC
                ) as rn
            FROM game_session gs
            JOIN member m ON gs.member_id = m.id
            WHERE gs.total_rounds = 30
              AND gs.game_type = 'SOLO_GUESS'
              AND gs.status = 'COMPLETED'
              AND gs.member_id IS NOT NULL
              AND m.status = 'ACTIVE'
              AND gs.started_at >= :periodStart
        )
        SELECT member_id, nickname, total_score, duration_seconds, ended_at
        FROM ranked
        WHERE rn = 1
        ORDER BY total_score DESC, duration_seconds ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findMonthlyBest30RankingByDuration(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("limit") int limit);

    /**
     * 역대 30곡 최고 기록 랭킹 (명예의 전당)
     */
    @Query(value = """
        WITH ranked AS (
            SELECT
                gs.member_id,
                m.nickname,
                gs.total_score,
                TIMESTAMPDIFF(SECOND, gs.started_at, gs.ended_at) as duration_seconds,
                gs.ended_at,
                ROW_NUMBER() OVER (
                    PARTITION BY gs.member_id
                    ORDER BY gs.total_score DESC,
                             TIMESTAMPDIFF(SECOND, gs.started_at, gs.ended_at) ASC
                ) as rn
            FROM game_session gs
            JOIN member m ON gs.member_id = m.id
            WHERE gs.total_rounds = 30
              AND gs.game_type = 'SOLO_GUESS'
              AND gs.status = 'COMPLETED'
              AND gs.member_id IS NOT NULL
              AND m.status = 'ACTIVE'
        )
        SELECT member_id, nickname, total_score, duration_seconds, ended_at
        FROM ranked
        WHERE rn = 1
        ORDER BY total_score DESC, duration_seconds ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findAllTimeBest30RankingByDuration(@Param("limit") int limit);

    /**
     * 특정 회원의 30곡 최고 기록 조회 (주간)
     */
    @Query(value = """
        SELECT
            gs.total_score,
            TIMESTAMPDIFF(SECOND, gs.started_at, gs.ended_at) as duration_seconds,
            gs.ended_at
        FROM game_session gs
        WHERE gs.member_id = :memberId
          AND gs.total_rounds = 30
          AND gs.game_type = 'SOLO_GUESS'
          AND gs.status = 'COMPLETED'
          AND gs.started_at >= :periodStart
        ORDER BY gs.total_score DESC, duration_seconds ASC
        LIMIT 1
        """, nativeQuery = true)
    List<Object[]> findMemberBest30Record(
            @Param("memberId") Long memberId,
            @Param("periodStart") LocalDateTime periodStart);

    /**
     * 특정 회원의 역대 30곡 최고 기록 조회
     */
    @Query(value = """
        SELECT
            gs.total_score,
            TIMESTAMPDIFF(SECOND, gs.started_at, gs.ended_at) as duration_seconds,
            gs.ended_at
        FROM game_session gs
        WHERE gs.member_id = :memberId
          AND gs.total_rounds = 30
          AND gs.game_type = 'SOLO_GUESS'
          AND gs.status = 'COMPLETED'
        ORDER BY gs.total_score DESC, duration_seconds ASC
        LIMIT 1
        """, nativeQuery = true)
    List<Object[]> findMemberAllTimeBest30Record(@Param("memberId") Long memberId);

    /**
     * 내 주간 30곡 순위 조회 (나보다 높은 점수 또는 같은 점수+빠른 시간 가진 사람 수)
     */
    @Query(value = """
        WITH my_best AS (
            SELECT
                gs.total_score,
                TIMESTAMPDIFF(SECOND, gs.started_at, gs.ended_at) as duration_seconds
            FROM game_session gs
            WHERE gs.member_id = :memberId
              AND gs.total_rounds = 30
              AND gs.game_type = 'SOLO_GUESS'
              AND gs.status = 'COMPLETED'
              AND gs.started_at >= :periodStart
            ORDER BY gs.total_score DESC, duration_seconds ASC
            LIMIT 1
        ),
        others_best AS (
            SELECT
                gs.member_id,
                gs.total_score,
                TIMESTAMPDIFF(SECOND, gs.started_at, gs.ended_at) as duration_seconds,
                ROW_NUMBER() OVER (
                    PARTITION BY gs.member_id
                    ORDER BY gs.total_score DESC,
                             TIMESTAMPDIFF(SECOND, gs.started_at, gs.ended_at) ASC
                ) as rn
            FROM game_session gs
            JOIN member m ON gs.member_id = m.id
            WHERE gs.total_rounds = 30
              AND gs.game_type = 'SOLO_GUESS'
              AND gs.status = 'COMPLETED'
              AND gs.member_id IS NOT NULL
              AND gs.member_id != :memberId
              AND m.status = 'ACTIVE'
              AND gs.started_at >= :periodStart
        )
        SELECT COUNT(*)
        FROM others_best ob, my_best mb
        WHERE ob.rn = 1
          AND (ob.total_score > mb.total_score
               OR (ob.total_score = mb.total_score AND ob.duration_seconds < mb.duration_seconds))
        """, nativeQuery = true)
    Long countHigherRankedMembers(
            @Param("memberId") Long memberId,
            @Param("periodStart") LocalDateTime periodStart);

    /**
     * 주간 30곡 참여자 수
     */
    @Query(value = """
        SELECT COUNT(DISTINCT gs.member_id)
        FROM game_session gs
        JOIN member m ON gs.member_id = m.id
        WHERE gs.total_rounds = 30
          AND gs.game_type = 'SOLO_GUESS'
          AND gs.status = 'COMPLETED'
          AND gs.member_id IS NOT NULL
          AND m.status = 'ACTIVE'
          AND gs.started_at >= :periodStart
        """, nativeQuery = true)
    Long countBest30Participants(@Param("periodStart") LocalDateTime periodStart);

    /**
     * 역대 30곡 참여자 수
     */
    @Query(value = """
        SELECT COUNT(DISTINCT gs.member_id)
        FROM game_session gs
        JOIN member m ON gs.member_id = m.id
        WHERE gs.total_rounds = 30
          AND gs.game_type = 'SOLO_GUESS'
          AND gs.status = 'COMPLETED'
          AND gs.member_id IS NOT NULL
          AND m.status = 'ACTIVE'
        """, nativeQuery = true)
    Long countAllTimeBest30Participants();

    // ========== 관리자 회원관리용 - 실시간 게임 수 집계 ==========

    /**
     * 여러 회원의 GameSession 게임 수를 한 번에 조회 (N+1 방지)
     * @return List of [memberId, count]
     */
    @Query("SELECT gs.member.id, COUNT(gs) FROM GameSession gs " +
           "WHERE gs.member.id IN :memberIds AND gs.status = 'COMPLETED' " +
           "GROUP BY gs.member.id")
    List<Object[]> countGamesByMemberIds(@Param("memberIds") List<Long> memberIds);
}