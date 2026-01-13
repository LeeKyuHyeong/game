package com.kh.game.repository;

import com.kh.game.entity.GameRound;
import com.kh.game.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // Song 참조를 null로 설정 (Song 삭제 전 호출)
    @Modifying
    @Query("UPDATE GameRound gr SET gr.song = null WHERE gr.song.id = :songId")
    void clearSongReference(@Param("songId") Long songId);

    // 특정 회원의 곡별 플레이 통계 (정답 횟수, 총 플레이 횟수)
    @Query("SELECT gr.song.id, gr.song.title, gr.song.artist, " +
            "SUM(CASE WHEN gr.isCorrect = true THEN 1 ELSE 0 END) as correctCount, " +
            "COUNT(gr) as totalCount " +
            "FROM GameRound gr " +
            "WHERE gr.gameSession.member.id = :memberId AND gr.song IS NOT NULL " +
            "GROUP BY gr.song.id, gr.song.title, gr.song.artist " +
            "ORDER BY totalCount DESC, correctCount DESC")
    List<Object[]> findMemberSongStatistics(@Param("memberId") Long memberId);

    // 특정 회원이 가장 많이 맞춘 곡 TOP N
    @Query("SELECT gr.song.id, gr.song.title, gr.song.artist, " +
            "SUM(CASE WHEN gr.isCorrect = true THEN 1 ELSE 0 END) as correctCount, " +
            "COUNT(gr) as totalCount " +
            "FROM GameRound gr " +
            "WHERE gr.gameSession.member.id = :memberId AND gr.song IS NOT NULL " +
            "GROUP BY gr.song.id, gr.song.title, gr.song.artist " +
            "HAVING SUM(CASE WHEN gr.isCorrect = true THEN 1 ELSE 0 END) > 0 " +
            "ORDER BY correctCount DESC, totalCount DESC")
    List<Object[]> findMemberMostCorrectSongs(@Param("memberId") Long memberId);

    // 특정 회원이 가장 많이 틀린 곡 TOP N (정답률 기준)
    @Query("SELECT gr.song.id, gr.song.title, gr.song.artist, " +
            "SUM(CASE WHEN gr.isCorrect = true THEN 1 ELSE 0 END) as correctCount, " +
            "COUNT(gr) as totalCount " +
            "FROM GameRound gr " +
            "WHERE gr.gameSession.member.id = :memberId AND gr.song IS NOT NULL " +
            "GROUP BY gr.song.id, gr.song.title, gr.song.artist " +
            "HAVING COUNT(gr) >= 2 " +
            "ORDER BY (SUM(CASE WHEN gr.isCorrect = true THEN 1 ELSE 0 END) * 1.0 / COUNT(gr)) ASC")
    List<Object[]> findMemberHardestSongs(@Param("memberId") Long memberId);
}
