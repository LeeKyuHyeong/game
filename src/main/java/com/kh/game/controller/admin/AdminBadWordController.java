package com.kh.game.controller.admin;

import com.kh.game.entity.BadWord;
import com.kh.game.service.BadWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/badword")
@RequiredArgsConstructor
public class AdminBadWordController {

    private final BadWordService badWordService;

    /**
     * 기존 URL → 시스템 설정 페이지로 리다이렉트
     */
    @GetMapping({"", "/"})
    public String redirectToSystem() {
        return "redirect:/admin/system?tab=badword";
    }

    /**
     * AJAX 로딩용 비속어 목록 콘텐츠 (fragment)
     */
    @GetMapping("/content")
    public String listContent(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String active,
                       @RequestParam(defaultValue = "createdAt") String sort,
                       @RequestParam(defaultValue = "desc") String direction,
                       Model model) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<BadWord> badWordPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            badWordPage = badWordService.search(keyword, pageable);
        } else if (active != null && !active.isEmpty()) {
            boolean isActive = "Y".equals(active);
            badWordPage = badWordService.findByActive(isActive, pageable);
        } else {
            badWordPage = badWordService.findAll(pageable);
        }

        model.addAttribute("badWords", badWordPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", badWordPage.getTotalPages());
        model.addAttribute("totalItems", badWordPage.getTotalElements());
        model.addAttribute("activeCount", badWordService.countActive());
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", active);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("menu", "system");

        return "admin/badword/fragments/badword";
    }

    /**
     * 비속어 상세 조회
     */
    @GetMapping("/detail/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id) {
        return badWordService.findById(id)
                .map(badWord -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", badWord.getId());
                    result.put("word", badWord.getWord());
                    result.put("replacement", badWord.getReplacement());
                    result.put("isActive", badWord.getIsActive());
                    result.put("createdAt", badWord.getCreatedAt());
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 비속어 등록
     */
    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> save(
            @RequestParam(required = false) Long id,
            @RequestParam String word,
            @RequestParam(required = false) String replacement,
            @RequestParam(required = false, defaultValue = "true") Boolean isActive) {

        Map<String, Object> result = new HashMap<>();
        try {
            if (id != null) {
                // 수정
                badWordService.updateBadWord(id, word, replacement, isActive);
                result.put("message", "수정되었습니다.");
            } else {
                // 신규 등록
                badWordService.addBadWord(word, replacement);
                result.put("message", "등록되었습니다.");
            }
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 비속어 삭제
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            badWordService.deleteBadWord(id);
            result.put("success", true);
            result.put("message", "삭제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 활성화 상태 토글
     */
    @PostMapping("/toggle/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            badWordService.toggleActive(id);
            result.put("success", true);
            result.put("message", "상태가 변경되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "상태 변경 중 오류가 발생했습니다: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 필터링 테스트
     */
    @PostMapping("/test")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testFilter(@RequestParam String message) {
        Map<String, Object> result = new HashMap<>();
        try {
            String filtered = badWordService.filterMessage(message);
            boolean hasBadWord = badWordService.containsBadWord(message);

            result.put("success", true);
            result.put("original", message);
            result.put("filtered", filtered);
            result.put("hasBadWord", hasBadWord);
            result.put("foundWords", badWordService.findBadWords(message));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "테스트 중 오류가 발생했습니다: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 캐시 리로드
     */
    @PostMapping("/reload-cache")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reloadCache() {
        Map<String, Object> result = new HashMap<>();
        try {
            badWordService.reloadCache();
            result.put("success", true);
            result.put("message", "캐시가 갱신되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "캐시 갱신 중 오류가 발생했습니다: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 일괄 등록
     */
    @PostMapping("/bulk-add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkAdd(@RequestParam String words) {
        Map<String, Object> result = new HashMap<>();
        try {
            String[] wordArray = words.split("\n");
            int success = 0;
            int fail = 0;

            for (String word : wordArray) {
                word = word.trim();
                if (!word.isEmpty()) {
                    try {
                        badWordService.addBadWord(word, null);
                        success++;
                    } catch (Exception e) {
                        fail++;
                    }
                }
            }

            result.put("success", true);
            result.put("message", String.format("등록 완료 (성공: %d, 실패: %d)", success, fail));
            result.put("successCount", success);
            result.put("failCount", fail);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "일괄 등록 중 오류가 발생했습니다: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}