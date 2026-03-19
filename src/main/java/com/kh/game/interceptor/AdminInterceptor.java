package com.kh.game.interceptor;

import com.kh.game.entity.Member;
import com.kh.game.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 관리자 인터셉터 (Phase 3: 인증/인가는 Spring Security로 이전)
 * 역할: adminMember 세션 속성 자동 세팅 + ModelAndView에 관리자 정보 전달
 * Phase 6에서 완전 제거 예정
 */
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final MemberRepository memberRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return true; // Spring Security가 인증 처리
        }

        // adminMember가 아직 세션에 없으면 자동 세팅 (하위 호환)
        Member adminMember = (Member) session.getAttribute("adminMember");
        if (adminMember == null) {
            Long memberId = (Long) session.getAttribute("memberId");
            if (memberId != null) {
                Member member = memberRepository.findById(memberId).orElse(null);
                if (member != null && member.getRole() == Member.MemberRole.ADMIN) {
                    session.setAttribute("adminMember", member);
                    session.setAttribute("admin", true);
                }
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Member adminMember = (Member) session.getAttribute("adminMember");
                if (adminMember != null) {
                    modelAndView.addObject("adminMember", adminMember);
                }
            }
        }
    }
}
