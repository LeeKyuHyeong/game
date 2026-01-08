package com.kh.game.repository;

import com.kh.game.entity.GameRoundAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRoundAttemptRepository extends JpaRepository<GameRoundAttempt, Long> {

    List<GameRoundAttempt> findByGameRoundIdOrderByAttemptNumberAsc(Long gameRoundId);

    // 특정 노래에 대한 모든 오답 조회
    @Query("SELECT gra FROM GameRoundAttempt gra " +
            "JOIN gra.gameRound gr " +
            "WHERE gr.song.id = :songId AND gra.isCorrect = false " +
            "ORDER BY gra.createdAt DESC")
    List<GameRoundAttempt> findWrongAnswersBySongId(@Param("songId") Long songId);

    // 특정 노래에 대한 정답 시도 횟수 통계
    @Query("SELECT gr.attemptCount, COUNT(gr) FROM GameRound gr " +
            "WHERE gr.song.id = :songId AND gr.isCorrect = true " +
            "GROUP BY gr.attemptCount " +
            "ORDER BY gr.attemptCount")
    List<Object[]> getAttemptStatsBySongId(@Param("songId") Long songId);
}