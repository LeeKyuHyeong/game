package com.kh.game.controller.admin;

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
}
