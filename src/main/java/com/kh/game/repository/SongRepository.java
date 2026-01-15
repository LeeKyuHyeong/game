package com.kh.game.repository;

import com.kh.game.entity.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {

    Page<Song> findByUseYn(String useYn, Pageable pageable);

    List<Song> findByUseYn(String useYn);

    List<Song> findByUseYnAndFilePathIsNotNull(String useYn);

    List<Song> findByUseYnAndYoutubeVideoIdIsNotNull(String useYn);

    // YouTube 또는 MP3 파일이 있는 곡 조회
    @Query("SELECT s FROM Song s WHERE s.useYn = :useYn AND (s.youtubeVideoId IS NOT NULL OR s.filePath IS NOT NULL)")
    List<Song> findByUseYnAndHasAudioSource(@Param("useYn") String useYn);

    // YouTube 또는 MP3 파일이 있는 곡 조회 (특정 장르 제외 - 게임용)
    @Query("SELECT s FROM Song s WHERE s.useYn = :useYn AND (s.youtubeVideoId IS NOT NULL OR s.filePath IS NOT NULL) AND (s.genre IS NULL OR s.genre.code <> :excludeGenreCode)")
    List<Song> findByUseYnAndHasAudioSourceExcludingGenre(@Param("useYn") String useYn, @Param("excludeGenreCode") String excludeGenreCode);

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

    // 아티스트 목록 조회 (곡 수 포함, 특정 장르 제외) - 게임용
    @Query("SELECT s.artist, COUNT(s) FROM Song s WHERE s.useYn = 'Y' AND (s.youtubeVideoId IS NOT NULL OR s.filePath IS NOT NULL) AND (s.genre IS NULL OR s.genre.code <> :excludeGenreCode) GROUP BY s.artist ORDER BY s.artist")
    List<Object[]> findDistinctArtistsWithCountExcludingGenre(@Param("excludeGenreCode") String excludeGenreCode);

    // 아티스트 검색 (자동완성용) - YouTube 또는 MP3
    @Query("SELECT DISTINCT s.artist FROM Song s WHERE s.useYn = 'Y' AND (s.youtubeVideoId IS NOT NULL OR s.filePath IS NOT NULL) AND LOWER(s.artist) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY s.artist")
    List<String> findArtistsByKeyword(@Param("keyword") String keyword);

    // 아티스트 검색 (자동완성용, 특정 장르 제외) - 게임용
    @Query("SELECT DISTINCT s.artist FROM Song s WHERE s.useYn = 'Y' AND (s.youtubeVideoId IS NOT NULL OR s.filePath IS NOT NULL) AND (s.genre IS NULL OR s.genre.code <> :excludeGenreCode) AND LOWER(s.artist) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY s.artist")
    List<String> findArtistsByKeywordExcludingGenre(@Param("keyword") String keyword, @Param("excludeGenreCode") String excludeGenreCode);

    // 복합 필터 검색 (관리자용) - 단일 아티스트
    @Query("SELECT s FROM Song s WHERE " +
            "(:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.artist) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:artist IS NULL OR s.artist = :artist) AND " +
            "(:genreId IS NULL OR s.genre.id = :genreId) AND " +
            "(:useYn IS NULL OR s.useYn = :useYn) AND " +
            "(:isSolo IS NULL OR s.isSolo = :isSolo) AND " +
            "(:releaseYear IS NULL OR s.releaseYear = :releaseYear)")
    Page<Song> searchWithFilters(
            @Param("keyword") String keyword,
            @Param("artist") String artist,
            @Param("genreId") Long genreId,
            @Param("useYn") String useYn,
            @Param("isSolo") Boolean isSolo,
            @Param("releaseYear") Integer releaseYear,
            Pageable pageable);

    // 복합 필터 검색 (관리자용) - 다중 아티스트
    @Query("SELECT s FROM Song s WHERE " +
            "(:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.artist) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(s.artist IN :artists) AND " +
            "(:genreId IS NULL OR s.genre.id = :genreId) AND " +
            "(:useYn IS NULL OR s.useYn = :useYn) AND " +
            "(:isSolo IS NULL OR s.isSolo = :isSolo) AND " +
            "(:releaseYear IS NULL OR s.releaseYear = :releaseYear)")
    Page<Song> searchWithFiltersMultipleArtists(
            @Param("keyword") String keyword,
            @Param("artists") List<String> artists,
            @Param("genreId") Long genreId,
            @Param("useYn") String useYn,
            @Param("isSolo") Boolean isSolo,
            @Param("releaseYear") Integer releaseYear,
            Pageable pageable);

    // 전체 연도 목록 (관리자용 - 전체 곡 대상)
    @Query("SELECT DISTINCT s.releaseYear FROM Song s WHERE s.releaseYear IS NOT NULL ORDER BY s.releaseYear DESC")
    List<Integer> findAllDistinctYears();

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

    // 유효한 YouTube Video ID를 가진 곡만 조회 (null, 빈 문자열 제외) - Error 2 방지용
    @Query("SELECT s FROM Song s WHERE s.useYn = :useYn " +
           "AND s.youtubeVideoId IS NOT NULL " +
           "AND s.youtubeVideoId <> '' " +
           "AND LENGTH(TRIM(s.youtubeVideoId)) = 11")
    List<Song> findByUseYnAndValidYoutubeVideoId(@Param("useYn") String useYn);

    // 유효한 YouTube Video ID를 가진 곡 조회 (특정 장르 제외, ID 제외) - 멀티게임용
    @Query("SELECT s FROM Song s WHERE s.useYn = 'Y' " +
           "AND s.youtubeVideoId IS NOT NULL " +
           "AND s.youtubeVideoId <> '' " +
           "AND LENGTH(TRIM(s.youtubeVideoId)) = 11 " +
           "AND (s.genre IS NULL OR s.genre.code <> :excludeGenreCode) " +
           "AND s.id NOT IN :excludeIds")
    List<Song> findValidSongsExcluding(
            @Param("excludeGenreCode") String excludeGenreCode,
            @Param("excludeIds") Set<Long> excludeIds);

    // 장르별 유효한 YouTube Video ID를 가진 곡 조회 - 멀티게임용
    @Query("SELECT s FROM Song s WHERE s.useYn = 'Y' " +
           "AND s.youtubeVideoId IS NOT NULL " +
           "AND s.youtubeVideoId <> '' " +
           "AND LENGTH(TRIM(s.youtubeVideoId)) = 11 " +
           "AND (:genreId IS NULL OR s.genre.id = :genreId) " +
           "AND (s.genre IS NULL OR s.genre.code <> :excludeGenreCode) " +
           "AND s.id NOT IN :excludeIds")
    List<Song> findValidSongsByGenreExcluding(
            @Param("genreId") Long genreId,
            @Param("excludeGenreCode") String excludeGenreCode,
            @Param("excludeIds") Set<Long> excludeIds);

    // YouTube 유효성 플래그 기반 조회 (검증된 곡만)
    @Query("SELECT s FROM Song s WHERE s.useYn = 'Y' " +
           "AND s.youtubeVideoId IS NOT NULL " +
           "AND s.youtubeVideoId <> '' " +
           "AND (s.isYoutubeValid IS NULL OR s.isYoutubeValid = true) " +
           "AND (s.genre IS NULL OR s.genre.code <> :excludeGenreCode) " +
           "AND s.id NOT IN :excludeIds")
    List<Song> findYoutubeValidSongsExcluding(
            @Param("excludeGenreCode") String excludeGenreCode,
            @Param("excludeIds") Set<Long> excludeIds);

    // YouTube 무효화된 곡 조회 (배치용)
    @Query("SELECT s FROM Song s WHERE s.isYoutubeValid = false")
    List<Song> findInvalidYoutubeSongs();

    // YouTube 유효성 미확인 곡 조회 (배치용)
    @Query("SELECT s FROM Song s WHERE s.useYn = 'Y' " +
           "AND s.youtubeVideoId IS NOT NULL " +
           "AND s.youtubeVideoId <> '' " +
           "AND (s.youtubeCheckedAt IS NULL OR s.youtubeCheckedAt < :beforeDate)")
    List<Song> findSongsNeedingYoutubeCheck(@Param("beforeDate") LocalDateTime beforeDate);

    // ========== 이력 기반 관리용 메서드 ==========

    /**
     * 아티스트의 활성 곡 목록 조회
     */
    List<Song> findByArtistAndUseYn(String artist, String useYn);

    /**
     * 아티스트의 활성 곡 수 조회
     */
    @Query("SELECT COUNT(s) FROM Song s WHERE s.artist = :artist AND s.useYn = 'Y' " +
           "AND (s.youtubeVideoId IS NOT NULL OR s.filePath IS NOT NULL) " +
           "AND (s.genre IS NULL OR s.genre.code <> 'RETRO')")
    int countActiveSongsByArtist(@Param("artist") String artist);
}