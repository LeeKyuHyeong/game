package com.kh.game.controller.client;

import com.kh.game.entity.Member;
import com.kh.game.security.CustomUserDetails;
import com.kh.game.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String redirect, Model model) {
        model.addAttribute("redirect", redirect);
        return "client/auth/login";
    }

    @PostMapping("/check-login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkLogin(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        String email = request.get("email");
        MemberService.LoginAttemptResult attemptResult = memberService.checkLoginAttempt(email);

        if (attemptResult.status() == MemberService.LoginAttemptStatus.IN_GAME) {
            result.put("canProceed", false);
            result.put("requireConfirm", true);
            result.put("inGame", true);
            result.put("message", "현재 다른 기기에서 게임이 진행 중입니다. 강제 로그인하시겠습니까?");
        } else {
            result.put("canProceed", true);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/register")
    public String registerPage() {
        return "client/auth/register";
    }

    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            String email = request.get("email");
            String password = request.get("password");
            String nickname = request.get("nickname");
            String username = request.get("username");

            if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
            }
            if (password == null || password.length() < 4) {
                throw new IllegalArgumentException("비밀번호는 4자 이상이어야 합니다.");
            }
            if (nickname == null || nickname.length() < 2 || nickname.length() > 20) {
                throw new IllegalArgumentException("닉네임은 2~20자 이내로 입력해주세요.");
            }

            memberService.register(email, password, nickname, username);

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

    @PostMapping("/logout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        session.invalidate();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/check-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        Map<String, Object> result = new HashMap<>();
        boolean exists = memberService.findByEmail(email).isPresent();
        result.put("available", !exists);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatus(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> result = new HashMap<>();

        if (userDetails != null) {
            Member member = userDetails.getMember();
            result.put("isLoggedIn", true);
            result.put("memberId", member.getId());
            result.put("nickname", member.getNickname());
            result.put("email", member.getEmail());
            result.put("role", member.getRole().name());
        } else {
            result.put("isLoggedIn", false);
        }

        return ResponseEntity.ok(result);
    }

    // 세션 유효성 검증 (Spring Security maximumSessions가 처리, 하위 호환용 유지)
    @GetMapping("/validate-session")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateSession(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> result = new HashMap<>();

        if (userDetails == null) {
            result.put("valid", false);
            result.put("reason", "NOT_LOGGED_IN");
        } else {
            result.put("valid", true);
        }

        return ResponseEntity.ok(result);
    }
}
