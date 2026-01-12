package com.kh.game.service;

import com.kh.game.entity.Member;
import com.kh.game.entity.Song;
import com.kh.game.entity.SongReport;
import com.kh.game.repository.SongReportRepository;
import com.kh.game.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SongReportService {

    private final SongReportRepository reportRepository;
    private final SongRepository songRepository;

    /**
     * 곡 신고 제출
     */
    @Transactional
    public Map<String, Object> submitReport(Long songId, Member member, String sessionId,
                                            SongReport.ReportType reportType, String description) {
        Map<String, Object> result = new HashMap<>();

        // 곡 존재 확인
        Song song = songRepository.findById(songId).orElse(null);
        if (song == null) {
            result.put("success", false);
            result.put("message", "곡을 찾을 수 없습니다.");
            return result;
        }

        // 중복 신고 체크
        Optional<SongReport> existingReport;
        if (member != null) {
            existingReport = reportRepository.findBySongAndMember(song, member);
        } else {
            existingReport = reportRepository.findBySongAndSessionId(song, sessionId);
        }

        if (existingReport.isPresent()) {
            result.put("success", false);
            result.put("message", "이미 해당 곡을 신고하셨습니다.");
            return result;
        }

        // 신고 저장
        SongReport report = new SongReport(song, member, sessionId, reportType);
        if (description != null && !description.trim().isEmpty()) {
            report.setDescription(description.trim());
        }
        reportRepository.save(report);

        log.info("곡 신고 접수 - songId: {}, type: {}, member: {}",
                songId, reportType, member != null ? member.getId() : "guest(" + sessionId + ")");

        result.put("success", true);
        result.put("message", "신고가 접수되었습니다. 검토 후 조치하겠습니다.");
        return result;
    }

    /**
     * 신고 목록 조회 (관리자)
     */
    public Page<SongReport> getReports(SongReport.ReportStatus status, Pageable pageable) {
        if (status != null) {
            return reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * 신고 상세 조회
     */
    public Optional<SongReport> findById(Long id) {
        return reportRepository.findById(id);
    }

    /**
     * 신고 처리 (관리자)
     */
    @Transactional
    public Map<String, Object> processReport(Long reportId, SongReport.ReportStatus newStatus,
                                             String adminNote, Member admin) {
        Map<String, Object> result = new HashMap<>();

        SongReport report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            result.put("success", false);
            result.put("message", "신고를 찾을 수 없습니다.");
            return result;
        }

        report.process(newStatus, adminNote, admin);

        log.info("곡 신고 처리 - reportId: {}, status: {}, admin: {}",
                reportId, newStatus, admin.getId());

        result.put("success", true);
        result.put("message", "신고가 처리되었습니다.");
        return result;
    }

    /**
     * 곡 비활성화 처리 (신고 확인 후)
     */
    @Transactional
    public Map<String, Object> disableSong(Long reportId, Member admin) {
        Map<String, Object> result = new HashMap<>();

        SongReport report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            result.put("success", false);
            result.put("message", "신고를 찾을 수 없습니다.");
            return result;
        }

        Song song = report.getSong();
        song.setUseYn("N");
        report.process(SongReport.ReportStatus.RESOLVED, "곡 비활성화 처리", admin);

        log.info("신고된 곡 비활성화 - songId: {}, reportId: {}, admin: {}",
                song.getId(), reportId, admin.getId());

        result.put("success", true);
        result.put("message", "곡이 비활성화되었습니다.");
        return result;
    }

    /**
     * 대기 중인 신고 수
     */
    public long getPendingCount() {
        return reportRepository.countByStatus(SongReport.ReportStatus.PENDING);
    }

    /**
     * 특정 곡의 신고 수
     */
    public long getReportCountBySong(Long songId) {
        Song song = songRepository.findById(songId).orElse(null);
        if (song == null) return 0;
        return reportRepository.countBySongAndStatus(song, SongReport.ReportStatus.PENDING);
    }
}
