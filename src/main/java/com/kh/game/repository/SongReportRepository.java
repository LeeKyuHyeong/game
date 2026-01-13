package com.kh.game.repository;

import com.kh.game.entity.Member;
import com.kh.game.entity.Song;
import com.kh.game.entity.SongReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SongReportRepository extends JpaRepository<SongReport, Long> {

    // 특정 곡의 신고 목록
    List<SongReport> findBySongOrderByCreatedAtDesc(Song song);

    // 상태별 신고 목록
    Page<SongReport> findByStatusOrderByCreatedAtDesc(SongReport.ReportStatus status, Pageable pageable);

    // 전체 목록 (최신순)
    Page<SongReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 중복 신고 체크 (회원)
    Optional<SongReport> findBySongAndMember(Song song, Member member);

    // 중복 신고 체크 (게스트 - 세션ID)
    Optional<SongReport> findBySongAndSessionId(Song song, String sessionId);

    // 곡별 대기 중인 신고 수 집계
    @Query("SELECT r.song.id, COUNT(r) FROM SongReport r WHERE r.status = 'PENDING' GROUP BY r.song.id")
    List<Object[]> countPendingReportsBySong();

    // 특정 상태의 신고 수
    long countByStatus(SongReport.ReportStatus status);

    // 특정 곡의 특정 상태 신고 수
    long countBySongAndStatus(Song song, SongReport.ReportStatus status);

    // 곡 ID로 신고 목록 조회
    @Query("SELECT r FROM SongReport r WHERE r.song.id = :songId ORDER BY r.createdAt DESC")
    List<SongReport> findBySongId(@Param("songId") Long songId);

    // 곡 ID로 신고 삭제
    void deleteBySongId(Long songId);
}
