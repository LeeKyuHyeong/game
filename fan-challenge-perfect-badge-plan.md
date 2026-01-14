# 아티스트 챌린지 퍼펙트 클리어 뱃지 구현 계획

## 요약
팬챌린지(아티스트 챌린지) 퍼펙트 클리어 시 뱃지를 지급하는 기능 구현

## 설계 결정

### 1. 뱃지 지급 방식
- **아티스트별 퍼펙트**: 기존 `FanChallengeRecord.isPerfectClear` 필드 활용 (UI에서 표시)
- **마일스톤 뱃지**: Badge 테이블에 6개 신규 뱃지 추가

### 2. 신규 뱃지 (6개)
| Code | 이름 | 조건 | 희귀도 |
|------|------|------|--------|
| FAN_FIRST_PERFECT | 첫 퍼펙트 ⭐ | 첫 아티스트 퍼펙트 클리어 | RARE |
| FAN_PERFECT_5 | 퍼펙트 수집가 🌟 | 5개 아티스트 퍼펙트 | EPIC |
| FAN_PERFECT_10 | 퍼펙트 마스터 💫 | 10개 아티스트 퍼펙트 | LEGENDARY |
| FAN_HARDCORE_FIRST | 하드코어 정복자 🔥 | 첫 하드코어 퍼펙트 | EPIC |
| FAN_HARDCORE_5 | 하드코어 마스터 💥 | 5개 하드코어 퍼펙트 | LEGENDARY |
| FAN_HARDCORE_10 | 하드코어 레전드 👑 | 10개 하드코어 퍼펙트 | LEGENDARY |

### 3. 곡 추가 시 처리
- 아티스트에 곡이 추가되면 해당 아티스트의 `isPerfectClear`를 `false`로 변경
- **마일스톤 뱃지는 회수하지 않음** (한번 획득하면 영구 보유)
- 배치 작업으로 매일 새벽 4시 검사 실행

---

## 수정 파일 목록

### 서비스 계층
1. **`src/main/java/com/kh/game/service/BadgeService.java`**
   - `checkBadgesAfterFanChallengePerfect()` 메서드 추가
   - `checkAllBadgesForMember()`에 팬챌린지 뱃지 체크 추가

2. **`src/main/java/com/kh/game/service/FanChallengeService.java`**
   - `updateRecord()`에서 퍼펙트 시 뱃지 체크 호출
   - BadgeService 의존성 주입 추가

### Repository
3. **`src/main/java/com/kh/game/repository/FanChallengeRecordRepository.java`**
   - 퍼펙트 아티스트 수 카운트 쿼리 추가
   - 퍼펙트 무효화 쿼리 추가

### 배치
4. **`src/main/java/com/kh/game/batch/FanChallengePerfectCheckBatch.java`** (신규)
   - 곡 추가 시 퍼펙트 무효화 배치

5. **`src/main/java/com/kh/game/batch/BatchScheduler.java`**
   - 신규 배치 스케줄 등록

### 초기 데이터
6. **`src/main/java/com/kh/game/config/DataInitializer.java`**
   - 6개 뱃지 초기화 추가

---

## 구현 단계

### Step 1: 뱃지 초기 데이터 추가
`DataInitializer.initBadges()`에 6개 뱃지 추가

```java
// 팬챌린지 퍼펙트 마일스톤 (카테고리: SPECIAL)
new Object[]{"FAN_FIRST_PERFECT", "첫 퍼펙트", "첫 아티스트 퍼펙트 클리어", "⭐", "#F4A261", Badge.BadgeCategory.SPECIAL, Badge.BadgeRarity.RARE, 60},
new Object[]{"FAN_PERFECT_5", "퍼펙트 수집가", "5개 아티스트 퍼펙트 클리어", "🌟", "#E9C46A", Badge.BadgeCategory.SPECIAL, Badge.BadgeRarity.EPIC, 61},
new Object[]{"FAN_PERFECT_10", "퍼펙트 마스터", "10개 아티스트 퍼펙트 클리어", "💫", "#F59E0B", Badge.BadgeCategory.SPECIAL, Badge.BadgeRarity.LEGENDARY, 62},
new Object[]{"FAN_HARDCORE_FIRST", "하드코어 정복자", "첫 하드코어 퍼펙트 클리어", "🔥", "#E76F51", Badge.BadgeCategory.SPECIAL, Badge.BadgeRarity.EPIC, 63},
new Object[]{"FAN_HARDCORE_5", "하드코어 마스터", "5개 아티스트 하드코어 퍼펙트", "💥", "#DC2626", Badge.BadgeCategory.SPECIAL, Badge.BadgeRarity.LEGENDARY, 64},
new Object[]{"FAN_HARDCORE_10", "하드코어 레전드", "10개 아티스트 하드코어 퍼펙트", "👑", "#B91C1C", Badge.BadgeCategory.SPECIAL, Badge.BadgeRarity.LEGENDARY, 65},
```

### Step 2: Repository 쿼리 추가
```java
// 회원의 퍼펙트 아티스트 수 (전체 난이도)
@Query("SELECT COUNT(DISTINCT r.artist) FROM FanChallengeRecord r " +
       "WHERE r.member = :member AND r.isPerfectClear = true")
long countDistinctPerfectArtistsByMember(@Param("member") Member member);

// 회원의 특정 난이도 퍼펙트 아티스트 수
@Query("SELECT COUNT(DISTINCT r.artist) FROM FanChallengeRecord r " +
       "WHERE r.member = :member AND r.isPerfectClear = true AND r.difficulty = :difficulty")
long countDistinctPerfectArtistsByMemberAndDifficulty(
    @Param("member") Member member,
    @Param("difficulty") FanChallengeDifficulty difficulty);

// 모든 퍼펙트 기록 조회 (배치용)
@Query("SELECT r FROM FanChallengeRecord r WHERE r.isPerfectClear = true")
List<FanChallengeRecord> findAllPerfectRecords();
```

### Step 3: BadgeService 메서드 추가
```java
/**
 * 팬 챌린지 퍼펙트 클리어 후 뱃지 체크
 */
@Transactional
public List<Badge> checkBadgesAfterFanChallengePerfect(Member member, FanChallengeDifficulty difficulty) {
    List<Badge> newBadges = new ArrayList<>();

    // 전체 퍼펙트 마일스톤
    long totalPerfect = fanChallengeRecordRepository.countDistinctPerfectArtistsByMember(member);
    if (totalPerfect >= 1) awardBadge(member, "FAN_FIRST_PERFECT").ifPresent(newBadges::add);
    if (totalPerfect >= 5) awardBadge(member, "FAN_PERFECT_5").ifPresent(newBadges::add);
    if (totalPerfect >= 10) awardBadge(member, "FAN_PERFECT_10").ifPresent(newBadges::add);

    // 하드코어 퍼펙트 마일스톤
    if (difficulty == FanChallengeDifficulty.HARDCORE) {
        long hardcorePerfect = fanChallengeRecordRepository
            .countDistinctPerfectArtistsByMemberAndDifficulty(member, FanChallengeDifficulty.HARDCORE);
        if (hardcorePerfect >= 1) awardBadge(member, "FAN_HARDCORE_FIRST").ifPresent(newBadges::add);
        if (hardcorePerfect >= 5) awardBadge(member, "FAN_HARDCORE_5").ifPresent(newBadges::add);
        if (hardcorePerfect >= 10) awardBadge(member, "FAN_HARDCORE_10").ifPresent(newBadges::add);
    }

    return newBadges;
}
```

### Step 4: FanChallengeService 수정
`updateRecord()` 메서드 내 퍼펙트 클리어 처리 부분에 추가:

```java
// 퍼펙트 클리어 시 뱃지 체크
if (session.getCorrectCount().equals(session.getTotalRounds())) {
    record.setIsPerfectClear(true);
    record.setBestTimeMs(session.getPlayTimeSeconds() * 1000);

    // 뱃지 지급 체크 (추가)
    Member member = session.getMember();
    if (member != null) {
        List<Badge> newBadges = badgeService.checkBadgesAfterFanChallengePerfect(member, difficulty);
        if (!newBadges.isEmpty()) {
            log.info("팬챌린지 퍼펙트 뱃지 획득: {} -> {}",
                member.getNickname(),
                newBadges.stream().map(Badge::getName).toList());
        }
    }
}
```

### Step 5: 퍼펙트 무효화 배치 생성
`FanChallengePerfectCheckBatch.java` 신규 생성:

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class FanChallengePerfectCheckBatch {
    public static final String BATCH_ID = "BATCH_FAN_CHALLENGE_PERFECT_CHECK";

    private final FanChallengeRecordRepository fanChallengeRecordRepository;
    private final SongService songService;
    private final BatchService batchService;

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        // 1. 현재 아티스트별 곡 수 조회
        // 2. 퍼펙트 기록 중 totalSongs < 현재 곡 수인 경우 isPerfectClear = false
        // 3. 결과 로깅
    }
}
```

### Step 6: BatchScheduler에 등록
```java
// 매일 새벽 4시 실행
@Scheduled(cron = "0 0 4 * * *")
public void runFanChallengePerfectCheck() {
    if (batchService.isBatchEnabled(FanChallengePerfectCheckBatch.BATCH_ID)) {
        fanChallengePerfectCheckBatch.execute(BatchExecutionHistory.ExecutionType.SCHEDULED);
    }
}
```

---

## 검증 방법
1. 애플리케이션 실행 후 Badge 테이블에 6개 뱃지 추가 확인
2. 팬챌린지에서 퍼펙트 클리어 달성 → 뱃지 획득 확인
3. 마이페이지에서 뱃지 표시 확인
4. 관리자에서 해당 아티스트에 곡 추가 후 배치 실행 → 퍼펙트 무효화 확인
5. 무효화된 상태에서 다시 퍼펙트 달성 → isPerfectClear 복구 확인

---

## 데이터 흐름

```
[게임 완료]
    ↓
FanChallengeService.updateRecord()
    ├── 퍼펙트 클리어? → isPerfectClear = true
    │                  → BadgeService.checkBadgesAfterFanChallengePerfect()
    │                      ├── 퍼펙트 아티스트 수 카운트
    │                      └── 마일스톤 달성 시 뱃지 지급
    └── 저장

[배치 작업 - 매일 4시]
    ↓
FanChallengePerfectCheckBatch.execute()
    ├── 아티스트별 현재 곡 수 조회
    ├── 퍼펙트 기록 검사
    │   └── totalSongs < 현재 곡 수 → isPerfectClear = false
    └── 결과 로깅
```
