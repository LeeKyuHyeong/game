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

@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final MemberRepository memberRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        // 로그인 페이지는 인증 없이 접근 가능
        if (requestURI.equals("/admin/login") || requestURI.equals("/admin/login-process")) {
            return true;
        }

        HttpSession session = request.getSession(false);

        // 세션 없음
        if (session == null) {
            response.sendRedirect("/admin/login");
            return false;
        }

        // 관리자 정보 확인
        Member adminMember = (Member) session.getAttribute("adminMember");

        if (adminMember == null) {
            // 클라이언트에서 ADMIN 권한으로 로그인한 경우 처리
            String memberRole = (String) session.getAttribute("memberRole");
            Long memberId = (Long) session.getAttribute("memberId");

            if ("ADMIN".equals(memberRole) && memberId != null) {
                // DB에서 회원 정보 조회하여 adminMember 세션에 설정
                Member member = memberRepository.findById(memberId).orElse(null);
                if (member != null && member.getRole() == Member.MemberRole.ADMIN) {
                    session.setAttribute("adminMember", member);
                    session.setAttribute("admin", true);
                    return true;
                }
            }

            // 기존 호환성: admin 속성만 있는 경우도 허용 (마이그레이션 기간)
            Boolean admin = (Boolean) session.getAttribute("admin");
            if (admin == null || !admin) {
                response.sendRedirect("/admin/login");
                return false;
            }
        } else {
            // 관리자 권한 재확인
            if (adminMember.getRole() != Member.MemberRole.ADMIN) {
                session.invalidate();
                response.sendRedirect("/admin/login");
                return false;
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 모든 관리자 페이지에 관리자 정보 전달
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
