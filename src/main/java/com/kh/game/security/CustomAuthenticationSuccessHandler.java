package com.kh.game.security;

import com.kh.game.entity.Member;
import com.kh.game.service.MemberService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final MemberService memberService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Member member = userDetails.getMember();

        // 로그인 이력 기록 및 lastLoginAt 갱신
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        memberService.recordLoginSuccess(member.getId(), ipAddress, userAgent);

        // 세션 토큰 생성 (중복 로그인 감지용, Phase 4에서 제거 예정)
        String sessionToken = memberService.createSessionToken(member.getId());

        // 하위 호환 세션 속성 세팅 (Phase 5에서 제거 예정)
        HttpSession session = request.getSession();
        session.setAttribute("member", member);
        session.setAttribute("memberId", member.getId());
        session.setAttribute("memberEmail", member.getEmail());
        session.setAttribute("memberNickname", member.getNickname());
        session.setAttribute("memberRole", member.getRole().name());
        session.setAttribute("sessionToken", sessionToken);
        session.setAttribute("isLoggedIn", true);

        // JSON 응답 (기존 프론트엔드 AJAX 호환)
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":true,\"nickname\":\"" + escapeJson(member.getNickname()) + "\"}");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
