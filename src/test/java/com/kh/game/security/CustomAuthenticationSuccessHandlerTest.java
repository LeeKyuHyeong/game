package com.kh.game.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kh.game.entity.Member;
import com.kh.game.service.MemberService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationSuccessHandlerTest {

    @Mock
    private MemberService memberService;

    private CustomAuthenticationSuccessHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ObjectMapper objectMapper;

    private Member testMember;
    private CustomUserDetails userDetails;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        handler = new CustomAuthenticationSuccessHandler(memberService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        objectMapper = new ObjectMapper();

        testMember = new Member();
        testMember.setId(1L);
        testMember.setEmail("user@test.com");
        testMember.setNickname("testUser");
        testMember.setRole(Member.MemberRole.USER);
        testMember.setStatus(Member.MemberStatus.ACTIVE);

        userDetails = new CustomUserDetails(testMember);
        authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    @DisplayName("로그인 성공 시 JSON 응답 반환")
    void onAuthenticationSuccess_returnsJsonResponse() throws Exception {
        when(memberService.createSessionToken(1L)).thenReturn("test-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        Map<String, Object> result = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("nickname")).isEqualTo("testUser");
    }

    @Test
    @DisplayName("로그인 성공 시 하위 호환 세션 속성 세팅")
    void onAuthenticationSuccess_setsBackwardCompatibleSessionAttributes() throws Exception {
        when(memberService.createSessionToken(1L)).thenReturn("test-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        HttpSession session = request.getSession();
        assertThat(session.getAttribute("member")).isEqualTo(testMember);
        assertThat(session.getAttribute("memberId")).isEqualTo(1L);
        assertThat(session.getAttribute("memberEmail")).isEqualTo("user@test.com");
        assertThat(session.getAttribute("memberNickname")).isEqualTo("testUser");
        assertThat(session.getAttribute("memberRole")).isEqualTo("USER");
        assertThat(session.getAttribute("sessionToken")).isEqualTo("test-token");
        assertThat(session.getAttribute("isLoggedIn")).isEqualTo(true);
    }

    @Test
    @DisplayName("관리자 로그인 시 ADMIN 역할 세션 속성")
    void onAuthenticationSuccess_adminRole_setsAdminRoleInSession() throws Exception {
        Member adminMember = new Member();
        adminMember.setId(2L);
        adminMember.setEmail("admin@test.com");
        adminMember.setNickname("admin");
        adminMember.setRole(Member.MemberRole.ADMIN);
        adminMember.setStatus(Member.MemberStatus.ACTIVE);

        CustomUserDetails adminDetails = new CustomUserDetails(adminMember);
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities());

        when(memberService.createSessionToken(2L)).thenReturn("admin-token");

        handler.onAuthenticationSuccess(request, response, adminAuth);

        assertThat(request.getSession().getAttribute("memberRole")).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("로그인 성공 시 세션 토큰 생성 호출")
    void onAuthenticationSuccess_createsSessionToken() throws Exception {
        when(memberService.createSessionToken(1L)).thenReturn("test-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(memberService).createSessionToken(1L);
    }

    @Test
    @DisplayName("로그인 성공 시 로그인 이력 기록 호출")
    void onAuthenticationSuccess_recordsLoginHistory() throws Exception {
        when(memberService.createSessionToken(1L)).thenReturn("test-token");
        request.addHeader("User-Agent", "TestBrowser/1.0");
        request.setRemoteAddr("127.0.0.1");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(memberService).recordLoginSuccess(eq(1L), anyString(), eq("TestBrowser/1.0"));
    }
}
