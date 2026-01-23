package com.kh.game.controller.admin;

import com.kh.game.entity.GameRoom;
import com.kh.game.repository.GameRoomChatRepository;
import com.kh.game.repository.GameRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 멀티게임 관리 통합 페이지 컨트롤러
 * 방 관리 + 채팅 내역을 하나의 페이지에서 탭으로 관리
 */
@Controller
@RequestMapping("/admin/multi")
@RequiredArgsConstructor
public class AdminMultiController {

    private final GameRoomRepository gameRoomRepository;
    private final GameRoomChatRepository gameRoomChatRepository;

    /**
     * 기존 URL → 통합 게임 관리 페이지로 리다이렉트
     */
    @GetMapping({"", "/"})
    public String redirectToGame(@RequestParam(defaultValue = "multi") String tab) {
        return "redirect:/admin/game?tab=" + tab;
    }

    /**
     * AJAX 로딩용 멀티게임 콘텐츠 (fragment)
     */
    @GetMapping("/content")
    public String multiContent(@RequestParam(defaultValue = "room") String subTab, Model model) {
        model.addAttribute("activeSubTab", subTab);
        model.addAttribute("menu", "game");

        // 방 통계
        long totalRooms = gameRoomRepository.count();
        long waitingCount = gameRoomRepository.countByStatus(GameRoom.RoomStatus.WAITING);
        long playingCount = gameRoomRepository.countByStatus(GameRoom.RoomStatus.PLAYING);
        long finishedCount = gameRoomRepository.countByStatus(GameRoom.RoomStatus.FINISHED);

        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("waitingCount", waitingCount);
        model.addAttribute("playingCount", playingCount);
        model.addAttribute("finishedCount", finishedCount);

        // 채팅 통계
        long totalChats = gameRoomChatRepository.count();
        long todayChats = gameRoomChatRepository.countTodayChats();

        model.addAttribute("totalChats", totalChats);
        model.addAttribute("todayChats", todayChats);

        return "admin/multi/fragments/multi";
    }
}
