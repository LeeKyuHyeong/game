package com.kh.game.repository;

import com.kh.game.entity.GameRoundAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    // 가장 흔한 오답 TOP N (전체)
    @Query("SELECT gra.userAnswer, COUNT(gra) as cnt FROM GameRoundAttempt gra " +
            "WHERE gra.isCorrect = false " +
            "GROUP BY gra.userAnswer " +
            "ORDER BY cnt DESC")
    List<Object[]> findMostCommonWrongAnswers();

    // 특정 곡에 대한 가장 흔한 오답
    @Query("SELECT gra.userAnswer, COUNT(gra) as cnt FROM GameRoundAttempt gra " +
            "JOIN gra.gameRound gr " +
            "WHERE gr.song.id = :songId AND gra.isCorrect = false " +
            "GROUP BY gra.userAnswer " +
            "ORDER BY cnt DESC")
    List<Object[]> findMostCommonWrongAnswersBySong(@Param("songId") Long songId);

    // 오답률이 높은 곡 (정답률이 낮은 곡)
    @Query("SELECT gr.song.id, gr.song.title, gr.song.artist, " +
            "SUM(CASE WHEN gr.isCorrect = true THEN 1 ELSE 0 END) as correct, " +
            "COUNT(gr) as total " +
            "FROM GameRound gr " +
            "WHERE gr.song IS NOT NULL AND gr.status != 'WAITING' " +
            "GROUP BY gr.song.id, gr.song.title, gr.song.artist " +
            "HAVING COUNT(gr) >= :minPlays " +
            "ORDER BY (SUM(CASE WHEN gr.isCorrect = true THEN 1 ELSE 0 END) * 1.0 / COUNT(gr))")
    List<Object[]> findHardestSongs(@Param("minPlays") int minPlays);

    // 최근 오답 목록
    @Query("SELECT gra.userAnswer, gr.song.title, gr.song.artist, gra.createdAt " +
            "FROM GameRoundAttempt gra " +
            "JOIN gra.gameRound gr " +
            "WHERE gra.isCorrect = false AND gr.song IS NOT NULL " +
            "ORDER BY gra.createdAt DESC")
    List<Object[]> findRecentWrongAnswers();

    // 오답+정답 쌍으로 그룹핑 (어떤 곡에서 어떤 오답이 몇 번 나왔는지)
    @Query("SELECT gra.userAnswer, gr.song.id, gr.song.title, gr.song.artist, COUNT(gra) as cnt " +
            "FROM GameRoundAttempt gra " +
            "JOIN gra.gameRound gr " +
            "WHERE gra.isCorrect = false AND gr.song IS NOT NULL " +
            "GROUP BY gra.userAnswer, gr.song.id, gr.song.title, gr.song.artist " +
            "ORDER BY cnt DESC")
    List<Object[]> findMostCommonWrongAnswersWithSong();

    // 지정된 날짜 이전에 생성된 시도 기록 삭제
    @Modifying
    @Query("DELETE FROM GameRoundAttempt gra WHERE gra.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);
}
