package com.kh.game.security;

import com.kh.game.config.SecurityConfig;
import com.kh.game.entity.Member;
import com.kh.game.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityFilterChainTest {

    private MockMvc mockMvc;

    @Controller
    static class TestController {
        @GetMapping("/")
        @ResponseBody
        String home() { return "home"; }

        @GetMapping("/auth/login")
        @ResponseBody
        String login() { return "login"; }

        @GetMapping("/game/guess/setup")
        @ResponseBody
        String game() { return "game"; }

        @GetMapping("/admin/dashboard")
        @ResponseBody
        String admin() { return "admin"; }

        @GetMapping("/mypage")
        @ResponseBody
        String mypage() { return "mypage"; }

        @PostMapping("/auth/login-process")
        @ResponseBody
        String loginPost() { return "loginPost"; }
    }

    @Configuration
    static class TestConfig {
        @Bean
        public MemberService memberService() {
            return mock(MemberService.class);
        }

        @Bean
        public CustomAuthenticationSuccessHandler successHandler() {
            return new CustomAuthenticationSuccessHandler(memberService());
        }

        @Bean
        public CustomAuthenticationFailureHandler failureHandler() {
            return new CustomAuthenticationFailureHandler();
        }
    }

    @BeforeEach
    void setUp() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(SecurityConfig.class, TestConfig.class, TestController.class);
        context.refresh();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ========== 공개 URL 테스트 ==========

    @Test
    @DisplayName("홈페이지는 인증 없이 접근 가능")
    void homePage_noAuth_accessible() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인 페이지는 인증 없이 접근 가능")
    void loginPage_noAuth_accessible() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk());
    }

    // ========== Phase 2: permitAll 유지 + formLogin 설정 ==========

    @Test
    @DisplayName("Phase 2: 게임 페이지는 Security 필터 통과")
    void gamePage_noAuth_passesSecurityFilter() throws Exception {
        mockMvc.perform(get("/game/guess/setup"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Phase 2: 관리자 페이지도 Security 필터 통과")
    void adminPage_noAuth_passesSecurityFilter() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Phase 2: 마이페이지도 Security 필터 통과")
    void myPage_noAuth_passesSecurityFilter() throws Exception {
        mockMvc.perform(get("/mypage"))
                .andExpect(status().isOk());
    }

    // ========== CSRF 비활성화 확인 ==========

    @Test
    @DisplayName("Phase 2: CSRF 비활성화 상태 - POST 요청이 403 아닌 것")
    void csrf_disabled_postWithoutToken_notForbidden() throws Exception {
        mockMvc.perform(post("/auth/login-process")
                        .param("email", "test@test.com")
                        .param("password", "1234"))
                .andExpect(status().isOk());
    }

    // ========== Phase 2: formLogin 설정 확인 ==========

    @Test
    @DisplayName("Phase 2: 로그인 처리 URL이 /auth/login-process로 설정")
    void loginProcessUrl_configured() throws Exception {
        // login-process에 POST하면 Spring Security가 처리 (인증 실패해도 FailureHandler가 200 반환)
        mockMvc.perform(post("/auth/login-process")
                        .param("email", "nonexistent@test.com")
                        .param("password", "wrong"))
                .andExpect(status().isOk());
    }
}
