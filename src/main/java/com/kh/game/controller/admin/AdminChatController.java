package com.kh.game.controller.admin;

import com.kh.game.entity.GameRoom;
import com.kh.game.entity.GameRoomChat;
import com.kh.game.entity.Member;
import com.kh.game.repository.GameRoomChatRepository;
import com.kh.game.repository.GameRoomRepository;
import com.kh.game.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/chat")
@RequiredArgsConstructor
public class AdminChatController {

    private final GameRoomChatRepository chatRepository;
    private final GameRoomRepository gameRoomRepository;
    private final MemberRepository memberRepository;

    /**
     * 레거시 URL → 통합 페이지로 리다이렉트
     */
    @GetMapping
    public String redirectToMulti() {
        return "redirect:/admin/multi?tab=chat";
    }

    /**
     * AJAX 로딩용 채팅 내역 콘텐츠 (fragment)
     */
    @GetMapping("/content")
    public String listContent(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "50") int size,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String roomCode,
                              @RequestParam(required = false) String nickname,
                              @RequestParam(required = false) String messageType,
                              @RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<GameRoomChat> chatPage = chatRepository.findAllWithFilters(
                keyword, roomCode, nickname, messageType,
                parseDate(startDate, true), parseDate(endDate, false),
                pageable);

        // 오늘 채팅 수
        long todayChats = chatRepository.countTodayChats();

        model.addAttribute("chats", chatPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", chatPage.getTotalPages());
        model.addAttribute("totalItems", chatPage.getTotalElements());
        model.addAttribute("todayChats", todayChats);
        model.addAttribute("keyword", keyword);
        model.addAttribute("roomCode", roomCode);
        model.addAttribute("nickname", nickname);
        model.addAttribute("messageType", messageType);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin/multi/fragments/chat";
    }

    /**
     * 채팅 삭제
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            chatRepository.deleteById(id);
            result.put("success", true);
            result.put("message", "채팅이 삭제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 선택 삭제
     */
    @PostMapping("/delete-selected")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteSelected(@RequestBody List<Long> ids) {
        Map<String, Object> result = new HashMap<>();
        try {
            chatRepository.deleteAllById(ids);
            result.put("success", true);
            result.put("message", ids.size() + "개의 채팅이 삭제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 특정 회원의 모든 채팅 삭제
     */
    @PostMapping("/delete-by-member/{memberId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteByMember(@PathVariable Long memberId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

            chatRepository.deleteByMember(member);
            result.put("success", true);
            result.put("message", "해당 회원의 모든 채팅이 삭제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 채팅 통계
     */
    @GetMapping("/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("success", true);
            result.put("totalChats", chatRepository.count());
            result.put("todayChats", chatRepository.countTodayChats());
            result.put("chatTypeStats", chatRepository.countByMessageType());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "통계 조회 중 오류가 발생했습니다.");
        }
        return ResponseEntity.ok(result);
    }

    private LocalDateTime parseDate(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return isStart ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
        } catch (Exception e) {
            return null;
        }
    }
}