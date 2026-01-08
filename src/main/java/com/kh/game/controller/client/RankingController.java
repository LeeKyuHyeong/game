package com.kh.game.controller.client;

import com.kh.game.entity.Member;
import com.kh.game.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class RankingController {

    private final MemberService memberService;

    // 랭킹 페이지
    @GetMapping("/ranking")
    public String rankingPage(Model model) {
        model.addAttribute("scoreRanking", memberService.getTopRankingByScore(20));
        model.addAttribute("accuracyRanking", memberService.getTopRankingByAccuracy(20));
        model.addAttribute("gamesRanking", memberService.getTopRankingByGames(20));
        return "client/ranking";
    }

    // 랭킹 API
    @GetMapping("/api/ranking")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getRanking(
            @RequestParam(defaultValue = "score") String type,
            @RequestParam(defaultValue = "10") int limit) {

        List<Member> members;

        switch (type) {
            case "accuracy":
                members = memberService.getTopRankingByAccuracy(limit);
                break;
            case "games":
                members = memberService.getTopRankingByGames(limit);
                break;
            case "score":
            default:
                members = memberService.getTopRankingByScore(limit);
                break;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Member member : members) {
            Map<String, Object> memberInfo = new HashMap<>();
            memberInfo.put("id", member.getId());
            memberInfo.put("nickname", member.getNickname());
            memberInfo.put("totalScore", member.getTotalScore());
            memberInfo.put("totalGames", member.getTotalGames());
            memberInfo.put("totalCorrect", member.getTotalCorrect());
            memberInfo.put("totalRounds", member.getTotalRounds());
            memberInfo.put("accuracyRate", member.getAccuracyRate());
            memberInfo.put("averageScore", member.getAverageScore());
            result.add(memberInfo);
        }

        return ResponseEntity.ok(result);
    }
}