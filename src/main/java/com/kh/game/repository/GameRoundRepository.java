package com.kh.game.repository;

import com.kh.game.entity.GameRound;
import com.kh.game.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRoundRepository extends JpaRepository<GameRound, Long> {

    List<GameRound> findByGameSessionOrderByRoundNumberAsc(GameSession gameSession);

    List<GameRound> findByGameSessionIdOrderByRoundNumberAsc(Long gameSessionId);

    @Query("SELECT gr FROM GameRound gr WHERE gr.gameSession.id = :sessionId AND gr.roundNumber = :roundNumber")
    GameRound findBySessionAndRound(@Param("sessionId") Long sessionId, @Param("roundNumber") Integer roundNumber);

    @Query("SELECT COUNT(gr) FROM GameRound gr WHERE gr.song.id = :songId AND gr.isCorrect = true")
    Long countCorrectBySongId(@Param("songId") Long songId);

    @Query("SELECT COUNT(gr) FROM GameRound gr WHERE gr.song.id = :songId")
    Long countTotalBySongId(@Param("songId") Long songId);

    @Query("SELECT gr.song.id, gr.song.title, gr.song.artist, COUNT(gr), " +
            "SUM(CASE WHEN gr.isCorrect = true THEN 1 ELSE 0 END) " +
            "FROM GameRound gr WHERE gr.song IS NOT NULL GROUP BY gr.song.id, gr.song.title, gr.song.artist ORDER BY COUNT(gr) DESC")
    List<Object[]> findSongStatistics();

    @Query("SELECT gr.genre.id, gr.genre.name, COUNT(gr), " +
            "SUM(CASE WHEN gr.isCorrect = true THEN 1 ELSE 0 END) " +
            "FROM GameRound gr WHERE gr.genre IS NOT NULL GROUP BY gr.genre.id, gr.genre.name ORDER BY COUNT(gr) DESC")
    List<Object[]> findGenreStatistics();
}