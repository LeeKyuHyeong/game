#!/bin/bash
# .claude/hooks/session_start.sh
# Claude Code SessionStart Hook — 일일 미션 자동 주입기
# Max 구독 사용 (API 키 불필요)

TODAY=$(date +%Y-%m-%d)
MISSION_FILE="DAILY_MISSION.md"

# ── 오늘 미션 이미 있으면 간단한 컨텍스트만 출력 ──────────
if [ -f "$MISSION_FILE" ] && grep -q "^> 생성일: $TODAY" "$MISSION_FILE" 2>/dev/null; then
  MISSION_TITLE=$(grep "^## 미션:" "$MISSION_FILE" | head -1 | sed 's/## 미션: //')
  echo "## 오늘의 미션 ($TODAY)"
  echo "이미 생성됨: $MISSION_TITLE"
  echo "작업할 준비가 되면 '오늘 미션 진행해줘'라고 말해주세요."
  exit 0
fi

# ── 오늘 미션 없으면 분석 요청 주입 ──────────────────────
PROJECT_NAME=$(basename "$PWD")

# 소스 구조 수집
STRUCTURE=$(find . -type f -name "*.java" \
  ! -path "*/build/*" ! -path "*/.gradle/*" ! -path "*/target/*" ! -path "*/test/*" \
  2>/dev/null | head -60 | sort | sed 's|./||')

# 주요 어노테이션 패턴 수집
ANNOTATIONS=$(grep -rh \
  "@\(Service\|Repository\|RestController\|Scheduled\|Cacheable\|Transactional\|Async\|Query\|EventListener\)" \
  --include="*.java" --no-filename . 2>/dev/null | sort -u | head -30)

# 이전 미션 히스토리 (중복 방지)
HISTORY=""
if [ -f "$MISSION_FILE" ]; then
  HISTORY=$(grep -E "^## 미션:|^- \*\*영역\*\*" "$MISSION_FILE" 2>/dev/null | tail -14)
fi

# 의존성 수집
DEPS=""
if [ -f "build.gradle" ]; then
  DEPS=$(grep -E "implementation|runtimeOnly" build.gradle 2>/dev/null | head -20)
fi

# ── Claude에게 미션 생성 지시 출력 ────────────────────────
# (SessionStart stdout → Claude 컨텍스트로 자동 주입됨)
cat <<EOF
## 자동 Daily Mission 요청 ($TODAY)

프로젝트 \`$PROJECT_NAME\`에 대해 오늘의 개선 미션을 지금 바로 생성하고 \`$MISSION_FILE\`에 저장해주세요.

**분석 대상 소스 구조:**
\`\`\`
$STRUCTURE
\`\`\`

**발견된 어노테이션/패턴:**
\`\`\`
$ANNOTATIONS
\`\`\`

**의존성:**
\`\`\`
$DEPS
\`\`\`

**이전 미션 히스토리 (영역 중복 제외용):**
\`\`\`
$HISTORY
\`\`\`

**저장 형식** (DAILY_MISSION.md 상단에 추가):
\`\`\`markdown
# Daily Dev Mission — $PROJECT_NAME

> 생성일: $TODAY | 프로젝트: $PROJECT_NAME

---

## 미션: [과제 제목]

- **영역**: [개선 영역]
- **난이도**: [입문/중급/고급]

### 문제점
[실제 파일/클래스명 언급하며 구체적으로]

### 왜 면접 강점이 되는가
[테크 스타트업 면접 관점에서 1-2문장]

### 구현 가이드
1. [구체적 단계 — 클래스명/어노테이션 포함]
2. ...
3. ...
4. ...

### 면접 질문 3선

**Q1.** [질문]
> 핵심 키워드: [키워드1], [키워드2]

**Q2.** [질문]
> 핵심 키워드: [키워드1], [키워드2]

**Q3.** [질문]
> 핵심 키워드: [키워드1], [키워드2]
\`\`\`

기존 $MISSION_FILE 내용은 아래에 그대로 보존하고, 오늘 미션을 맨 위에 추가해주세요.
EOF