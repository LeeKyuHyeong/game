package com.kh.game.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAuthenticationFailureHandlerTest {

    private CustomAuthenticationFailureHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        handler = new CustomAuthenticationFailureHandler();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("잘못된 자격증명 시 에러 메시지")
    void onAuthenticationFailure_badCredentials_returnsMessage() throws Exception {
        AuthenticationException ex = new BadCredentialsException("Bad credentials");

        handler.onAuthenticationFailure(request, response, ex);

        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        Map<String, Object> result = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("정지된 계정 시 에러 메시지")
    void onAuthenticationFailure_locked_returnsMessage() throws Exception {
        AuthenticationException ex = new LockedException("Account locked");

        handler.onAuthenticationFailure(request, response, ex);

        Map<String, Object> result = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("정지된 계정입니다.");
    }

    @Test
    @DisplayName("비활성 계정 시 에러 메시지")
    void onAuthenticationFailure_disabled_returnsMessage() throws Exception {
        AuthenticationException ex = new DisabledException("Account disabled");

        handler.onAuthenticationFailure(request, response, ex);

        Map<String, Object> result = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).isEqualTo("비활성 계정입니다.");
    }

    @Test
    @DisplayName("HTTP 상태 코드 200 반환 (기존 프론트엔드 호환)")
    void onAuthenticationFailure_returnsOkStatus() throws Exception {
        AuthenticationException ex = new BadCredentialsException("Bad credentials");

        handler.onAuthenticationFailure(request, response, ex);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
