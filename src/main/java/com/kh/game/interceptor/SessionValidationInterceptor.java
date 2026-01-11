package com.kh.game.interceptor;

import com.kh.game.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 세션 유효성 검증 인터셉터
 * 중복 로그인 감지 및 강제 로그아웃 처리
 */
@Component
@RequiredArgsConstructor
public class SessionValidationInterceptor implements HandlerInterceptor {

    private final MemberService memberService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // AJAX 요청인지 확인
        boolean isAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));

        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;  // 세션 없으면 그냥 통과 (로그인 안 된 상태)
        }

        Long memberId = (Long) session.getAttribute("memberId");
        String sessionToken = (String) session.getAttribute("sessionToken");

        // 로그인 안 된 상태면 통과
        if (memberId == null || sessionToken == null) {
            return true;
        }

        // 세션 토큰 유효성 검증
        boolean isValid = memberService.validateSessionToken(memberId, sessionToken);

        if (!isValid) {
            // 세션 무효화 (다른 기기에서 로그인됨)
            session.removeAttribute("memberId");
            session.removeAttribute("memberEmail");
            session.removeAttribute("memberNickname");
            session.removeAttribute("memberRole");
            session.removeAttribute("sessionToken");
            session.removeAttribute("isLoggedIn");

            if (isAjax) {
                // AJAX 요청인 경우 - JSON 응답
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"SESSION_INVALIDATED\",\"message\":\"다른 기기에서 로그인하여 현재 세션이 종료되었습니다.\"}");
                return false;
            } else {
                // 일반 요청인 경우 - 헤더에 표시 (JS에서 감지)
                response.setHeader("X-Session-Invalid", "true");
                // 페이지는 그대로 로드하되, JS에서 감지하여 알림 표시
            }
        }

        return true;
    }
}
