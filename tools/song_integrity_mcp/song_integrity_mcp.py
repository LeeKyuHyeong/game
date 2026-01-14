#!/usr/bin/env python3
"""
Song Integrity MCP Server
노래 데이터 정합성 체크 MCP 서버

7개 에이전트 팀:
1. DataManager - DB 데이터 저장/관리
2. YouTubeSearcher - YouTube 정보 검색
3. DataVerifier - 검색 결과 검증
4. DataComparator - 데이터 비교
5. SearchHelper - 검색/검증 보조
6. DecisionAdvisor - 사용자 결정 요청
7. DBUpdater - 운영DB 적용
"""
import sys
import json
import logging
from typing import Optional, List

# MCP 프로토콜
from mcp.server import Server
from mcp.types import Tool, TextContent
from mcp.server.stdio import stdio_server

# 오케스트레이터
from orchestrator import IntegrityCheckOrchestrator
from models import DecisionType

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stderr)]
)
logger = logging.getLogger("SongIntegrityMCP")

# MCP 서버 초기화
server = Server("song-integrity-mcp")

# 오케스트레이터 인스턴스
orchestrator: Optional[IntegrityCheckOrchestrator] = None


def get_orchestrator() -> IntegrityCheckOrchestrator:
    """오케스트레이터 싱글톤"""
    global orchestrator
    if orchestrator is None:
        orchestrator = IntegrityCheckOrchestrator()
    return orchestrator


@server.list_tools()
async def list_tools():
    """사용 가능한 도구 목록"""
    return [
        Tool(
            name="integrity_start_session",
            description="정합성 체크 세션을 시작합니다. 특정 song_ids를 지정하거나, 비우면 전체 노래를 체크합니다.",
            inputSchema={
                "type": "object",
                "properties": {
                    "song_ids": {
                        "type": "array",
                        "items": {"type": "integer"},
                        "description": "체크할 노래 ID 목록 (비우면 전체)"
                    }
                }
            }
        ),
        Tool(
            name="integrity_check_song",
            description="특정 노래의 정합성을 체크합니다. YouTube 검색, 검증, 비교까지 전체 워크플로우를 수행합니다.",
            inputSchema={
                "type": "object",
                "properties": {
                    "song_id": {
                        "type": "integer",
                        "description": "체크할 노래 ID"
                    }
                },
                "required": ["song_id"]
            }
        ),
        Tool(
            name="integrity_check_next",
            description="세션에서 다음 노래를 체크합니다. 세션이 없으면 에러를 반환합니다.",
            inputSchema={
                "type": "object",
                "properties": {}
            }
        ),
        Tool(
            name="integrity_make_decision",
            description="노래에 대한 결정을 내립니다. keep(유지), update(수정), delete(삭제), replace(교체), skip(건너뛰기)",
            inputSchema={
                "type": "object",
                "properties": {
                    "song_id": {
                        "type": "integer",
                        "description": "결정할 노래 ID"
                    },
                    "decision": {
                        "type": "string",
                        "enum": ["keep", "update", "delete", "replace", "skip"],
                        "description": "결정 유형"
                    },
                    "new_youtube_id": {
                        "type": "string",
                        "description": "새 YouTube ID (replace 시 필수)"
                    },
                    "update_fields": {
                        "type": "object",
                        "description": "업데이트할 필드들 (update 시 사용)"
                    },
                    "reason": {
                        "type": "string",
                        "description": "결정 사유"
                    },
                    "apply_now": {
                        "type": "boolean",
                        "description": "즉시 DB에 적용할지 여부",
                        "default": False
                    }
                },
                "required": ["song_id", "decision"]
            }
        ),
        Tool(
            name="integrity_apply_all",
            description="모든 결정을 DB에 적용합니다. dry_run=true면 미리보기만, false면 실제 적용",
            inputSchema={
                "type": "object",
                "properties": {
                    "dry_run": {
                        "type": "boolean",
                        "description": "Dry-run 모드 (기본 true)",
                        "default": True
                    }
                }
            }
        ),
        Tool(
            name="integrity_get_summary",
            description="현재 세션의 요약 정보를 반환합니다.",
            inputSchema={
                "type": "object",
                "properties": {}
            }
        ),
        Tool(
            name="integrity_get_stats",
            description="DB 통계 정보를 반환합니다.",
            inputSchema={
                "type": "object",
                "properties": {}
            }
        ),
        Tool(
            name="integrity_agent_status",
            description="모든 에이전트의 상태를 반환합니다.",
            inputSchema={
                "type": "object",
                "properties": {}
            }
        ),
        Tool(
            name="integrity_search_alternatives",
            description="특정 노래의 대체 YouTube 영상을 검색합니다.",
            inputSchema={
                "type": "object",
                "properties": {
                    "artist": {
                        "type": "string",
                        "description": "아티스트명"
                    },
                    "title": {
                        "type": "string",
                        "description": "곡명"
                    },
                    "max_results": {
                        "type": "integer",
                        "description": "최대 결과 수",
                        "default": 5
                    }
                },
                "required": ["artist", "title"]
            }
        ),
        Tool(
            name="integrity_verify_video",
            description="YouTube Video ID가 유효한지 확인합니다.",
            inputSchema={
                "type": "object",
                "properties": {
                    "video_id": {
                        "type": "string",
                        "description": "YouTube Video ID"
                    }
                },
                "required": ["video_id"]
            }
        ),
        Tool(
            name="integrity_clear_cache",
            description="모든 캐시를 초기화합니다.",
            inputSchema={
                "type": "object",
                "properties": {}
            }
        ),
        Tool(
            name="integrity_check_answer_match",
            description="YouTube 제목에서 추출한 노래 제목과 song_answer 테이블의 대표 정답을 비교합니다.",
            inputSchema={
                "type": "object",
                "properties": {
                    "song_id": {
                        "type": "integer",
                        "description": "체크할 노래 ID"
                    },
                    "youtube_title": {
                        "type": "string",
                        "description": "YouTube 비디오 제목 (직접 입력 시 사용, 없으면 자동 조회)"
                    }
                },
                "required": ["song_id"]
            }
        ),
        Tool(
            name="integrity_check_answer_match_batch",
            description="여러 노래의 YouTube 제목과 song_answer 대표 정답을 일괄 비교합니다.",
            inputSchema={
                "type": "object",
                "properties": {
                    "song_ids": {
                        "type": "array",
                        "items": {"type": "integer"},
                        "description": "체크할 노래 ID 목록"
                    }
                },
                "required": ["song_ids"]
            }
        ),
        Tool(
            name="integrity_get_song_answers",
            description="특정 노래의 정답 목록을 조회합니다 (대표 정답 포함).",
            inputSchema={
                "type": "object",
                "properties": {
                    "song_id": {
                        "type": "integer",
                        "description": "조회할 노래 ID"
                    }
                },
                "required": ["song_id"]
            }
        )
    ]


@server.call_tool()
async def call_tool(name: str, arguments: dict):
    """도구 호출 처리"""
    orch = get_orchestrator()

    try:
        if name == "integrity_start_session":
            song_ids = arguments.get("song_ids")
            result = orch.start_session(song_ids)

        elif name == "integrity_check_song":
            song_id = arguments["song_id"]
            result = orch.check_single_song(song_id)

        elif name == "integrity_check_next":
            result = orch.check_next()

        elif name == "integrity_make_decision":
            result = orch.process_user_decision(
                song_id=arguments["song_id"],
                decision_type=arguments["decision"],
                new_youtube_id=arguments.get("new_youtube_id"),
                update_fields=arguments.get("update_fields"),
                reason=arguments.get("reason"),
                apply_immediately=arguments.get("apply_now", False)
            )

        elif name == "integrity_apply_all":
            dry_run = arguments.get("dry_run", True)
            result = orch.apply_all_decisions(dry_run=dry_run)

        elif name == "integrity_get_summary":
            result = orch.get_session_summary()

        elif name == "integrity_get_stats":
            result = orch.get_db_statistics()

        elif name == "integrity_agent_status":
            result = orch.get_agent_status()

        elif name == "integrity_search_alternatives":
            result = orch.search_helper.search_alternative_videos(
                artist=arguments["artist"],
                title=arguments["title"],
                max_results=arguments.get("max_results", 5)
            )

        elif name == "integrity_verify_video":
            video_id = arguments["video_id"]
            exists, title = orch.search_helper.verify_video_exists(video_id)
            result = {"video_id": video_id, "exists": exists, "title": title}

        elif name == "integrity_clear_cache":
            orch.clear_all_caches()
            result = {"status": "All caches cleared"}

        elif name == "integrity_check_answer_match":
            result = orch.check_answer_match(
                song_id=arguments["song_id"],
                youtube_title=arguments.get("youtube_title")
            )

        elif name == "integrity_check_answer_match_batch":
            result = orch.check_answer_match_batch(arguments["song_ids"])

        elif name == "integrity_get_song_answers":
            result = orch.data_manager.get_song_answers(arguments["song_id"])

        else:
            result = {"error": f"Unknown tool: {name}"}

        # 결과를 JSON 문자열로 변환
        if isinstance(result, dict):
            # display 필드가 있으면 먼저 출력
            display = result.pop("display", None)
            output = ""
            if display:
                output = display + "\n\n---\n\n"
            output += json.dumps(result, ensure_ascii=False, indent=2, default=str)
        else:
            output = json.dumps(result, ensure_ascii=False, indent=2, default=str)

        return [TextContent(type="text", text=output)]

    except Exception as e:
        logger.error(f"Tool error: {e}", exc_info=True)
        return [TextContent(type="text", text=json.dumps({"error": str(e)}))]


async def main():
    """메인 함수"""
    logger.info("Starting Song Integrity MCP Server...")
    async with stdio_server() as (read_stream, write_stream):
        await server.run(read_stream, write_stream, server.create_initialization_options())


if __name__ == "__main__":
    import asyncio
    asyncio.run(main())
