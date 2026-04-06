# Daily Dev Mission — game

> 생성일: 2026-03-25 | 프로젝트: game

---

## 미션: MultiGameService 동시성 버그 수정 — HashMap → ConcurrentHashMap + synchronized 제거

- **영역**: 동시성(Concurrency) / 스레드 안전성
- **난이도**: 중급

### 문제점

`MultiGameService`의 `usedSongsByRoom` 필드가 일반 `HashMap`으로 선언되어 있어, 멀티플레이어 방 여러 개가 동시에 게임을 진행할 때 **Race Condition**이 발생할 수 있다. 여러 스레드가 동시에 `put`/`get`/`remove`를 호출하면 데이터 손실이나 `ConcurrentModificationException`이 발생한다. 또한 `skipCurrentSong()` 메서드에 `synchronized` 키워드가 서비스 인스턴스 전체를 잠가, 관계없는 다른 방의 요청까지 블로킹하는 성능 병목이 존재한다.

### 왜 면접 강점이 되는가

와디즈/빗썸 같은 테크 스타트업은 동시 접속 환경에서의 데이터 정합성을 중시한다. "실제 멀티플레이어 서비스에서 Race Condition을 발견하고, `ConcurrentHashMap` + 방 단위 락(또는 Optimistic Locking)으로 해결했다"는 경험은 **단순 CRUD 개발자와 차별화**되는 강력한 이력이다.

### 구현 가이드

1. **`MultiGameService.usedSongsByRoom`을 `ConcurrentHashMap`으로 교체** — `new HashMap<>()` → `new ConcurrentHashMap<>()`으로 변경. `computeIfAbsent()` 호출 시 value인 `Set`도 `ConcurrentHashMap.newKeySet()`으로 스레드 안전하게 생성
2. **`skipCurrentSong()` 메서드의 `synchronized` 제거** — 메서드 레벨 `synchronized` 대신, `GameRoom` 엔티티에 `@Version` 필드를 추가하여 Optimistic Locking 적용. 충돌 시 `OptimisticLockException`을 잡아 재시도 또는 사용자에게 안내
3. **방 단위 락이 필요한 구간 식별** — `selectSong()` → `usedSongsByRoom.add()` 사이의 원자성이 필요한 구간에 `ConcurrentHashMap.compute()`를 활용하여 단일 원자적 연산으로 통합
4. **동시성 테스트 작성** — `ExecutorService`와 `CountDownLatch`를 사용해 10개 스레드가 동시에 같은 방에서 `selectSong()`을 호출하는 테스트 작성. 중복 곡 선택이 발생하지 않는지 검증

### 면접 질문 3선

**Q1.** `ConcurrentHashMap`과 `Collections.synchronizedMap()`의 차이를 설명하고, 이 프로젝트에서 `ConcurrentHashMap`을 선택한 이유는?
> 핵심 키워드: 세그먼트 락(Segment Lock), 읽기 무잠금(Lock-Free Read), 처리량(Throughput)

**Q2.** 메서드 레벨 `synchronized` 대신 Optimistic Locking(`@Version`)을 적용했을 때의 트레이드오프는?
> 핵심 키워드: 낙관적 동시성 제어, `OptimisticLockException`, 재시도 전략(Retry)

**Q3.** 멀티플레이어 게임에서 인메모리 상태(`Map`)와 DB 상태가 불일치할 수 있는 시나리오와 해결 방법은?
> 핵심 키워드: 단일 진실 공급원(Single Source of Truth), 트랜잭션 경계, 서버 재시작 시 상태 유실

---

## 이전 미션 기록

### GlobalExceptionHandler 도입으로 예외 처리 일원화

- **영역**: 예외 처리 아키텍처 / Spring MVC 에러 핸들링
- **난이도**: 중급

### Batch 전체 테이블 스캔(findAll) → DB 레벨 쿼리 최적화

- **영역**: 배치 성능 최적화 / JPA 쿼리 튜닝
- **난이도**: 중급