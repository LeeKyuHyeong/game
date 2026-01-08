package com.kh.game.controller.admin;

import com.kh.game.dto.GameSettings;
import com.kh.game.entity.GameRound;
import com.kh.game.entity.GameSession;
import com.kh.game.service.GameSessionService;
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
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/history")
@RequiredArgsConstructor
public class AdminGameHistoryController {

    private final GameSessionService gameSessionService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String gameType,
                       @RequestParam(required = false) String status,
                       Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<GameSession> sessionPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            sessionPage = gameSessionService.search(keyword, pageable);
            model.addAttribute("keyword", keyword);
        } else if (gameType != null && !gameType.isEmpty()) {
            sessionPage = gameSessionService.findByGameType(
                    GameSession.GameType.valueOf(gameType), pageable);
            model.addAttribute("gameType", gameType);
        } else if (status != null && !status.isEmpty()) {
            sessionPage = gameSessionService.findByStatus(
                    GameSession.GameStatus.valueOf(status), pageable);
            model.addAttribute("status", status);
        } else {
            sessionPage = gameSessionService.findAll(pageable);
        }

        model.addAttribute("sessions", sessionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", sessionPage.getTotalPages());
        model.addAttribute("totalItems", sessionPage.getTotalElements());
        model.addAttribute("todayCount", gameSessionService.getTodayGameCount());
        model.addAttribute("avgScore", gameSessionService.getAverageScore());
        model.addAttribute("menu", "history");

        return "admin/history/list";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        GameSession session = gameSessionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        List<GameRound> rounds = gameSessionService.findRoundsBySessionId(id);
        GameSettings settings = gameSessionService.parseSettings(session.getSettings());

        model.addAttribute("gameSession", session);
        model.addAttribute("rounds", rounds);
        model.addAttribute("settings", settings);
        model.addAttribute("menu", "history");

        return "admin/history/detail";
    }

    @GetMapping("/detail/{id}/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detailJson(@PathVariable Long id) {
        return gameSessionService.findById(id)
                .map(session -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("session", session);
                    result.put("rounds", gameSessionService.findRoundsBySessionId(id));
                    result.put("settings", gameSessionService.parseSettings(session.getSettings()));
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            gameSessionService.deleteById(id);
            result.put("success", true);
            result.put("message", "삭제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "삭제 중 오류가 발생했습니다.");
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ranking")
    public String ranking(@RequestParam(defaultValue = "all") String period, Model model) {
        List<GameSession> rankings;

        switch (period) {
            case "daily":
                rankings = gameSessionService.getDailyTopScores(50);
                break;
            case "weekly":
                rankings = gameSessionService.getWeeklyTopScores(50);
                break;
            default:
                rankings = gameSessionService.getTopScores(50);
        }

        model.addAttribute("rankings", rankings);
        model.addAttribute("period", period);
        model.addAttribute("menu", "ranking");

        return "admin/history/ranking";
    }
}