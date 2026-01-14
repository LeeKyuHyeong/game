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
        // 내가맞추기 랭킹
        model.addAttribute("guessScoreRanking", memberService.getGuessRankingByScore(20));
        model.addAttribute("guessAccuracyRanking", memberService.getGuessRankingByAccuracy(20));
        model.addAttribute("guessGamesRanking", memberService.getGuessRankingByGames(20));

        // 멀티게임 랭킹
        model.addAttribute("multiScoreRanking", memberService.getMultiRankingByScore(20));
        model.addAttribute("multiAccuracyRanking", memberService.getMultiRankingByAccuracy(20));
        model.addAttribute("multiGamesRanking", memberService.getMultiRankingByGames(20));

        // 주간 랭킹
        model.addAttribute("weeklyGuessRanking", memberService.getWeeklyGuessRankingByScore(20));
        model.addAttribute("weeklyMultiRanking", memberService.getWeeklyMultiRankingByScore(20));

        // 최고 기록 랭킹
        model.addAttribute("guessBestRanking", memberService.getGuessBestScoreRanking(20));
        model.addAttribute("multiBestRanking", memberService.getMultiBestScoreRanking(20));

        return "client/ranking";
    }

    // 랭킹 API (모드별, 기간별 지원)
    @GetMapping("/api/ranking")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getRanking(
            @RequestParam(defaultValue = "guess") String mode,
            @RequestParam(defaultValue = "score") String type,
            @RequestParam(defaultValue = "all") String period,
            @RequestParam(defaultValue = "10") int limit) {

        List<Member> members;

        if ("multi".equals(mode)) {
            // 멀티게임 랭킹
            members = getMultiRankingMembers(type, period, limit);
            return ResponseEntity.ok(toMultiRankingResponse(members, period));
        } else {
            // 내가맞추기 랭킹 (기본값)
            members = getGuessRankingMembers(type, period, limit);
            return ResponseEntity.ok(toGuessRankingResponse(members, period));
        }
    }

    // 내가맞추기 랭킹 멤버 조회
    private List<Member> getGuessRankingMembers(String type, String period, int limit) {
        if ("weekly".equals(period)) {
            return memberService.getWeeklyGuessRankingByScore(limit);
        } else if ("best".equals(period)) {
            return memberService.getGuessBestScoreRanking(limit);
        } else {
            // all (전체) - 6가지 랭킹 타입
            switch (type) {
                case "accuracy":
                    return memberService.getGuessRankingByAccuracy(limit);
                case "avgScore":
                    return memberService.getGuessRankingByAvgScore(limit);
                case "correct":
                    return memberService.getGuessRankingByCorrect(limit);
                case "games":
                    return memberService.getGuessRankingByGames(limit);
                case "rounds":
                    return memberService.getGuessRankingByRounds(limit);
                case "score":
                default:
                    return memberService.getGuessRankingByScore(limit);
            }
        }
    }

    // 멀티게임 랭킹 멤버 조회
    private List<Member> getMultiRankingMembers(String type, String period, int limit) {
        if ("weekly".equals(period)) {
            return memberService.getWeeklyMultiRankingByScore(limit);
        } else if ("best".equals(period)) {
            return memberService.getMultiBestScoreRanking(limit);
        } else if ("tier".equals(period)) {
            // LP 티어 랭킹
            return memberService.getMultiTierRanking(limit);
        } else if ("wins".equals(period)) {
            // 1등 횟수 랭킹
            return memberService.getMultiWinsRanking(limit);
        } else {
            // all (전체)
            switch (type) {
                case "accuracy":
                    return memberService.getMultiRankingByAccuracy(limit);
                case "games":
                    return memberService.getMultiRankingByGames(limit);
                case "score":
                default:
                    return memberService.getMultiRankingByScore(limit);
            }
        }
    }

    // 내가맞추기 랭킹 응답 변환
    private List<Map<String, Object>> toGuessRankingResponse(List<Member> members, String period) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Member member : members) {
            Map<String, Object> memberInfo = new HashMap<>();
            memberInfo.put("id", member.getId());
            memberInfo.put("nickname", member.getNickname());

            // 기간에 따른 점수 표시
            if ("weekly".equals(period)) {
                memberInfo.put("totalScore", member.getWeeklyGuessScore() != null ? member.getWeeklyGuessScore() : 0);
                memberInfo.put("totalGames", member.getWeeklyGuessGames() != null ? member.getWeeklyGuessGames() : 0);
                memberInfo.put("totalCorrect", member.getWeeklyGuessCorrect() != null ? member.getWeeklyGuessCorrect() : 0);
                memberInfo.put("totalRounds", member.getWeeklyGuessRounds() != null ? member.getWeeklyGuessRounds() : 0);
                memberInfo.put("accuracyRate", member.getWeeklyGuessAccuracyRate());
            } else if ("best".equals(period)) {
                memberInfo.put("totalScore", member.getBestGuessScore() != null ? member.getBestGuessScore() : 0);
                memberInfo.put("accuracyRate", member.getBestGuessAccuracy() != null ? member.getBestGuessAccuracy() : 0.0);
                memberInfo.put("achievedAt", member.getBestGuessAt());
                // 최고 기록 모드에서는 게임수/라운드수 없음
                memberInfo.put("totalGames", 1);
                memberInfo.put("totalCorrect", 0);
                memberInfo.put("totalRounds", 0);
            } else {
                memberInfo.put("totalScore", member.getGuessScore() != null ? member.getGuessScore() : 0);
                memberInfo.put("totalGames", member.getGuessGames() != null ? member.getGuessGames() : 0);
                memberInfo.put("totalCorrect", member.getGuessCorrect() != null ? member.getGuessCorrect() : 0);
                memberInfo.put("totalRounds", member.getGuessRounds() != null ? member.getGuessRounds() : 0);
                memberInfo.put("accuracyRate", member.getGuessAccuracyRate());
                memberInfo.put("averageScore", member.getGuessAverageScore());
            }

            // 티어 정보 추가
            addTierInfo(memberInfo, member);

            // 뱃지 정보 추가
            addBadgeInfo(memberInfo, member);

            result.add(memberInfo);
        }
        return result;
    }

    // 멀티게임 랭킹 응답 변환
    private List<Map<String, Object>> toMultiRankingResponse(List<Member> members, String period) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Member member : members) {
            Map<String, Object> memberInfo = new HashMap<>();
            memberInfo.put("id", member.getId());
            memberInfo.put("nickname", member.getNickname());

            // 기간에 따른 점수 표시
            if ("weekly".equals(period)) {
                memberInfo.put("totalScore", member.getWeeklyMultiScore() != null ? member.getWeeklyMultiScore() : 0);
                memberInfo.put("totalGames", member.getWeeklyMultiGames() != null ? member.getWeeklyMultiGames() : 0);
                memberInfo.put("totalCorrect", member.getWeeklyMultiCorrect() != null ? member.getWeeklyMultiCorrect() : 0);
                memberInfo.put("totalRounds", member.getWeeklyMultiRounds() != null ? member.getWeeklyMultiRounds() : 0);
                memberInfo.put("accuracyRate", member.getWeeklyMultiAccuracyRate());
            } else if ("best".equals(period)) {
                memberInfo.put("totalScore", member.getBestMultiScore() != null ? member.getBestMultiScore() : 0);
                memberInfo.put("accuracyRate", member.getBestMultiAccuracy() != null ? member.getBestMultiAccuracy() : 0.0);
                memberInfo.put("achievedAt", member.getBestMultiAt());
                // 최고 기록 모드에서는 게임수/라운드수 없음
                memberInfo.put("totalGames", 1);
                memberInfo.put("totalCorrect", 0);
                memberInfo.put("totalRounds", 0);
            } else if ("tier".equals(period)) {
                // LP 티어 랭킹
                memberInfo.put("multiLp", member.getMultiLp() != null ? member.getMultiLp() : 0);
                memberInfo.put("multiWins", member.getMultiWins() != null ? member.getMultiWins() : 0);
                memberInfo.put("multiTop3", member.getMultiTop3() != null ? member.getMultiTop3() : 0);
                memberInfo.put("totalGames", member.getMultiGames() != null ? member.getMultiGames() : 0);
                memberInfo.put("totalScore", 0);  // 티어 랭킹에서는 점수 대신 LP 표시
                memberInfo.put("accuracyRate", member.getMultiAccuracyRate());
            } else if ("wins".equals(period)) {
                // 1등 횟수 랭킹
                memberInfo.put("multiWins", member.getMultiWins() != null ? member.getMultiWins() : 0);
                memberInfo.put("totalGames", member.getMultiGames() != null ? member.getMultiGames() : 0);
                memberInfo.put("totalScore", member.getMultiWins() != null ? member.getMultiWins() : 0);  // 1등 횟수를 점수로 표시
                memberInfo.put("accuracyRate", member.getMultiAccuracyRate());
            } else {
                memberInfo.put("totalScore", member.getMultiScore() != null ? member.getMultiScore() : 0);
                memberInfo.put("totalGames", member.getMultiGames() != null ? member.getMultiGames() : 0);
                memberInfo.put("totalCorrect", member.getMultiCorrect() != null ? member.getMultiCorrect() : 0);
                memberInfo.put("totalRounds", member.getMultiRounds() != null ? member.getMultiRounds() : 0);
                memberInfo.put("accuracyRate", member.getMultiAccuracyRate());
                memberInfo.put("averageScore", member.getMultiAverageScore());
            }

            // 티어 정보 추가 (통합 + 멀티)
            addTierInfo(memberInfo, member);
            addMultiTierInfo(memberInfo, member);

            // 뱃지 정보 추가
            addBadgeInfo(memberInfo, member);

            result.add(memberInfo);
        }
        return result;
    }

    // 뱃지 정보 추가
    private void addBadgeInfo(Map<String, Object> memberInfo, Member member) {
        if (member.getSelectedBadge() != null) {
            memberInfo.put("badgeEmoji", member.getSelectedBadgeEmoji());
            memberInfo.put("badgeName", member.getSelectedBadgeName());
        } else {
            memberInfo.put("badgeEmoji", null);
            memberInfo.put("badgeName", null);
        }
    }

    // 통합 티어 정보 추가
    private void addTierInfo(Map<String, Object> memberInfo, Member member) {
        Member.MemberTier tier = member.getTier() != null ? member.getTier() : Member.MemberTier.BRONZE;
        memberInfo.put("tier", tier.name());
        memberInfo.put("tierDisplayName", tier.getDisplayName());
        memberInfo.put("tierColor", tier.getColor());
    }

    // 멀티게임 LP 티어 정보 추가
    private void addMultiTierInfo(Map<String, Object> memberInfo, Member member) {
        memberInfo.put("multiTier", member.getMultiTier() != null ? member.getMultiTier().name() : "BRONZE");
        memberInfo.put("multiTierDisplayName", member.getMultiTierDisplayName());
        memberInfo.put("multiTierColor", member.getMultiTierColor());
        memberInfo.put("multiLp", member.getMultiLp() != null ? member.getMultiLp() : 0);
    }

    // 내 순위 API
    @GetMapping("/api/ranking/my")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMyRanking(
            @SessionAttribute(value = "memberId", required = false) Long memberId) {

        Map<String, Object> result = new HashMap<>();

        if (memberId == null) {
            result.put("loggedIn", false);
            return ResponseEntity.ok(result);
        }

        Member member = memberService.findById(memberId).orElse(null);
        if (member == null) {
            result.put("loggedIn", false);
            return ResponseEntity.ok(result);
        }

        result.put("loggedIn", true);
        result.put("nickname", member.getNickname());

        // 티어 정보
        Member.MemberTier tier = member.getTier() != null ? member.getTier() : Member.MemberTier.BRONZE;
        result.put("tier", tier.name());
        result.put("tierDisplayName", tier.getDisplayName());
        result.put("tierColor", tier.getColor());

        // 내가맞추기 순위
        int guessScore = member.getGuessScore() != null ? member.getGuessScore() : 0;
        int guessGames = member.getGuessGames() != null ? member.getGuessGames() : 0;

        if (guessGames > 0) {
            long rank = memberService.getMyGuessRank(guessScore);
            long total = memberService.getGuessParticipantCount();
            result.put("guessRank", rank);
            result.put("guessTotal", total);
            result.put("guessScore", guessScore);
            result.put("guessGames", guessGames);
        } else {
            result.put("guessRank", 0);
            result.put("guessTotal", 0);
            result.put("guessScore", 0);
            result.put("guessGames", 0);
        }

        return ResponseEntity.ok(result);
    }
}
