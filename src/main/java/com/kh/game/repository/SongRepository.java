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

    List<Song> findByUseYnAndYoutubeVideoIdIsNotNull(String useYn);

    // YouTube 또는 MP3 파일이 있는 곡 조회
    @Query("SELECT s FROM Song s WHERE s.useYn = :useYn AND (s.youtubeVideoId IS NOT NULL OR s.filePath IS NOT NULL)")
    List<Song> findByUseYnAndHasAudioSource(@Param("useYn") String useYn);

    Page<Song> findByTitleContainingOrArtistContaining(String title, String artist, Pageable pageable);

    Page<Song> findByTitleContainingOrArtistContainingAndUseYn(String title, String artist, String useYn, Pageable pageable);

    // 아티스트 필터링
    Page<Song> findByArtist(String artist, Pageable pageable);

    Page<Song> findByArtistAndUseYn(String artist, String useYn, Pageable pageable);

    List<Song> findByArtistAndUseYnAndFilePathIsNotNull(String artist, String useYn);

    // 아티스트 필터링 (YouTube 또는 MP3)
    @Query("SELECT s FROM Song s WHERE s.artist = :artist AND s.useYn = :useYn AND (s.youtubeVideoId IS NOT NULL OR s.filePath IS NOT NULL)")
    List<Song> findByArtistAndUseYnAndHasAudioSource(@Param("artist") String artist, @Param("useYn") String useYn);

    // 아티스트 목록 조회 (곡 수 포함) - YouTube 또는 MP3
    @Query("SELECT s.artist, COUNT(s) FROM Song s WHERE s.useYn = 'Y' AND (s.youtubeVideoId IS NOT NULL OR s.filePath IS NOT NULL) GROUP BY s.artist ORDER BY s.artist")
    List<Object[]> findDistinctArtistsWithCount();

    // 아티스트 검색 (자동완성용) - YouTube 또는 MP3
    @Query("SELECT DISTINCT s.artist FROM Song s WHERE s.useYn = 'Y' AND (s.youtubeVideoId IS NOT NULL OR s.filePath IS NOT NULL) AND LOWER(s.artist) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY s.artist")
    List<String> findArtistsByKeyword(@Param("keyword") String keyword);

    // 복합 필터 검색 (관리자용) - 단일 아티스트
    @Query("SELECT s FROM Song s WHERE " +
            "(:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.artist) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:artist IS NULL OR s.artist = :artist) AND " +
            "(:genreId IS NULL OR s.genre.id = :genreId) AND " +
            "(:useYn IS NULL OR s.useYn = :useYn) AND " +
            "(:isSolo IS NULL OR s.isSolo = :isSolo)")
    Page<Song> searchWithFilters(
            @Param("keyword") String keyword,
            @Param("artist") String artist,
            @Param("genreId") Long genreId,
            @Param("useYn") String useYn,
            @Param("isSolo") Boolean isSolo,
            Pageable pageable);

    // 복합 필터 검색 (관리자용) - 다중 아티스트
    @Query("SELECT s FROM Song s WHERE " +
            "(:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.artist) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(s.artist IN :artists) AND " +
            "(:genreId IS NULL OR s.genre.id = :genreId) AND " +
            "(:useYn IS NULL OR s.useYn = :useYn) AND " +
            "(:isSolo IS NULL OR s.isSolo = :isSolo)")
    Page<Song> searchWithFiltersMultipleArtists(
            @Param("keyword") String keyword,
            @Param("artists") List<String> artists,
            @Param("genreId") Long genreId,
            @Param("useYn") String useYn,
            @Param("isSolo") Boolean isSolo,
            Pageable pageable);

    // 전체 아티스트 목록 (관리자용 - 전체 곡 대상)
    @Query("SELECT DISTINCT s.artist FROM Song s WHERE s.artist IS NOT NULL ORDER BY s.artist")
    List<String> findAllDistinctArtists();

    // 중복된 YouTube video ID 목록 조회 (활성 곡 중 2개 이상 있는 것)
    @Query("SELECT s.youtubeVideoId FROM Song s " +
            "WHERE s.youtubeVideoId IS NOT NULL AND s.useYn = 'Y' " +
            "GROUP BY s.youtubeVideoId " +
            "HAVING COUNT(s) > 1")
    List<String> findDuplicateYoutubeVideoIds();

    // 특정 YouTube video ID를 가진 곡 조회 (ID 오름차순)
    List<Song> findByYoutubeVideoIdAndUseYnOrderByIdAsc(String youtubeVideoId, String useYn);
}