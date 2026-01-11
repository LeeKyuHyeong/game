package com.kh.game.config;

import com.kh.game.interceptor.AdminInterceptor;
import com.kh.game.interceptor.SessionValidationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AdminInterceptor adminInterceptor;
    private final SessionValidationInterceptor sessionValidationInterceptor;

    @Value("${file.upload-dir:uploads/songs}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 관리자 페이지 인터셉터
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login", "/admin/login-process");

        // 클라이언트 세션 유효성 검증 인터셉터 (중복 로그인 감지)
        registry.addInterceptor(sessionValidationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",           // 인증 관련
                        "/admin/**",          // 관리자 (별도 인터셉터)
                        "/css/**", "/js/**",  // 정적 리소스
                        "/images/**", "/uploads/**",
                        "/error"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/songs/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}