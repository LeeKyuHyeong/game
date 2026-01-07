package com.kh.game.repository;

import com.kh.game.entity.SongAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongAnswerRepository extends JpaRepository<SongAnswer, Long> {

    List<SongAnswer> findBySongId(Long songId);

    List<SongAnswer> findBySongIdOrderByIsPrimaryDesc(Long songId);

    void deleteBySongId(Long songId);

    @Query("SELECT sa FROM SongAnswer sa WHERE sa.song.id = :songId AND LOWER(REPLACE(sa.answer, ' ', '')) = LOWER(REPLACE(:answer, ' ', ''))")
    List<SongAnswer> findMatchingAnswer(@Param("songId") Long songId, @Param("answer") String answer);

    boolean existsBySongId(Long songId);
}