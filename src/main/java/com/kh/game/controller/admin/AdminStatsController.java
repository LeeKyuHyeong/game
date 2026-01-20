package com.kh.game.controller.admin;

import com.kh.game.service.SongService;
import com.kh.game.service.WrongAnswerStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final WrongAnswerStatsService wrongAnswerStatsService;
    private final SongService songService;

    @GetMapping("/wrong-answers")
    public String wrongAnswerStats(Model model) {
        // 요약 정보
        Map<String, Object> summary = wrongAnswerStatsService.getStatsSummary();

        // 가장 흔한 오답 TOP 20 (오답+정답 쌍으로 그룹핑)
        List<Map<String, Object>> commonWrongAnswers = wrongAnswerStatsService.getMostCommonWrongAnswersWithSong(20);

        // 가장 어려운 곡 TOP 20 (최소 5회 이상 플레이된 곡)
        List<Map<String, Object>> hardestSongs = wrongAnswerStatsService.getHardestSongs(5, 20);

        // 최근 오답 50개
        List<Map<String, Object>> recentWrongAnswers = wrongAnswerStatsService.getRecentWrongAnswers(50);

        model.addAttribute("summary", summary);
        model.addAttribute("commonWrongAnswers", commonWrongAnswers);
        model.addAttribute("hardestSongs", hardestSongs);
        model.addAttribute("recentWrongAnswers", recentWrongAnswers);
        model.addAttribute("menu", "wrong-stats");

        return "admin/stats/wrong-answers";
    }

    @GetMapping("/wrong-answers/song/{songId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getWrongAnswersForSong(
            @PathVariable Long songId,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> wrongAnswers = wrongAnswerStatsService.getWrongAnswersForSong(songId, limit);
        return ResponseEntity.ok(wrongAnswers);
    }

    // ========================================
    // 곡 대중성 통계 페이지
    // ========================================

    @GetMapping("/popularity")
    public String songPopularityStats(
            @RequestParam(defaultValue = "10") int minPlays,
            @RequestParam(defaultValue = "false") boolean filterJunk,
            Model model) {

        // 정크 데이터 요약
        Map<String, Object> junkSummary = wrongAnswerStatsService.getJunkDataSummary();

        // 대중성 불일치 곡 목록 (검토 필요)
        List<Map<String, Object>> mismatches = wrongAnswerStatsService.getSongPopularityMismatch(minPlays);

        // 전체 대중성 통계 (정답률 순)
        List<Map<String, Object>> popularityStats = wrongAnswerStatsService.getSongPopularityStats(minPlays, 100);

        // 필터링된 오답 (정크 제외)
        List<Map<String, Object>> filteredWrongAnswers = filterJunk
                ? wrongAnswerStatsService.getMostCommonWrongAnswersWithSongFiltered(30)
                : wrongAnswerStatsService.getMostCommonWrongAnswersWithSong(30);

        model.addAttribute("junkSummary", junkSummary);
        model.addAttribute("mismatches", mismatches);
        model.addAttribute("popularityStats", popularityStats);
        model.addAttribute("filteredWrongAnswers", filteredWrongAnswers);
        model.addAttribute("minPlays", minPlays);
        model.addAttribute("filterJunk", filterJunk);
        model.addAttribute("menu", "popularity-stats");

        return "admin/stats/popularity";
    }

    @PostMapping("/popularity/update/{songId}")
    @ResponseBody
    public ResponseEntity<?> updateSongPopularity(
            @PathVariable Long songId,
            @RequestParam boolean isPopular) {
        try {
            songService.updatePopularity(songId, isPopular);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/popularity/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPopularityStatsApi(
            @RequestParam(defaultValue = "10") int minPlays) {
        Map<String, Object> result = Map.of(
                "junkSummary", wrongAnswerStatsService.getJunkDataSummary(),
                "mismatches", wrongAnswerStatsService.getSongPopularityMismatch(minPlays),
                "popularityStats", wrongAnswerStatsService.getSongPopularityStats(minPlays, 100)
        );
        return ResponseEntity.ok(result);
    }
}
