package com.kh.game.repository;

import com.kh.game.entity.Member;
import com.kh.game.entity.Song;
import com.kh.game.entity.SongPopularityVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
