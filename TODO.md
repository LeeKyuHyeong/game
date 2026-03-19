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

### Phase 4: 세션 관리 + CSRF ✅ 완료
- [x] maximumSessions(1) 동시 세션 제어 + expiredUrl 설정
- [x] SessionValidationInterceptor를 WebConfig에서 제거 (maximumSessions로 대체)
- [x] CSRF 활성화 (HttpSessionCsrfTokenRepository + meta 태그 + fetch wrapper 자동 첨부)
- [x] head fragments 3개에 CSRF meta 태그 추가
- [x] common.js fetch wrapper에 CSRF 토큰 자동 첨부 로직 추가
- [x] admin 로그인 폼 th:action으로 변경 (CSRF 토큰 자동 삽입)
- [x] adminHead에 common.js 추가 (CSRF fetch wrapper 공유)
- [x] Security 테스트 28개 전체 통과 (CSRF 테스트 2개 추가)

### Phase 5: 컨트롤러 정리 ✅ 완료
- [x] session.getAttribute("member") → @AuthenticationPrincipal CustomUserDetails 전환 (AdminBatchAffectedController 4곳, AdminRankingController 2곳, AdminController, AdminSongReportController 2곳, AuthController 3곳)
- [x] @SessionAttribute("adminMember") → @AuthenticationPrincipal 전환 (AdminSongReportController 2곳)
- [x] 소스 코드 내 session.getAttribute 0건 (인터셉터 제외, Phase 6에서 제거)
- [x] Thymeleaf sec:authorize 태그 적용 (guess/retro setup, fan-challenge result)
- [x] 전체 컨트롤러 session.getAttribute("memberId"/"isLoggedIn"/"memberNickname") → @AuthenticationPrincipal 전환 완료 (10개 컨트롤러, ~55곳)
- [x] SuccessHandler 하위 호환 세션 속성 유지 (AdminInterceptor가 sessionToken/memberId 참조, Phase 6에서 함께 제거)
- [x] Security 테스트 28개 전체 통과

### Phase 6: 정리 및 제거 ✅ 완료
- [x] AdminInterceptor 삭제 + WebConfig에서 제거
- [x] SessionValidationInterceptor 삭제
- [x] interceptor 패키지 완전 제거
- [x] Member 엔티티 sessionToken/sessionCreatedAt 필드 제거
- [x] MemberService 세션 메서드 제거 (createSessionToken, validateSessionToken, invalidateSessionToken, hasActiveSession)
- [x] checkLoginAttempt에서 sessionToken 참조 제거 (게임 중 여부만 확인)
- [x] SuccessHandler 하위 호환 세션 속성 제거 (member, memberId, memberEmail 등 7개)
- [x] AuthController에서 세션 토큰 관련 코드 제거 (logout, validate-session 단순화)
- [x] AdminMemberController kickSession → SessionRegistry 기반으로 전환
- [x] SecurityConfig에 SessionRegistry 빈 등록 + sessionManagement 연동
- [x] Security 테스트 25개 전체 통과 + 컴파일 성공

## 미완료 항목

### UI/UX 개선

### 기능 구현

### 관리자 화면
