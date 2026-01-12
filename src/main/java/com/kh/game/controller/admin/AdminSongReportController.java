package com.kh.game.controller.admin;

import com.kh.game.entity.Member;
import com.kh.game.entity.SongReport;
import com.kh.game.service.SongReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/admin/report")
@RequiredArgsConstructor
public class AdminSongReportController {

    private final SongReportService songReportService;

    /**
     * 신고 목록 페이지
     */
    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String status,
                       Model model) {

        Pageable pageable = PageRequest.of(page, size);
        SongReport.ReportStatus statusFilter = null;

        if (status != null && !status.isEmpty()) {
            try {
                statusFilter = SongReport.ReportStatus.valueOf(status);
            } catch (Exception ignored) {
            }
        }

        Page<SongReport> reports = songReportService.getReports(statusFilter, pageable);

        model.addAttribute("reports", reports.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reports.getTotalPages());
        model.addAttribute("totalElements", reports.getTotalElements());
        model.addAttribute("pendingCount", songReportService.getPendingCount());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("menu", "report");

        return "admin/report/list";
    }

    /**
     * 신고 처리
     */
    @PostMapping("/process/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> process(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @SessionAttribute("adminMember") Member admin) {

        String statusStr = request.get("status");
        String adminNote = request.getOrDefault("adminNote", "");

        SongReport.ReportStatus newStatus;
        try {
            newStatus = SongReport.ReportStatus.valueOf(statusStr);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "잘못된 상태입니다."));
        }

        return ResponseEntity.ok(songReportService.processReport(id, newStatus, adminNote, admin));
    }

    /**
     * 곡 비활성화 처리
     */
    @PostMapping("/disable-song/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> disableSong(
            @PathVariable Long id,
            @SessionAttribute("adminMember") Member admin) {

        return ResponseEntity.ok(songReportService.disableSong(id, admin));
    }
}
