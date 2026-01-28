package com.kh.game.controller.admin;

import com.kh.game.batch.MonthlyRankingResetBatch;
import com.kh.game.batch.RankingSnapshotBatch;
import com.kh.game.batch.WeeklyRankingResetBatch;
import com.kh.game.entity.*;
import com.kh.game.entity.RankingHistory.PeriodType;
import com.kh.game.repository.FanChallengeRecordRepository;
import com.kh.game.repository.GenreChallengeRecordRepository;
import com.kh.game.repository.MemberRepository;
import com.kh.game.repository.RankingHistoryRepository;
import com.kh.game.service.GameSessionService;
import com.kh.game.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private final GenreChallengeRecordRepository genreChallengeRecordRepository;
    private final GameSessionService gameSessionService;
    private final RankingSnapshotBatch rankingSnapshotBatch;
    private final WeeklyRankingResetBatch weeklyRankingResetBatch;
    private final MonthlyRankingResetBatch monthlyRankingResetBatch;
    private final RankingHistoryRepository rankingHistoryRepository;

    /**
     * 통합 랭킹 관리 페이지
     */
    @GetMapping({"", "/"})
    public String rankingIndex(@RequestParam(defaultValue = "multi") String tab, Model model) {
        model.addAttribute("activeTab", tab);
        model.addAttribute("menu", "ranking");
        return "admin/ranking/index";
    }

    /**
     * AJAX 로딩용 랭킹 콘텐츠 (fragment)
     */
    @GetMapping("/content")
    public String rankingContent(
            @RequestParam(required = false) String rankType,
            @RequestParam(defaultValue = "multi") String tab,
            @RequestParam(required = false) String artist,
            Model model) {

        // 탭별 기본 rankType 설정
        if (rankType == null || rankType.isEmpty()) {
            switch (tab) {
                case "multi": rankType = "multi"; break;
                case "challenge30": rankType = "weeklyBest30"; break;
                case "artist": rankType = "fan"; break;
                case "genre": rankType = "genreTotal"; break;
                case "retro": rankType = "retro"; break;
                default: rankType = "multi";
            }
        }

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
        List<Map<String, Object>> best30Rankings = new ArrayList<>();

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
            case "weeklyBest30":
                best30Rankings = gameSessionService.getWeeklyBest30RankingByDuration(50);
                break;
            case "monthlyBest30":
                best30Rankings = gameSessionService.getMonthlyBest30RankingByDuration(50);
                break;
            case "hallOfFame":
                best30Rankings = gameSessionService.getAllTimeBest30RankingByDuration(50);
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
            case "genreTotal":
                // 장르 챌린지 - 총 정답수 랭킹
                List<Object[]> totalCorrectRanking = genreChallengeRecordRepository.findTotalCorrectCountRanking(PageRequest.of(0, 50));
                model.addAttribute("genreRankings", convertGenreRankingToMap(totalCorrectRanking, "totalCorrect"));
                break;
            case "genreCount":
                // 장르 챌린지 - 도전 장르수 랭킹
                List<Object[]> genreCountRanking = genreChallengeRecordRepository.findGenreClearCountRanking(PageRequest.of(0, 50));
                model.addAttribute("genreRankings", convertGenreRankingToMap(genreCountRanking, "genreCount"));
                break;
            case "genreCombo":
                // 장르 챌린지 - 최대 콤보 랭킹
                List<Object[]> maxComboRanking = genreChallengeRecordRepository.findMaxComboRanking(PageRequest.of(0, 50));
                model.addAttribute("genreRankings", convertGenreRankingToMap(maxComboRanking, "maxCombo"));
                break;
            default:
                memberRankings = memberService.getMultiTierRanking(50);
        }

        model.addAttribute("multiTierDistribution", multiTierDistribution);
        model.addAttribute("totalMultiPlayers", totalMultiPlayers);
        model.addAttribute("memberRankings", memberRankings);
        model.addAttribute("fanRankings", fanRankings);
        model.addAttribute("best30Rankings", best30Rankings);
        model.addAttribute("rankType", rankType);

        return "admin/ranking/fragments/ranking";
    }

    /**
     * 주간 랭킹 수동 리셋
     */
    @PostMapping("/reset/weekly")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resetWeekly(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Member member = (Member) session.getAttribute("member");
        if (member == null || !Member.MemberRole.ADMIN.equals(member.getRole())) {
            result.put("success", false);
            result.put("message", "관리자 권한이 필요합니다.");
            return ResponseEntity.status(403).body(result);
        }

        try {
            int snapshotCount = rankingSnapshotBatch.executeWeekly(BatchExecutionHistory.ExecutionType.MANUAL);
            int resetCount = weeklyRankingResetBatch.execute(BatchExecutionHistory.ExecutionType.MANUAL);
            result.put("success", true);
            result.put("message", String.format("주간 랭킹 리셋 완료 (스냅샷: %d건, 리셋: %d명)", snapshotCount, resetCount));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "주간 리셋 중 오류: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 월간 랭킹 수동 리셋
     */
    @PostMapping("/reset/monthly")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resetMonthly(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Member member = (Member) session.getAttribute("member");
        if (member == null || !Member.MemberRole.ADMIN.equals(member.getRole())) {
            result.put("success", false);
            result.put("message", "관리자 권한이 필요합니다.");
            return ResponseEntity.status(403).body(result);
        }

        try {
            int snapshotCount = rankingSnapshotBatch.executeMonthly(BatchExecutionHistory.ExecutionType.MANUAL);
            int resetCount = monthlyRankingResetBatch.execute(BatchExecutionHistory.ExecutionType.MANUAL);
            result.put("success", true);
            result.put("message", String.format("월간 랭킹 리셋 완료 (스냅샷: %d건, 리셋: %d명)", snapshotCount, resetCount));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "월간 리셋 중 오류: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 조회 가능한 기간 목록
     */
    @GetMapping("/history/periods")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getHistoryPeriods() {
        List<Object[]> periods = rankingHistoryRepository.findDistinctPeriods();
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Object[] row : periods) {
            Map<String, Object> period = new HashMap<>();
            PeriodType periodType = (PeriodType) row[0];
            LocalDate start = (LocalDate) row[1];
            LocalDate end = (LocalDate) row[2];

            period.put("periodType", periodType.name());
            period.put("periodTypeDisplay", periodType.getDisplayName());
            period.put("periodStart", start.format(fmt));
            period.put("periodEnd", end.format(fmt));
            period.put("label", periodType.getDisplayName() + " " + start.format(fmt) + " ~ " + end.format(fmt));
            result.add(period);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 특정 기간의 스냅샷 데이터
     */
    @GetMapping("/history/content")
    public String getHistoryContent(@RequestParam String periodStart,
                                     @RequestParam String periodEnd,
                                     Model model) {
        LocalDate start = LocalDate.parse(periodStart);
        LocalDate end = LocalDate.parse(periodEnd);

        List<RankingHistory> historyList = rankingHistoryRepository.findByPeriod(start, end);

        // 랭킹 타입별로 그룹화
        Map<String, List<RankingHistory>> groupedHistory = new LinkedHashMap<>();
        for (RankingHistory rh : historyList) {
            String typeName = rh.getRankingType().getDisplayName();
            groupedHistory.computeIfAbsent(typeName, k -> new ArrayList<>()).add(rh);
        }

        model.addAttribute("groupedHistory", groupedHistory);
        model.addAttribute("periodStart", periodStart);
        model.addAttribute("periodEnd", periodEnd);
        model.addAttribute("isHistoryView", true);

        return "admin/ranking/fragments/ranking-history";
    }

    /**
     * 장르 챌린지 랭킹 데이터를 Map으로 변환 (닉네임 포함)
     */
    private List<Map<String, Object>> convertGenreRankingToMap(List<Object[]> rankings, String valueKey) {
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rankings) {
            Long memberId = (Long) row[0];
            Object value = row[1];

            Member member = memberService.findById(memberId).orElse(null);
            if (member != null) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("rank", rank++);
                entry.put("memberId", memberId);
                entry.put("nickname", member.getNickname());
                entry.put(valueKey, value);
                result.add(entry);
            }
        }
        return result;
    }
}
