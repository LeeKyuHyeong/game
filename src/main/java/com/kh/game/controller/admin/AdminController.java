package com.kh.game.controller.admin;

import com.kh.game.entity.Member;
import com.kh.game.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MemberService memberService;

    @GetMapping("/login")
    public String login(HttpSession session) {
        // 이미 로그인된 경우 메인으로 이동
        if (session.getAttribute("adminMember") != null) {
            return "redirect:/admin/song";
        }
        return "admin/login";
    }

    @PostMapping("/login-process")
    public String loginProcess(@RequestParam String username,
                               @RequestParam String password,
                               HttpServletRequest request,
                               HttpSession session,
                               Model model) {
        try {
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // DB 기반 로그인 (MemberService 사용)
            Member member = memberService.login(username, password, ipAddress, userAgent);

            // 관리자 권한 체크
            if (member.getRole() != Member.MemberRole.ADMIN) {
                model.addAttribute("error", "관리자 권한이 없습니다.");
                return "admin/login";
            }

            // 세션에 관리자 정보 저장
            session.setAttribute("adminMember", member);
            session.setAttribute("admin", true);  // 기존 호환성 유지

            return "redirect:/admin/song";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "admin/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_CLIENT_IP",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 여러 IP가 있을 경우 첫 번째 IP 반환
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}
