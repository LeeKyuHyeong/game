# TODO

## 계정
- 관리자 : a@a.com / 123!@#
- 일반 : rbgud2380@naver.com / 1234

## Spring Security 마이그레이션 (TDD)

### Phase 1: 기반 구축 (진행 중)
- [x] pom.xml에 spring-boot-starter-security 추가
- [x] CustomUserDetails 구현 (위임 패턴, Member 래핑)
- [x] CustomUserDetailsService 구현 + 테스트 7개 통과
- [x] SecurityFilterChain 구현 (permitAll) + 테스트 6개 통과
- [ ] 기존 테스트 전체 통과 확인

### Phase 2: 인증 흐름 전환
- [ ] CustomAuthenticationSuccessHandler (하위 호환 세션 속성 세팅)
- [ ] CustomAuthenticationFailureHandler (실패 사유별 메시지)
- [ ] SecurityFilterChain에 formLogin/logout 설정
- [ ] AuthController 리팩토링

### Phase 3: 인가 전환
- [ ] URL별 권한 규칙 추가 (admin → ADMIN, mypage → authenticated)
- [ ] @EnableMethodSecurity + @PreAuthorize 적용
- [ ] AdminInterceptor 제거

### Phase 4: 세션 관리 + CSRF
- [ ] maximumSessions(1) 동시 세션 제어
- [ ] SessionValidationInterceptor 제거
- [ ] CSRF 활성화 + Thymeleaf/AJAX 토큰 적용

### Phase 5: 컨트롤러 정리
- [ ] session.getAttribute("member") → @AuthenticationPrincipal 전환 (~140곳)
- [ ] Thymeleaf sec:authorize 태그 적용
- [ ] SuccessHandler 하위 호환 세션 속성 제거

### Phase 6: 정리 및 제거
- [ ] AdminInterceptor, SessionValidationInterceptor 삭제
- [ ] Member 엔티티 sessionToken/sessionCreatedAt 필드 제거
- [ ] MemberService 세션 관련 메서드 제거
- [ ] 전체 통합 테스트 통과 확인

## 미완료 항목

### UI/UX 개선

### 기능 구현

### 관리자 화면
