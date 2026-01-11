package com.kh.game.controller.client;

import com.kh.game.entity.Member;
import com.kh.game.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    // 로그인 페이지
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String redirect, Model model) {
        model.addAttribute("redirect", redirect);
        return "client/auth/login";
    }

    // 로그인 처리
    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest,
            HttpSession session) {

        Map<String, Object> result = new HashMap<>();

        try {
            String email = request.get("email");
            String password = request.get("password");
            boolean forceLogin = "true".equals(request.get("forceLogin"));

            String ipAddress = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            // 중복 로그인 체크 (forceLogin이 아닌 경우)
            if (!forceLogin) {
                MemberService.LoginAttemptResult attemptResult = memberService.checkLoginAttempt(email);

                if (attemptResult.status() == MemberService.LoginAttemptStatus.IN_GAME) {
                    // 게임 중인 경우 - 확인 필요
                    result.put("success", false);
                    result.put("requireConfirm", true);
                    result.put("inGame", true);
                    result.put("message", "현재 다른 기기에서 게임이 진행 중입니다. 강제 로그인하시겠습니까?");
                    return ResponseEntity.ok(result);
                }
                // EXISTING_SESSION인 경우 - 바로 로그인 진행 (기존 세션은 자동 무효화)
            }

            Member member = memberService.login(email, password, ipAddress, userAgent);

            // 새 세션 토큰 생성 (기존 세션 자동 무효화)
            String sessionToken = memberService.createSessionToken(member.getId());

            // 세션에 회원 정보 저장
            session.setAttribute("memberId", member.getId());
            session.setAttribute("memberEmail", member.getEmail());
            session.setAttribute("memberNickname", member.getNickname());
            session.setAttribute("memberRole", member.getRole().name());
            session.setAttribute("sessionToken", sessionToken);
            session.setAttribute("isLoggedIn", true);

            result.put("success", true);
            result.put("nickname", member.getNickname());

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "로그인 처리 중 오류가 발생했습니다.");
        }

        return ResponseEntity.ok(result);
    }

    // 회원가입 페이지
    @GetMapping("/register")
    public String registerPage() {
        return "client/auth/register";
    }

    // 회원가입 처리
    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            String email = request.get("email");
            String password = request.get("password");
            String nickname = request.get("nickname");
            String username = request.get("username");

            // 유효성 검사
            if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
            }
            if (password == null || password.length() < 4) {
                throw new IllegalArgumentException("비밀번호는 4자 이상이어야 합니다.");
            }
            if (nickname == null || nickname.length() < 2 || nickname.length() > 20) {
                throw new IllegalArgumentException("닉네임은 2~20자 이내로 입력해주세요.");
            }

            Member member = memberService.register(email, password, nickname, username);

            result.put("success", true);
            result.put("message", "회원가입이 완료되었습니다.");

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "회원가입 처리 중 오류가 발생했습니다.");
        }

        return ResponseEntity.ok(result);
    }

    // 로그아웃
    @PostMapping("/logout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        // 세션 토큰 무효화
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId != null) {
            memberService.invalidateSessionToken(memberId);
        }

        session.removeAttribute("memberId");
        session.removeAttribute("memberEmail");
        session.removeAttribute("memberNickname");
        session.removeAttribute("memberRole");
        session.removeAttribute("sessionToken");
        session.removeAttribute("isLoggedIn");

        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    // 이메일 중복 체크
    @GetMapping("/check-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        Map<String, Object> result = new HashMap<>();
        boolean exists = memberService.findByEmail(email).isPresent();
        result.put("available", !exists);
        return ResponseEntity.ok(result);
    }

    // 현재 로그인 상태 확인
    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatus(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        if (isLoggedIn != null && isLoggedIn) {
            result.put("isLoggedIn", true);
            result.put("memberId", session.getAttribute("memberId"));
            result.put("nickname", session.getAttribute("memberNickname"));
            result.put("email", session.getAttribute("memberEmail"));
        } else {
            result.put("isLoggedIn", false);
        }

        return ResponseEntity.ok(result);
    }

    // 세션 유효성 검증 (중복 로그인 감지용)
    @GetMapping("/validate-session")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateSession(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        Long memberId = (Long) session.getAttribute("memberId");
        String sessionToken = (String) session.getAttribute("sessionToken");

        if (memberId == null || sessionToken == null) {
            result.put("valid", false);
            result.put("reason", "NOT_LOGGED_IN");
            return ResponseEntity.ok(result);
        }

        boolean isValid = memberService.validateSessionToken(memberId, sessionToken);

        if (isValid) {
            result.put("valid", true);
        } else {
            // 세션이 무효화됨 - 다른 기기에서 로그인됨
            result.put("valid", false);
            result.put("reason", "SESSION_INVALIDATED");
            result.put("message", "다른 기기에서 로그인하여 현재 세션이 종료되었습니다.");
        }

        return ResponseEntity.ok(result);
    }

    // 클라이언트 IP 가져오기
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}