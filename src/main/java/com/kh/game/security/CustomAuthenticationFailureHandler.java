package com.kh.game.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String message = resolveMessage(exception);

        // HTTP 200 반환 (기존 프론트엔드가 response.json()으로 파싱하므로)
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + escapeJson(message) + "\"}");
    }

    private String resolveMessage(AuthenticationException exception) {
        if (exception instanceof LockedException) {
            return "정지된 계정입니다.";
        }
        if (exception instanceof DisabledException) {
            return "비활성 계정입니다.";
        }
        if (exception instanceof BadCredentialsException || exception instanceof UsernameNotFoundException) {
            return "이메일 또는 비밀번호가 일치하지 않습니다.";
        }
        return "로그인 처리 중 오류가 발생했습니다.";
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
