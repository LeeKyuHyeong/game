package com.kh.game.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Phase 1: 모든 URL permitAll (기존 Interceptor와 공존)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // Phase 1: CSRF 비활성화 (기존 폼 호환, Phase 4에서 활성화 예정)
                .csrf(csrf -> csrf.disable())
                // Phase 1: 기본 폼 로그인 비활성화 (기존 AuthController 유지)
                .formLogin(form -> form.disable())
                // Phase 1: 기본 로그아웃 비활성화 (기존 AuthController 유지)
                .logout(logout -> logout.disable())
                // Phase 1: 기본 httpBasic 비활성화
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
