package com.kh.game.controller.admin;

import com.kh.game.entity.BatchAffectedSong;
import com.kh.game.entity.Member;
import com.kh.game.service.BatchService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 배치 영향 곡 관리 컨트롤러
 * - YouTube 검증 배치, 중복 곡 검사 배치 등에서 비활성화된 곡 조회
 * - 개별/일괄 복구 기능
 */
@Controller
@RequestMapping("/admin/batch-affected")
@RequiredArgsConstructor
public class AdminBatchAffectedController {

    private final BatchService batchService;

    /**
     * 메인 페이지
     */
    @GetMapping
    public String index(Model model) {
        Map<String, Object> stats = batchService.getAffectedSongStats();
        model.addAttribute("stats", stats);
        model.addAttribute("menu", "batch-affected");
        return "admin/batch-affected/index";
    }

    /**
     * 목록 콘텐츠 (AJAX 로딩용)
     */
    @GetMapping("/content")
    public String content(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String batchId,
                          @RequestParam(required = false) Boolean isRestored,
                          @RequestParam(required = false) String keyword,
                          Model model) {

        // 빈 문자열을 null로 변환
        if (batchId != null && batchId.trim().isEmpty()) {
            batchId = null;
        }
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<BatchAffectedSong> affectedPage = batchService.getAffectedSongs(batchId, isRestored, keyword, pageable);

        model.addAttribute("affectedList", affectedPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", affectedPage.getTotalPages());
        model.addAttribute("totalItems", affectedPage.getTotalElements());
        model.addAttribute("batchId", batchId);
        model.addAttribute("isRestored", isRestored);
        model.addAttribute("keyword", keyword);

        return "admin/batch-affected/fragments/list";
    }

    /**
     * 통계 정보 API
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = batchService.getAffectedSongStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 개별 곡 복구
     */
    @PostMapping("/restore/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> restoreSong(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        try {
            Member admin = (Member) session.getAttribute("member");
            if (admin == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(result);
            }

            boolean success = batchService.restoreSong(id, admin);
            if (success) {
                result.put("success", true);
                result.put("message", "곡이 복구되었습니다.");
            } else {
                result.put("success", false);
                result.put("message", "복구에 실패했습니다. 이미 복구되었거나 존재하지 않는 기록입니다.");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "복구 중 오류 발생: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 일괄 복구 (배치 실행 이력 기준)
     */
    @PostMapping("/restore-all/history/{historyId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> restoreAllByHistory(@PathVariable Long historyId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        try {
            Member admin = (Member) session.getAttribute("member");
            if (admin == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(result);
            }

            int restoredCount = batchService.restoreAllByHistory(historyId, admin);
            result.put("success", true);
            result.put("restoredCount", restoredCount);
            result.put("message", restoredCount + "곡이 복구되었습니다.");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "일괄 복구 중 오류 발생: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 일괄 복구 (배치 ID 기준 - 미복구 전체)
     */
    @PostMapping("/restore-all/batch/{batchId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> restoreAllByBatchId(@PathVariable String batchId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        try {
            Member admin = (Member) session.getAttribute("member");
            if (admin == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(result);
            }

            int restoredCount = batchService.restoreAllByBatchId(batchId, admin);
            result.put("success", true);
            result.put("restoredCount", restoredCount);
            result.put("message", restoredCount + "곡이 복구되었습니다.");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "일괄 복구 중 오류 발생: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 선택 복구 (여러 ID)
     */
    @PostMapping("/restore-selected")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> restoreSelected(@RequestBody Map<String, Object> request,
                                                                HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        try {
            Member admin = (Member) session.getAttribute("member");
            if (admin == null) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(result);
            }

            @SuppressWarnings("unchecked")
            java.util.List<Integer> ids = (java.util.List<Integer>) request.get("ids");
            if (ids == null || ids.isEmpty()) {
                result.put("success", false);
                result.put("message", "선택된 곡이 없습니다.");
                return ResponseEntity.ok(result);
            }

            int restoredCount = 0;
            for (Integer id : ids) {
                if (batchService.restoreSong(id.longValue(), admin)) {
                    restoredCount++;
                }
            }

            result.put("success", true);
            result.put("restoredCount", restoredCount);
            result.put("message", restoredCount + "곡이 복구되었습니다.");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "복구 중 오류 발생: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }
}
