package com.kh.game.controller.admin;

import com.kh.game.dto.GameSettings;
import com.kh.game.entity.*;
import com.kh.game.service.GameSessionService;
import com.kh.game.service.MemberService;
import com.kh.game.service.FanChallengeService;
import com.kh.game.service.GenreService;
import com.kh.game.repository.MemberRepository;
import com.kh.game.repository.FanChallengeRecordRepository;
import com.kh.game.repository.GameRoomRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Controller
@RequestMapping("/admin/history")
@RequiredArgsConstructor
public class AdminGameHistoryController {

    private final GameSessionService gameSessionService;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final FanChallengeRecordRepository fanChallengeRecordRepository;
    private final FanChallengeService fanChallengeService;
    private final GameRoomRepository gameRoomRepository;
    private final GenreService genreService;

    /**
     * 기존 URL → 통합 게임 관리 페이지로 리다이렉트
     */
    @GetMapping({"", "/"})
    public String redirectToGame(@RequestParam(defaultValue = "history") String tab) {
        return "redirect:/admin/game?tab=" + tab;
    }

    /**
     * AJAX 로딩용 게임 이력 콘텐츠 (fragment)
     */
    @GetMapping("/content")
    public String listContent(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String gameType,
                              @RequestParam(required = false) String status,
                              Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<GameSession> sessionPage;

        // 검색 조건 파싱
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        GameSession.GameType parsedGameType = null;
        GameSession.GameStatus parsedStatus = null;

        if (gameType != null && !gameType.isEmpty()) {
            try {
                parsedGameType = GameSession.GameType.valueOf(gameType);
            } catch (IllegalArgumentException e) {
                // 잘못된 gameType 무시
            }
        }
        if (status != null && !status.isEmpty()) {
            try {
                parsedStatus = GameSession.GameStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // 잘못된 status 무시
            }
        }

        // 검색 조건에 따라 조회
        if (hasKeyword) {
            sessionPage = gameSessionService.search(keyword, pageable);
        } else if (parsedGameType != null && parsedStatus != null) {
            sessionPage = gameSessionService.findByGameTypeAndStatus(parsedGameType, parsedStatus, pageable);
        } else if (parsedGameType != null) {
            sessionPage = gameSessionService.findByGameType(parsedGameType, pageable);
        } else if (parsedStatus != null) {
            sessionPage = gameSessionService.findByStatus(parsedStatus, pageable);
        } else {
            sessionPage = gameSessionService.findAll(pageable);
        }

        // 항상 검색 조건을 model에 추가 (폼 상태 유지)
        model.addAttribute("keyword", keyword);
        model.addAttribute("gameType", gameType);
        model.addAttribute("status", status);

        model.addAttribute("sessions", sessionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", sessionPage.getTotalPages());
        model.addAttribute("totalItems", sessionPage.getTotalElements());
        model.addAttribute("todayCount", gameSessionService.getTodayGameCount());
        model.addAttribute("avgScore", gameSessionService.getAverageScore());
        model.addAttribute("playingCount", gameRoomRepository.countByStatus(GameRoom.RoomStatus.PLAYING));
        model.addAttribute("menu", "game");

        return "admin/history/fragments/history";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        GameSession session = gameSessionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        List<GameRound> rounds = gameSessionService.findRoundsBySessionId(id);
        GameSettings settings = gameSessionService.parseSettings(session.getSettings());

        // 고정 장르 이름 조회
        String fixedGenreName = null;
        if (settings.getFixedGenreId() != null) {
            fixedGenreName = genreService.findById(settings.getFixedGenreId())
                    .map(Genre::getName)
                    .orElse(null);
        }

        model.addAttribute("gameSession", session);
        model.addAttribute("rounds", rounds);
        model.addAttribute("settings", settings);
        model.addAttribute("fixedGenreName", fixedGenreName);
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

    /**
     * AJAX 로딩용 랭킹 콘텐츠 (fragment)
     */
    @GetMapping("/ranking/content")
    public String rankingContent(
            @RequestParam(defaultValue = "guess") String rankType,
            @RequestParam(required = false) String artist,
            Model model) {

        // 멀티게임 LP 티어 분포 조회
        Map<String, Long> multiTierDistribution = new LinkedHashMap<>();
        multiTierDistribution.put("CHALLENGER", 0L);
        multiTierDistribution.put("MASTER", 0L);
        multiTierDistribution.put("DIAMOND", 0L);
        multiTierDistribution.put("PLATINUM", 0L);
        multiTierDistribution.put("GOLD", 0L);
        multiTierDistribution.put("SILVER", 0L);
        multiTierDistribution.put("BRONZE", 0L);

        List<Object[]> multiTierCounts = memberRepository.countByMultiTier();
        long totalMultiPlayers = 0;
        for (Object[] row : multiTierCounts) {
            if (row[0] != null) {
                String tierName = ((MultiTier) row[0]).name();
                Long count = (Long) row[1];
                multiTierDistribution.put(tierName, count);
                totalMultiPlayers += count;
            }
        }

        // 랭킹 타입에 따른 회원 조회
        List<Member> memberRankings = new ArrayList<>();
        List<FanChallengeRecord> fanRankings = new ArrayList<>();
        List<Map<String, Object>> fanArtistStats = new ArrayList<>();

        switch (rankType) {
            case "guess":
                memberRankings = memberService.getGuessRankingByScore(50);
                break;
            case "multi":
                memberRankings = memberService.getMultiTierRanking(50);
                break;
            case "weekly":
                memberRankings = memberService.getWeeklyGuessRankingByScore(50);
                break;
            case "best":
                memberRankings = memberService.getGuessBestScoreRanking(50);
                break;
            // 레트로 게임 랭킹
            case "retro":
                memberRankings = memberService.getRetroRankingByScore(50);
                break;
            case "retroWeekly":
                memberRankings = memberService.getWeeklyRetroRankingByScore(50);
                break;
            case "retroBest":
                memberRankings = memberService.getRetroBest30Ranking(50);
                break;
            // 멀티게임 주간
            case "weeklyMulti":
                memberRankings = memberService.getWeeklyMultiRankingByScore(50);
                break;
            // 팬챌린지 랭킹
            case "fan":
                // 인기 아티스트 목록 조회
                List<Object[]> popularArtists = fanChallengeRecordRepository.findPopularArtists(PageRequest.of(0, 20));
                for (Object[] row : popularArtists) {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("artist", row[0]);
                    stat.put("challengeCount", row[1]);
                    // 퍼펙트 클리어 수 조회
                    long perfectCount = fanChallengeRecordRepository.countPerfectClears();
                    stat.put("perfectCount", perfectCount);
                    fanArtistStats.add(stat);
                }
                // 특정 아티스트 선택 시 해당 아티스트 랭킹 조회
                if (artist != null && !artist.isEmpty()) {
                    fanRankings = fanChallengeRecordRepository.findTopByArtist(artist, PageRequest.of(0, 50));
                }
                model.addAttribute("fanArtistStats", fanArtistStats);
                model.addAttribute("selectedArtist", artist);
                break;
            case "fanPerfect":
                // 퍼펙트 클리어 기록만 조회
                Page<FanChallengeRecord> perfectPage = fanChallengeRecordRepository.findByIsPerfectClear(true, PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "achievedAt")));
                fanRankings = perfectPage.getContent();
                break;
            default:
                memberRankings = memberService.getGuessRankingByScore(50);
        }

        model.addAttribute("multiTierDistribution", multiTierDistribution);
        model.addAttribute("totalMultiPlayers", totalMultiPlayers);
        model.addAttribute("memberRankings", memberRankings);
        model.addAttribute("fanRankings", fanRankings);
        model.addAttribute("rankType", rankType);

        return "admin/history/fragments/ranking";
    }

    /**
     * 기존 랭킹 URL 리다이렉트 (하위 호환)
     */
    @GetMapping("/ranking")
    public String rankingRedirect() {
        return "redirect:/admin/history?tab=ranking";
    }
}
