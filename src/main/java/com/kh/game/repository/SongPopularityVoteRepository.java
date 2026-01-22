package com.kh.game.repository;

import com.kh.game.entity.Member;
import com.kh.game.entity.Song;
import com.kh.game.entity.SongPopularityVote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SongPopularityVoteRepository extends JpaRepository<SongPopularityVote, Long> {

    /**
     * 특정 곡에 대한 회원의 투표 조회
     */
    Optional<SongPopularityVote> findBySongAndMember(Song song, Member member);

    /**
     * 특정 곡에 회원이 이미 투표했는지 확인
     */
    boolean existsBySongAndMember(Song song, Member member);

    /**
     * 회원이 특정 곡 목록에 대해 투표한 기록 일괄 조회 (결과 화면용)
     */
    @Query("SELECT v FROM SongPopularityVote v WHERE v.member = :member AND v.song.id IN :songIds")
    List<SongPopularityVote> findByMemberAndSongIdIn(@Param("member") Member member, @Param("songIds") List<Long> songIds);

    /**
     * 특정 곡의 평균 평점 조회
     */
    @Query("SELECT AVG(v.rating) FROM SongPopularityVote v WHERE v.song = :song")
    Double getAverageRatingBySong(@Param("song") Song song);

    /**
     * 특정 곡의 투표 수 조회
     */
    long countBySong(Song song);

    // ========== 관리자용 통계 쿼리 ==========

    /**
     * 전체 투표 목록 (곡, 회원 정보 포함) - 페이징
     */
    @Query("SELECT v FROM SongPopularityVote v " +
           "JOIN FETCH v.song s " +
           "JOIN FETCH v.member m " +
           "ORDER BY v.createdAt DESC")
    List<SongPopularityVote> findAllWithSongAndMember();

    /**
     * 전체 투표 목록 - 페이징 (countQuery 분리)
     */
    @Query(value = "SELECT v FROM SongPopularityVote v " +
                   "JOIN FETCH v.song s " +
                   "JOIN FETCH v.member m",
           countQuery = "SELECT COUNT(v) FROM SongPopularityVote v")
    Page<SongPopularityVote> findAllWithSongAndMember(Pageable pageable);

    /**
     * 곡별 통계: songId, title, artist, avgRating, voteCount
     */
    @Query("SELECT s.id, s.title, s.artist, AVG(v.rating), COUNT(v), " +
           "SUM(CASE WHEN v.rating = 1 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN v.rating = 2 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN v.rating = 3 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN v.rating = 4 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN v.rating = 5 THEN 1 ELSE 0 END) " +
           "FROM SongPopularityVote v JOIN v.song s " +
           "GROUP BY s.id, s.title, s.artist " +
           "ORDER BY COUNT(v) DESC")
    List<Object[]> getSongStatistics();

    /**
     * 아티스트별 통계: artist, songCount, avgRating, totalVotes
     */
    @Query("SELECT s.artist, COUNT(DISTINCT s.id), AVG(v.rating), COUNT(v) " +
           "FROM SongPopularityVote v JOIN v.song s " +
           "GROUP BY s.artist " +
           "ORDER BY COUNT(v) DESC")
    List<Object[]> getArtistStatistics();

    /**
     * 특정 시간 이후 투표 수 (오늘 투표 수 조회용)
     */
    long countByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 특정 곡의 투표 목록
     */
    @Query("SELECT v FROM SongPopularityVote v " +
           "JOIN FETCH v.member m " +
           "WHERE v.song.id = :songId " +
           "ORDER BY v.createdAt DESC")
    List<SongPopularityVote> findBySongIdWithMember(@Param("songId") Long songId);

    /**
     * 특정 곡의 투표 목록 - 페이징
     */
    @Query(value = "SELECT v FROM SongPopularityVote v " +
                   "JOIN FETCH v.member m " +
                   "WHERE v.song.id = :songId",
           countQuery = "SELECT COUNT(v) FROM SongPopularityVote v WHERE v.song.id = :songId")
    Page<SongPopularityVote> findBySongIdWithMember(@Param("songId") Long songId, Pageable pageable);

    /**
     * 투표된 곡 수 (중복 제외)
     */
    @Query("SELECT COUNT(DISTINCT v.song.id) FROM SongPopularityVote v")
    long countDistinctSongs();

    /**
     * 전체 평균 평점
     */
    @Query("SELECT AVG(v.rating) FROM SongPopularityVote v")
    Double getOverallAverageRating();

    /**
     * 검색 (곡명 또는 아티스트)
     */
    @Query(value = "SELECT v FROM SongPopularityVote v " +
                   "JOIN FETCH v.song s " +
                   "JOIN FETCH v.member m " +
                   "WHERE LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                   "OR LOWER(s.artist) LIKE LOWER(CONCAT('%', :keyword, '%'))",
           countQuery = "SELECT COUNT(v) FROM SongPopularityVote v " +
                        "JOIN v.song s " +
                        "WHERE LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(s.artist) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<SongPopularityVote> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
