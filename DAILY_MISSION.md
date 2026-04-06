# Daily Dev Mission — game

> 생성일: 2026-03-25 | 프로젝트: game

---

## 완료된 미션

### Polling → WebSocket(STOMP/SockJS) 전환 (2026-04-06)

- **영역**: 실시간 통신 / WebSocket
- **커밋**: `f3d36cc`
- **내용**: 멀티플레이어 HTTP polling(2초/1초/500ms)을 STOMP over SockJS로 전환. 서버→클라이언트 push 전용, POST 액션은 REST 유지. 연결 실패 시 polling fallback 지원.
- **주요 파일**: `WebSocketConfig`, `WebSocketAuthInterceptor`, `GameBroadcastService`, `ws-client.js`, `multi-waiting.js`, `multi-play.js`, `multi-result.js`

### MultiGameService 동시성 버그 수정 (2026-04-06)

- **영역**: 동시성(Concurrency) / 스레드 안전성
- **커밋**: `60afc77`
- **내용**: `HashMap` → `ConcurrentHashMap`, 메서드 레벨 `synchronized` → 방 단위 락(`roomLocks`), `GameRoom`에 `@Version` 추가(Optimistic Locking), `selectSong()` 원자적 처리.

### GlobalExceptionHandler 도입으로 예외 처리 일원화 (2026-04-06)

- **영역**: 예외 처리 아키텍처 / Spring MVC 에러 핸들링
- **커밋**: `ed73b21`
- **내용**: `@RestControllerAdvice` 기반 `GlobalExceptionHandler` 도입. 14개 컨트롤러에서 ~60개 try-catch 제거. `BusinessException` 계층 구조 신설. REST/MVC 자동 분기 응답.

### Batch 전체 테이블 스캔(findAll) → DB 레벨 쿼리 최적화

- **영역**: 배치 성능 최적화 / JPA 쿼리 튜닝
- **내용**: Batch 작업에서 `findAll()` 후 Java 필터링 → Repository 레벨 조건 쿼리로 변경.
