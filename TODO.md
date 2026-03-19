# TODO

## 계정
- 관리자 : a@a.com / 123!@#
- 일반 : rbgud2380@naver.com / 1234

## Spring Security 마이그레이션 (TDD)

### Phase 1: 기반 구축 ✅ 완료
- [x] pom.xml에 spring-boot-starter-security + thymeleaf-extras-springsecurity6 + spring-security-test 추가
- [x] CustomUserDetails 구현 (위임 패턴, Member 래핑, ROLE_USER/ROLE_ADMIN 매핑)
- [x] CustomUserDetailsService 구현 + 테스트 7개 통과
- [x] SecurityFilterChain 구현 (permitAll, csrf/formLogin/logout/httpBasic 비활성화) + 테스트 6개 통과
- [x] Security 관련 테스트 13개 전체 통과
- [ ] 기존 통합 테스트 전체 통과 확인 (ApplicationContext 로드 실패 — DB 연결 등 환경 문제, Security와 무관)

### Phase 2: 인증 흐름 전환 ✅ 완료
- [x] CustomAuthenticationSuccessHandler 구현 (하위 호환 세션 속성 + JSON 응답 + 로그인 이력 기록) + 테스트 5개
- [x] CustomAuthenticationFailureHandler 구현 (BadCredentials/Locked/Disabled 메시지) + 테스트 4개
- [x] SecurityFilterChain에 formLogin/logout 설정 (loginProcessingUrl=/auth/login-process) + 테스트 7개
- [x] AuthController 리팩토링 (로그인 → Spring Security 위임, 중복체크 → /auth/check-login 분리)
- [x] auth-login.js 프론트엔드 Spring Security formLogin 연동
- [x] MemberService.recordLoginSuccess() 추가 (SuccessHandler에서 호출)
- [x] Security 테스트 23개 전체 통과

### Phase 3: 인가 전환 ✅ 완료
- [x] URL별 권한 규칙 추가 (AntPathRequestMatcher: /admin/** → ROLE_ADMIN, /mypage/** → authenticated)
- [x] @EnableMethodSecurity 활성화
- [x] AdminInterceptor 단순화 (인증/인가 → Spring Security 위임, adminMember 세션 세팅만 유지)
- [x] Security 테스트 27개 전체 통과 (인가 테스트 6개 추가)

### Phase 4: 세션 관리 + CSRF (미착수)
- [ ] maximumSessions(1) 동시 세션 제어 (현재 커스텀 sessionToken 방식)
- [ ] SessionValidationInterceptor 제거 (현재 중복 로그인 감지에 사용 중)
- [ ] CSRF 활성화 + Thymeleaf/AJAX 토큰 적용 (현재 csrf.disable() 상태)

### Phase 5: 컨트롤러 정리 (미착수)
- [ ] session.getAttribute("member") → @AuthenticationPrincipal 전환 (~17곳 확인됨)
- [ ] Thymeleaf sec:authorize 태그 적용 (현재 0건)
- [ ] SuccessHandler 하위 호환 세션 속성 제거

### Phase 6: 정리 및 제거 (Phase 2~5 완료 후 진행)
- [ ] AdminInterceptor, SessionValidationInterceptor 삭제
- [ ] Member 엔티티 sessionToken/sessionCreatedAt 필드 제거
- [ ] MemberService 세션 관련 메서드 제거 (createSessionToken, validateSessionToken, invalidateSessionToken, checkLoginAttempt)
- [ ] 전체 통합 테스트 통과 확인

## 미완료 항목

### UI/UX 개선

### 기능 구현

### 관리자 화면
