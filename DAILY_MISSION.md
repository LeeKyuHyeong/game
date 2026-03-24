# Daily Dev Mission — game

> 생성일: 2026-03-24 | 프로젝트: game

---

## 미션: Batch 전체 테이블 스캔(findAll) → DB 레벨 쿼리 최적화

- **영역**: 배치 성능 최적화 / JPA 쿼리 튜닝
- **난이도**: 중급

### 문제점

여러 배치 잡에서 `findAll()`로 전체 테이블을 메모리에 로드한 뒤 Java Stream으로 필터링하고 있어, 데이터 증가 시 OOM 및 심각한 성능 저하 위험이 있음.

**문제 파일 및 위치:**
- `DailyStatsBatch.java` — `gameRoomRepository.findAll()`, `chatRepository.findAll()`, `memberRepository.findAll()`, `loginHistoryRepository.findAll()` 4회 전체 스캔 후 날짜 필터링
- `SystemReportBatch.java` — `memberRepository.findAll()` 후 `stream().filter(status == ACTIVE).count()`
- `InactiveMemberBatch.java` — `memberRepository.findAll()` 후 status, role, lastLogin 조건 Java 필터링
- `SongAnswerGenerationBatch.java` — `songRepository.findAll()` 후 `useYn` 필터링 (이미 `findByUseYn("Y")` 메서드가 존재함)

### 왜 면접 강점이 되는가

"전체 로드 후 애플리케이션 필터링 vs DB 레벨 쿼리" 최적화는 실무에서 가장 흔하게 마주치는 성능 병목이며, N+1 문제와 함께 JPA 면접 단골 주제다. 프로덕션 배치에서 실제로 개선한 경험은 강력한 어필 포인트.

### 구현 가이드

1. **Repository에 전용 쿼리 메서드 추가** — 각 배치에서 필요한 조건을 `@Query` 또는 Spring Data 메서드 쿼리로 정의
   - 예: `memberRepository.countByStatus(MemberStatus.ACTIVE)` → `SystemReportBatch` 개선
   - 예: `memberRepository.findByStatusAndRoleNotAndLastLoginBefore(...)` → `InactiveMemberBatch` 개선
   - 예: `DailyStatsBatch`용 날짜 범위 카운트 쿼리 (`countByCreatedAtBetween`)

2. **DailyStatsBatch 집중 리팩터링** — 4개의 `findAll()` 호출을 각각 `COUNT` 또는 집계 쿼리로 교체
   ```java
   // Before: gameRoomRepository.findAll().stream().filter(날짜).count()
   // After:  gameRoomRepository.countByCreatedAtBetween(startOfDay, endOfDay)
   ```

3. **SongAnswerGenerationBatch 즉시 수정** — 이미 존재하는 `findByUseYn("Y")` 메서드로 교체 (가장 간단)

4. **검증** — 배치 실행 시간을 `BatchExecutionHistory`의 `duration` 필드로 before/after 비교하여 개선 효과 측정

### 면접 질문 3선

**Q1.** JPA에서 `findAll()` 후 Java Stream 필터링과 `@Query`로 DB 필터링의 차이점은? 각각 언제 적합한가?
> 핵심 키워드: 네트워크 I/O, 메모리 사용량, DB 인덱스 활용, 페이징

**Q2.** 대량 데이터를 처리하는 배치에서 `@Transactional` 범위가 너무 넓으면 어떤 문제가 발생하는가?
> 핵심 키워드: 영속성 컨텍스트 비대화, 커넥션 점유, 롤백 범위, 청크 단위 처리

**Q3.** Spring Data JPA의 메서드 쿼리 vs `@Query` vs Specification — 각각의 장단점과 선택 기준은?
> 핵심 키워드: 타입 안전성, 동적 쿼리, 가독성, 복잡도 임계점
