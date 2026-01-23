package com.kh.game.controller.admin;

import com.kh.game.entity.GameRoom;
import com.kh.game.entity.Genre;
import com.kh.game.repository.FanChallengeRecordRepository;
import com.kh.game.repository.GameRoomChatRepository;
import com.kh.game.repository.GameRoomRepository;
import com.kh.game.repository.GenreChallengeRecordRepository;
import com.kh.game.service.GameSessionService;
import com.kh.game.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 게임 관리 통합 페이지 컨트롤러
 * 게임 이력, 멀티게임, 챌린지를 하나의 페이지에서 탭으로 관리
 */
@Controller
@RequestMapping("/admin/game")
@RequiredArgsConstructor
public class AdminGameManagementController {

    private final GameSessionService gameSessionService;
    private final GameRoomRepository gameRoomRepository;
    private final GameRoomChatRepository gameRoomChatRepository;
    private final FanChallengeRecordRepository fanChallengeRecordRepository;
    private final GenreChallengeRecordRepository genreChallengeRecordRepository;
    private final GenreService genreService;

    /**
     * 통합 게임 관리 페이지
     */
    @GetMapping({"", "/"})
    public String gameIndex(@RequestParam(defaultValue = "history") String tab, Model model) {
        model.addAttribute("activeTab", tab);
        model.addAttribute("menu", "game");

        // 게임 이력 통계
        long todayGames = gameSessionService.getTodayGameCount();
        double avgScore = gameSessionService.getAverageScore();
        long totalGames = gameSessionService.countAll();
        model.addAttribute("todayGames", todayGames);
        model.addAttribute("avgScore", avgScore);
        model.addAttribute("totalGames", totalGames);

        // 멀티게임 통계
        long totalRooms = gameRoomRepository.count();
        long waitingCount = gameRoomRepository.countByStatus(GameRoom.RoomStatus.WAITING);
        long playingCount = gameRoomRepository.countByStatus(GameRoom.RoomStatus.PLAYING);
        long finishedCount = gameRoomRepository.countByStatus(GameRoom.RoomStatus.FINISHED);
        long totalChats = gameRoomChatRepository.count();
        long todayChats = gameRoomChatRepository.countTodayChats();

        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("waitingCount", waitingCount);
        model.addAttribute("playingCount", playingCount);
        model.addAttribute("finishedCount", finishedCount);
        model.addAttribute("totalChats", totalChats);
        model.addAttribute("todayChats", todayChats);

        // 챌린지 통계
        long fanTotalCount = fanChallengeRecordRepository.count();
        long fanPerfectCount = fanChallengeRecordRepository.countPerfectClears();
        long fanArtistCount = fanChallengeRecordRepository.countDistinctArtists();
        long fanTodayCount = fanChallengeRecordRepository.countTodayRecords();

        model.addAttribute("fanTotalCount", fanTotalCount);
        model.addAttribute("fanPerfectCount", fanPerfectCount);
        model.addAttribute("fanArtistCount", fanArtistCount);
        model.addAttribute("fanTodayCount", fanTodayCount);

        long genreTotalCount = genreChallengeRecordRepository.count();
        long genreActiveGenreCount = genreChallengeRecordRepository.countDistinctGenres();
        long genreTodayCount = genreChallengeRecordRepository.countTodayRecords();

        model.addAttribute("genreTotalCount", genreTotalCount);
        model.addAttribute("genreActiveGenreCount", genreActiveGenreCount);
        model.addAttribute("genreTodayCount", genreTodayCount);

        // 장르 목록 (드롭다운용)
        List<Genre> genres = genreService.findActiveGenres();
        model.addAttribute("genres", genres);

        return "admin/game/index";
    }
}
