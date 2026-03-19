package com.kh.game.config;

import com.kh.game.security.CustomAuthenticationFailureHandler;
import com.kh.game.security.CustomAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF: BREACH 공격 방지용 XOR 토큰 대신 plain 토큰 사용 (AJAX 호환성)
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
                // Phase 3: URL별 인가 규칙
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스
                        .requestMatchers(new AntPathRequestMatcher("/css/**"),
                                new AntPathRequestMatcher("/js/**"),
                                new AntPathRequestMatcher("/images/**"),
                                new AntPathRequestMatcher("/uploads/**"),
                                new AntPathRequestMatcher("/favicon.svg")).permitAll()
                        // 인증 관련
                        .requestMatchers(new AntPathRequestMatcher("/auth/**")).permitAll()
                        // 관리자 로그인 페이지
                        .requestMatchers(new AntPathRequestMatcher("/admin/login"),
                                new AntPathRequestMatcher("/admin/login-process"),
                                new AntPathRequestMatcher("/admin/logout")).permitAll()
                        // 관리자 페이지 - ADMIN 역할 필요
                        .requestMatchers(new AntPathRequestMatcher("/admin/**")).hasRole("ADMIN")
                        // 마이페이지 - 인증 필요
                        .requestMatchers(new AntPathRequestMatcher("/mypage/**")).authenticated()
                        // 나머지 - 모두 허용 (게임, 홈 등)
                        .anyRequest().permitAll()
                )
                // Phase 4: CSRF 활성화 (HttpSession 기반 + meta 태그로 전달)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                        .csrfTokenRequestHandler(csrfHandler)
                )
                // Phase 2: Spring Security formLogin
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login-process")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                // Phase 2: Spring Security logout
                .logout(logout -> logout
                        .logoutUrl("/auth/security-logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // Phase 4: 세션 관리 - 동시 세션 제어
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/auth/login?expired=true")
                )
                // httpBasic 비활성화 유지
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
