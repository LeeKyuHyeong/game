package com.kh.game.controller.admin;

import com.kh.game.entity.Genre;
import com.kh.game.repository.FanChallengeRecordRepository;
import com.kh.game.repository.GenreChallengeRecordRepository;
import com.kh.game.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 챌린지 관리 통합 페이지 컨트롤러
 * 팬 챌린지 + 장르 챌린지를 하나의 페이지에서 탭으로 관리
 */
@Controller
@RequestMapping("/admin/challenge")
@RequiredArgsConstructor
public class AdminChallengeController {

    private final FanChallengeRecordRepository fanChallengeRecordRepository;
    private final GenreChallengeRecordRepository genreChallengeRecordRepository;
    private final GenreService genreService;

    /**
     * 통합 챌린지 관리 페이지
     */
    @GetMapping({"", "/"})
    public String challengeIndex(@RequestParam(defaultValue = "fan") String tab, Model model) {
        model.addAttribute("activeTab", tab);
        model.addAttribute("menu", "challenge");

        // 팬 챌린지 통계
        long fanTotalCount = fanChallengeRecordRepository.count();
        long fanPerfectCount = fanChallengeRecordRepository.countPerfectClears();
        long fanArtistCount = fanChallengeRecordRepository.countDistinctArtists();
        long fanTodayCount = fanChallengeRecordRepository.countTodayRecords();

        model.addAttribute("fanTotalCount", fanTotalCount);
        model.addAttribute("fanPerfectCount", fanPerfectCount);
        model.addAttribute("fanArtistCount", fanArtistCount);
        model.addAttribute("fanTodayCount", fanTodayCount);

        // 장르 챌린지 통계
        long genreTotalCount = genreChallengeRecordRepository.count();
        long genreActiveGenreCount = genreChallengeRecordRepository.countDistinctGenres();
        long genreTodayCount = genreChallengeRecordRepository.countTodayRecords();

        model.addAttribute("genreTotalCount", genreTotalCount);
        model.addAttribute("genreActiveGenreCount", genreActiveGenreCount);
        model.addAttribute("genreTodayCount", genreTodayCount);

        // 장르 목록 (드롭다운용)
        List<Genre> genres = genreService.findActiveGenres();
        model.addAttribute("genres", genres);

        return "admin/challenge/index";
    }
}
