#!/usr/bin/env python3
"""
Song Integrity MCP - 테스트 스크립트
"""
import sys
import os

# Windows 콘솔 UTF-8 출력 설정
if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

# 현재 디렉토리를 경로에 추가
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from orchestrator import IntegrityCheckOrchestrator
from models import DecisionType


def test_db_connection():
    """DB 연결 테스트"""
    print("=" * 60)
    print("1. DB 연결 테스트")
    print("=" * 60)

    orch = IntegrityCheckOrchestrator()
    stats = orch.get_db_statistics()
    print(f"전체 노래 수: {stats['total_songs']}")
    print(f"YouTube ID 있는 노래: {stats['songs_with_youtube']}")
    print(f"솔로곡: {stats['solo_songs']}")
    print(f"그룹곡: {stats['group_songs']}")
    print()


def test_single_song_check(song_id: int = 1):
    """단일 노래 체크 테스트"""
    print("=" * 60)
    print(f"2. 단일 노래 체크 테스트 (ID: {song_id})")
    print("=" * 60)

    orch = IntegrityCheckOrchestrator()
    result = orch.check_single_song(song_id)

    if "error" in result:
        print(f"에러: {result['error']}")
        return

    # 각 단계별 결과 출력
    for step in result.get("steps", []):
        print(f"\n[{step['agent']}] {step['action']}")
        if step.get("formatted"):
            print(step["formatted"])

    print()


def test_youtube_search():
    """YouTube 검색 테스트"""
    print("=" * 60)
    print("3. YouTube 검색 테스트")
    print("=" * 60)

    orch = IntegrityCheckOrchestrator()

    # 대체 영상 검색
    alternatives = orch.search_helper.search_alternative_videos(
        artist="아이유",
        title="좋은 날",
        max_results=3
    )

    print(f"검색 결과: {len(alternatives)}개")
    for alt in alternatives:
        print(f"  - {alt['title']}")
        print(f"    채널: {alt['channel']}")
        print(f"    URL: {alt['url']}")
        print()


def test_session_workflow():
    """세션 워크플로우 테스트"""
    print("=" * 60)
    print("4. 세션 워크플로우 테스트 (첫 3곡)")
    print("=" * 60)

    orch = IntegrityCheckOrchestrator()

    # 세션 시작 (처음 3곡만)
    songs = orch.data_manager.get_songs_with_youtube_id()[:3]
    song_ids = [s.id for s in songs]

    session = orch.start_session(song_ids)
    print(f"세션 시작: {session['session_id']}")
    print(f"체크할 노래: {session['total_songs']}개")
    print()

    # 각 노래 체크
    for i in range(len(song_ids)):
        result = orch.check_next()

        if result.get("status") == "session_completed":
            print("세션 완료!")
            break

        print(f"\n--- 노래 {i+1} ---")
        if result.get("display"):
            print(result["display"][:500] + "...")

        # 테스트용: 모두 keep 결정
        song_id = result.get("song_id")
        if song_id:
            orch.process_user_decision(
                song_id=song_id,
                decision_type="keep",
                reason="테스트 - 자동 keep"
            )

    # 요약 출력
    print("\n" + "=" * 60)
    print("세션 요약")
    print("=" * 60)
    summary = orch.get_session_summary()
    print(f"비교 결과: {summary.get('comparison', {})}")
    print(f"결정 결과: {summary.get('decisions', {})}")


def test_agent_status():
    """에이전트 상태 테스트"""
    print("=" * 60)
    print("5. 에이전트 상태 테스트")
    print("=" * 60)

    orch = IntegrityCheckOrchestrator()
    status = orch.get_agent_status()

    for agent, info in status.items():
        print(f"\n{agent}:")
        for key, value in info.items():
            print(f"  {key}: {value}")


if __name__ == "__main__":
    print("Song Integrity MCP 테스트")
    print("=" * 60)
    print()

    # 인자로 특정 테스트만 실행 가능
    if len(sys.argv) > 1:
        test_name = sys.argv[1]
        if test_name == "db":
            test_db_connection()
        elif test_name == "song":
            song_id = int(sys.argv[2]) if len(sys.argv) > 2 else 1
            test_single_song_check(song_id)
        elif test_name == "youtube":
            test_youtube_search()
        elif test_name == "session":
            test_session_workflow()
        elif test_name == "status":
            test_agent_status()
        else:
            print(f"Unknown test: {test_name}")
            print("Available: db, song, youtube, session, status")
    else:
        # 전체 테스트
        test_db_connection()
        test_single_song_check()
        test_youtube_search()
        test_session_workflow()
        test_agent_status()
