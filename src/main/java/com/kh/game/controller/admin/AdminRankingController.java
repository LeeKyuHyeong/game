package com.kh.game.controller.admin;

import com.kh.game.entity.FanChallengeRecord;
import com.kh.game.entity.Member;
import com.kh.game.entity.MultiTier;
import com.kh.game.repository.FanChallengeRecordRepository;
import com.kh.game.repository.MemberRepository;
import com.kh.game.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

/**
 * 랭킹 관리 통합 페이지 컨트롤러
 * 솔로, 멀티, 레트로, 챌린지 랭킹을 하나의 페이지에서 탭으로 관리
 */
@Controller
@RequestMapping("/admin/ranking")
@RequiredArgsConstructor
public class AdminRankingController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final FanChallengeRecordRepository fanChallengeRecordRepository;

    /**
     * 통합 랭킹 관리 페이지
     */
    @GetMapping({"", "/"})
    public String rankingIndex(@RequestParam(defaultValue = "solo") String tab, Model model) {
        model.addAttribute("activeTab", tab);
        model.addAttribute("menu", "ranking");
        return "admin/ranking/index";
    }

    /**
     * AJAX 로딩용 랭킹 콘텐츠 (fragment)
     */
    @GetMapping("/content")
    public String rankingContent(
            @RequestParam(defaultValue = "guess") String rankType,
            @RequestParam(defaultValue = "solo") String tab,
            @RequestParam(required = false) String artist,
            Model model) {

        model.addAttribute("tab", tab);

        // 멀티게임 LP 티어 분포
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
            case "retro":
                memberRankings = memberService.getRetroRankingByScore(50);
                break;
            case "retroWeekly":
                memberRankings = memberService.getWeeklyRetroRankingByScore(50);
                break;
            case "retroBest":
                memberRankings = memberService.getRetroBest30Ranking(50);
                break;
            case "weeklyMulti":
                memberRankings = memberService.getWeeklyMultiRankingByScore(50);
                break;
            case "fan":
                // 인기 아티스트 목록 조회
                List<Object[]> popularArtists = fanChallengeRecordRepository.findPopularArtists(PageRequest.of(0, 20));
                for (Object[] row : popularArtists) {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("artist", row[0]);
                    stat.put("challengeCount", row[1]);
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
                Page<FanChallengeRecord> perfectPage = fanChallengeRecordRepository.findByIsPerfectClear(
                        true, PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "achievedAt")));
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

        return "admin/ranking/fragments/ranking";
    }
}
