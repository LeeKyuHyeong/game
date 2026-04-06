package com.kh.game.controller.admin;

import com.kh.game.batch.BatchScheduler;
import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/batch")
@RequiredArgsConstructor
public class AdminBatchController {

    private final BatchService batchService;
    private final BatchScheduler batchScheduler;

    /**
     * 기존 URL → 통합 시스템 설정 페이지로 리다이렉트
     */
    @GetMapping({"", "/"})
    public String redirectToSystem() {
        return "redirect:/admin/system?tab=batch";
    }

    /**
     * AJAX 로딩용 배치 목록 콘텐츠 (fragment)
     */
    @GetMapping("/content")
    public String listContent(Model model) {
        List<BatchConfig> batches = batchService.findAll();

        long totalCount = batches.size();
        long implementedCount = batchService.countImplemented();
        long enabledCount = batchService.countEnabled();
        int scheduledCount = batchScheduler.getScheduledCount();

        model.addAttribute("batches", batches);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("implementedCount", implementedCount);
        model.addAttribute("enabledCount", enabledCount);
        model.addAttribute("scheduledCount", scheduledCount);
        model.addAttribute("menu", "system");

        return "admin/batch/fragments/batch";
    }

    /**
     * 배치 상세 정보
     */
    @GetMapping("/detail/{batchId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detail(@PathVariable String batchId) {
        return batchService.findById(batchId)
                .map(config -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("batchId", config.getBatchId());
                    result.put("name", config.getName());
                    result.put("description", config.getDescription());
                    result.put("cronExpression", config.getCronExpression());
                    result.put("scheduleText", config.getScheduleText());
                    result.put("enabled", config.getEnabled());
                    result.put("implemented", config.getImplemented());
                    result.put("targetEntity", config.getTargetEntity());
                    result.put("priority", config.getPriority() != null ? config.getPriority().name() : "MEDIUM");
                    result.put("lastExecutedAt", config.getLastExecutedAt());
                    result.put("lastResult", config.getLastResult() != null ? config.getLastResult().name() : null);
                    result.put("lastResultMessage", config.getLastResultMessage());
                    result.put("lastAffectedCount", config.getLastAffectedCount());
                    result.put("lastExecutionTimeMs", config.getLastExecutionTimeMs());
                    result.put("isScheduled", batchScheduler.isScheduled(batchId));

                    // 최근 실행 이력
                    List<BatchExecutionHistory> history = batchService.getRecentHistory(batchId);
                    result.put("recentHistory", history.stream().map(h -> {
                        Map<String, Object> hMap = new HashMap<>();
                        hMap.put("id", h.getId());
                        hMap.put("executionType", h.getExecutionType().name());
                        hMap.put("result", h.getResult().name());
                        hMap.put("message", h.getMessage());
                        hMap.put("affectedCount", h.getAffectedCount());
                        hMap.put("executionTimeMs", h.getExecutionTimeMs());
                        hMap.put("executedAt", h.getExecutedAt());
                        return hMap;
                    }).collect(Collectors.toList()));

                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 배치 수동 실행
     */
    @PostMapping("/run/{batchId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> runBatch(@PathVariable String batchId) {
        BatchConfig config = batchService.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("배치를 찾을 수 없습니다."));

        if (!config.getImplemented()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "아직 구현되지 않은 배치입니다."));
        }

        batchScheduler.executeManually(batchId);
        return ResponseEntity.ok(Map.of("success", true, "message", config.getName() + " 배치가 실행되었습니다."));
    }

    /**
     * 배치 설정 수정
     */
    @PostMapping("/update/{batchId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateConfig(
            @PathVariable String batchId,
            @RequestParam(required = false) String cronExpression,
            @RequestParam(required = false) String scheduleText,
            @RequestParam(required = false) Boolean enabled) {

        if (cronExpression != null && !cronExpression.isEmpty()) {
            if (!isValidCronExpression(cronExpression)) {
                return ResponseEntity.ok(Map.of("success", false, "message", "유효하지 않은 Cron 표현식입니다."));
            }
        }

        batchService.updateConfig(batchId, cronExpression, scheduleText, enabled);
        batchScheduler.refreshSchedule(batchId);
        return ResponseEntity.ok(Map.of("success", true, "message", "배치 설정이 수정되었습니다."));
    }

    /**
     * 배치 활성화/비활성화 토글
     */
    @PostMapping("/toggle/{batchId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleEnabled(@PathVariable String batchId) {
        boolean enabled = batchService.toggleEnabled(batchId);
        batchScheduler.refreshSchedule(batchId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("enabled", enabled);
        result.put("message", enabled ? "배치가 활성화되었습니다." : "배치가 비활성화되었습니다.");
        return ResponseEntity.ok(result);
    }

    /**
     * 모든 스케줄 새로고침
     */
    @PostMapping("/refresh-schedules")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> refreshSchedules() {
        batchScheduler.refreshAllSchedules();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("scheduledCount", batchScheduler.getScheduledCount());
        result.put("message", "스케줄이 새로고침되었습니다.");
        return ResponseEntity.ok(result);
    }

    /**
     * Cron 표현식 유효성 검사
     */
    private boolean isValidCronExpression(String cronExpression) {
        try {
            // Spring의 CronTrigger로 유효성 검사
            new org.springframework.scheduling.support.CronTrigger(cronExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
