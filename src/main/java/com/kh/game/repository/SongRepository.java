package com.kh.game.repository;

import com.kh.game.entity.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {

    Page<Song> findByUseYn(String useYn, Pageable pageable);

    List<Song> findByUseYn(String useYn);

    List<Song> findByUseYnAndFilePathIsNotNull(String useYn);

    Page<Song> findByTitleContainingOrArtistContaining(String title, String artist, Pageable pageable);

    Page<Song> findByTitleContainingOrArtistContainingAndUseYn(String title, String artist, String useYn, Pageable pageable);

    // 아티스트 필터링
    Page<Song> findByArtist(String artist, Pageable pageable);

    Page<Song> findByArtistAndUseYn(String artist, String useYn, Pageable pageable);

    List<Song> findByArtistAndUseYnAndFilePathIsNotNull(String artist, String useYn);

    // 아티스트 목록 조회 (곡 수 포함)
    @Query("SELECT s.artist, COUNT(s) FROM Song s WHERE s.useYn = 'Y' AND s.filePath IS NOT NULL GROUP BY s.artist ORDER BY s.artist")
    List<Object[]> findDistinctArtistsWithCount();

    // 아티스트 검색 (자동완성용)
    @Query("SELECT DISTINCT s.artist FROM Song s WHERE s.useYn = 'Y' AND s.filePath IS NOT NULL AND LOWER(s.artist) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY s.artist")
    List<String> findArtistsByKeyword(@Param("keyword") String keyword);
}