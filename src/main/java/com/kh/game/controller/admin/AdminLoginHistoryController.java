package com.kh.game.controller.admin;

import com.kh.game.entity.MemberLoginHistory;
import com.kh.game.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin/login-history")
@RequiredArgsConstructor
public class AdminLoginHistoryController {

    private final MemberService memberService;

    /**
     * 레거시 URL → 통합 페이지로 리다이렉트
     */
    @GetMapping
    public String redirectToMember() {
        return "redirect:/admin/member?tab=login";
    }

    /**
     * AJAX 로딩용 로그인 이력 콘텐츠 (fragment)
     */
    @GetMapping("/content")
    public String listContent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<MemberLoginHistory> histories;

        if (email != null && !email.isEmpty()) {
            // 이메일 검색
            histories = memberService.getLoginHistoryByEmail(email, pageable);
        } else if (result != null && !result.isEmpty()) {
            // 결과 필터
            if ("FAIL".equals(result)) {
                histories = memberService.getFailedLogins(pageable);
            } else {
                histories = memberService.getLoginHistoryByResult(
                        MemberLoginHistory.LoginResult.valueOf(result), pageable);
            }
        } else if (startDate != null && endDate != null) {
            // 기간 검색
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            histories = memberService.getLoginHistoryByPeriod(start, end, pageable);
        } else {
            // 전체 조회
            histories = memberService.getLoginHistory(pageable);
        }

        model.addAttribute("histories", histories);
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", histories.getTotalPages());
        model.addAttribute("totalItems", histories.getTotalElements());
        model.addAttribute("email", email);
        model.addAttribute("result", result);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin/member/fragments/login-history";
    }
}