package com.kh.game.config;

import com.kh.game.security.CustomAuthenticationFailureHandler;
import com.kh.game.security.CustomAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
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
        http
                // Phase 2: 모든 URL permitAll 유지 (Phase 3에서 인가 규칙 추가 예정)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // Phase 2: CSRF 비활성화 유지 (Phase 4에서 활성화 예정)
                .csrf(csrf -> csrf.disable())
                // Phase 2: Spring Security formLogin 활성화
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login-process")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                // Phase 2: Spring Security logout 활성화
                .logout(logout -> logout
                        .logoutUrl("/auth/security-logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // httpBasic 비활성화 유지
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
