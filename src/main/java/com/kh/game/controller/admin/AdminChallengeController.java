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
 * 챌린지 기록 페이지 컨트롤러
 * 팬 챌린지 + 장르 챌린지 기록을 하나의 페이지에서 탭으로 관리 (기록 조회/관리용)
 */
@Controller
@RequestMapping("/admin/challenge")
@RequiredArgsConstructor
public class AdminChallengeController {

    private final FanChallengeRecordRepository fanChallengeRecordRepository;
    private final GenreChallengeRecordRepository genreChallengeRecordRepository;
    private final GenreService genreService;

    /**
     * 기존 URL → 통합 게임 관리 페이지로 리다이렉트
     */
    @GetMapping({"", "/"})
    public String redirectToGame(@RequestParam(defaultValue = "challenge") String tab) {
        return "redirect:/admin/game?tab=" + tab;
    }

    /**
     * AJAX 로딩용 챌린지 콘텐츠 (fragment)
     */
    @GetMapping("/content")
    public String challengeContent(@RequestParam(defaultValue = "fan") String subTab, Model model) {
        model.addAttribute("activeSubTab", subTab);
        model.addAttribute("menu", "game");

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

        return "admin/challenge/fragments/challenge";
    }
}
