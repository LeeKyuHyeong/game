package com.kh.game.controller.admin;

import com.kh.game.service.GenreService;
import com.kh.game.service.SongService;
import com.kh.game.service.SongPopularityVoteService;
import com.kh.game.service.SongReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 콘텐츠 관리 통합 페이지 컨트롤러
 * 노래, 정답, 장르, 대중성 투표를 하나의 페이지에서 탭으로 관리
 */
@Controller
@RequestMapping("/admin/content")
@RequiredArgsConstructor
public class AdminContentController {

    private final SongService songService;
    private final GenreService genreService;
    private final SongPopularityVoteService voteService;
    private final SongReportService songReportService;

    /**
     * 통합 콘텐츠 관리 페이지
     */
    @GetMapping({"", "/"})
    public String contentIndex(@RequestParam(defaultValue = "song") String tab, Model model) {
        model.addAttribute("activeTab", tab);
        model.addAttribute("menu", "content");

        // 노래 통계
        long totalSongs = songService.count();
        long activeSongs = songService.countByUseYn("Y");
        long inactiveSongs = songService.countByUseYn("N");
        model.addAttribute("totalSongs", totalSongs);
        model.addAttribute("activeSongs", activeSongs);
        model.addAttribute("inactiveSongs", inactiveSongs);

        // 장르 통계
        long totalGenres = genreService.count();
        long activeGenres = genreService.countActive();
        model.addAttribute("totalGenres", totalGenres);
        model.addAttribute("activeGenres", activeGenres);

        // 대중성 투표 통계
        Map<String, Object> voteStats = voteService.getTotalStats();
        model.addAttribute("totalVotes", voteStats.get("totalVotes"));
        model.addAttribute("todayVotes", voteStats.get("todayVotes"));
        model.addAttribute("avgRating", voteStats.get("avgRating"));

        // 노래 폼에 필요한 장르 목록
        model.addAttribute("genres", genreService.findActiveGenres());

        // 곡 신고 통계
        long pendingReports = songReportService.getPendingCount();
        model.addAttribute("pendingReports", pendingReports);

        return "admin/content/index";
    }
}
