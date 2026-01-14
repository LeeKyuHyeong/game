"""
Agent 6: DecisionAdvisor
삭제할지, update할지, 새로운 youtube_video_id를 연결해야할지 사용자에게 물어보는 인원
"""
from typing import Optional, List, Dict
import logging

import sys
sys.path.append('..')
from models import (
    SongData, YouTubeInfo, ComparisonResult, UserDecision,
    DecisionType, VerificationStatus
)

logger = logging.getLogger("DecisionAdvisor")


class DecisionAdvisor:
    """사용자 결정 요청 담당 에이전트"""

    def __init__(self):
        self._pending_decisions: Dict[int, ComparisonResult] = {}
        self._completed_decisions: Dict[int, UserDecision] = {}

    def analyze_and_recommend(
        self,
        comparison: ComparisonResult,
        alternatives: Optional[List[dict]] = None
    ) -> dict:
        """비교 결과 분석 및 권장 조치 제시"""
        song = comparison.song_data
        youtube = comparison.youtube_info

        self._pending_decisions[song.id] = comparison

        recommendation = {
            "song_id": song.id,
            "song_info": {
                "artist": song.artist,
                "title": song.title,
                "year": song.release_year,
                "is_solo": song.is_solo,
                "duration": song.play_duration,
                "youtube_id": song.youtube_video_id
            },
            "youtube_info": youtube.to_dict() if youtube else None,
            "status": comparison.status.value,
            "differences": comparison.differences,
            "options": [],
            "recommended_action": None,
            "alternatives": alternatives[:3] if alternatives else []
        }

        # 상태별 옵션 생성
        if comparison.status == VerificationStatus.VERIFIED:
            recommendation["options"] = [
                {
                    "action": DecisionType.KEEP.value,
                    "description": "현재 데이터 유지 (정합성 확인됨)",
                    "recommended": True
                }
            ]
            if comparison.differences:
                recommendation["options"].append({
                    "action": DecisionType.UPDATE.value,
                    "description": "부수 정보 업데이트 (연도/재생시간 등)",
                    "recommended": False,
                    "update_preview": self._get_update_preview(comparison)
                })
            recommendation["recommended_action"] = DecisionType.KEEP.value

        elif comparison.status == VerificationStatus.MISMATCH:
            recommendation["options"] = [
                {
                    "action": DecisionType.KEEP.value,
                    "description": "현재 데이터 유지 (불일치 무시)",
                    "recommended": False
                },
                {
                    "action": DecisionType.UPDATE.value,
                    "description": "YouTube 정보로 업데이트",
                    "recommended": False,
                    "update_preview": self._get_update_preview(comparison)
                }
            ]

            if alternatives:
                recommendation["options"].append({
                    "action": DecisionType.REPLACE.value,
                    "description": "새로운 YouTube 영상으로 교체",
                    "recommended": True,
                    "alternatives_preview": [
                        {
                            "video_id": alt["video_id"],
                            "title": alt["title"],
                            "channel": alt["channel"],
                            "url": alt["url"]
                        }
                        for alt in alternatives[:3]
                    ]
                })
                recommendation["recommended_action"] = DecisionType.REPLACE.value
            else:
                recommendation["options"].append({
                    "action": DecisionType.DELETE.value,
                    "description": "해당 노래 삭제",
                    "recommended": False
                })
                recommendation["recommended_action"] = DecisionType.UPDATE.value

        elif comparison.status == VerificationStatus.VIDEO_UNAVAILABLE:
            recommendation["options"] = [
                {
                    "action": DecisionType.DELETE.value,
                    "description": "해당 노래 삭제 (영상 없음)",
                    "recommended": False
                }
            ]

            if alternatives:
                recommendation["options"].insert(0, {
                    "action": DecisionType.REPLACE.value,
                    "description": "새로운 YouTube 영상으로 교체",
                    "recommended": True,
                    "alternatives_preview": [
                        {
                            "video_id": alt["video_id"],
                            "title": alt["title"],
                            "channel": alt["channel"],
                            "url": alt["url"]
                        }
                        for alt in alternatives[:3]
                    ]
                })
                recommendation["recommended_action"] = DecisionType.REPLACE.value
            else:
                recommendation["recommended_action"] = DecisionType.DELETE.value

        else:  # ERROR
            recommendation["options"] = [
                {
                    "action": DecisionType.SKIP.value,
                    "description": "이번에는 건너뛰기",
                    "recommended": True
                },
                {
                    "action": DecisionType.DELETE.value,
                    "description": "해당 노래 삭제",
                    "recommended": False
                }
            ]
            recommendation["recommended_action"] = DecisionType.SKIP.value

        return recommendation

    def _get_update_preview(self, comparison: ComparisonResult) -> dict:
        """업데이트 미리보기 생성"""
        preview = {"changes": []}
        song = comparison.song_data
        youtube = comparison.youtube_info

        if not youtube:
            return preview

        if not comparison.artist_match and youtube.parsed_artist:
            preview["changes"].append({
                "field": "artist",
                "from": song.artist,
                "to": youtube.parsed_artist
            })

        if not comparison.title_match and youtube.parsed_title:
            preview["changes"].append({
                "field": "title",
                "from": song.title,
                "to": youtube.parsed_title
            })

        if not comparison.year_match and youtube.parsed_year:
            preview["changes"].append({
                "field": "release_year",
                "from": song.release_year,
                "to": youtube.parsed_year
            })

        if not comparison.duration_match and youtube.duration:
            preview["changes"].append({
                "field": "play_duration",
                "from": song.play_duration,
                "to": youtube.duration
            })

        return preview

    def format_for_user(self, recommendation: dict) -> str:
        """사용자에게 보여줄 형식으로 포맷팅"""
        lines = []
        lines.append("=" * 60)
        lines.append(f"🎵 노래 정보 (ID: {recommendation['song_id']})")
        lines.append("-" * 60)

        song = recommendation["song_info"]
        lines.append(f"  아티스트: {song['artist']}")
        lines.append(f"  제목: {song['title']}")
        lines.append(f"  연도: {song['year'] or 'N/A'}")
        lines.append(f"  솔로: {'예' if song['is_solo'] else '아니오'}")
        lines.append(f"  재생시간: {song['duration'] or 'N/A'}초")
        lines.append(f"  YouTube: https://youtu.be/{song['youtube_id']}")

        lines.append("")
        lines.append(f"📊 검증 결과: {self._status_emoji(recommendation['status'])} {recommendation['status'].upper()}")

        if recommendation["differences"]:
            lines.append("")
            lines.append("⚠️ 차이점:")
            for diff in recommendation["differences"]:
                lines.append(f"  • {diff}")

        if recommendation.get("youtube_info"):
            yt = recommendation["youtube_info"]
            lines.append("")
            lines.append("📺 YouTube 정보:")
            lines.append(f"  제목: {yt['title']}")
            lines.append(f"  채널: {yt['channel_name']}")
            if yt.get('parsed_artist'):
                lines.append(f"  파싱된 아티스트: {yt['parsed_artist']}")
            if yt.get('parsed_title'):
                lines.append(f"  파싱된 제목: {yt['parsed_title']}")

        lines.append("")
        lines.append("🔧 가능한 조치:")
        for i, option in enumerate(recommendation["options"], 1):
            rec_mark = " ⭐ (권장)" if option.get("recommended") else ""
            lines.append(f"  {i}. [{option['action'].upper()}] {option['description']}{rec_mark}")

            if option.get("update_preview", {}).get("changes"):
                lines.append("      변경 내용:")
                for change in option["update_preview"]["changes"]:
                    lines.append(f"        - {change['field']}: '{change['from']}' → '{change['to']}'")

            if option.get("alternatives_preview"):
                lines.append("      대체 영상:")
                for alt in option["alternatives_preview"]:
                    lines.append(f"        - {alt['title'][:50]}...")
                    lines.append(f"          {alt['url']}")

        lines.append("")
        lines.append("=" * 60)
        return "\n".join(lines)

    def _status_emoji(self, status: str) -> str:
        """상태별 이모지"""
        emojis = {
            "verified": "✅",
            "mismatch": "⚠️",
            "video_unavailable": "❌",
            "error": "🔴",
            "pending": "⏳"
        }
        return emojis.get(status, "❓")

    def record_decision(
        self,
        song_id: int,
        decision_type: DecisionType,
        new_youtube_id: Optional[str] = None,
        update_fields: Optional[dict] = None,
        reason: Optional[str] = None
    ) -> UserDecision:
        """사용자 결정 기록"""
        decision = UserDecision(
            song_id=song_id,
            decision_type=decision_type,
            new_youtube_id=new_youtube_id,
            update_fields=update_fields,
            reason=reason
        )

        self._completed_decisions[song_id] = decision
        if song_id in self._pending_decisions:
            del self._pending_decisions[song_id]

        logger.info(f"Decision recorded for song {song_id}: {decision_type.value}")
        return decision

    def get_pending_decisions(self) -> List[int]:
        """대기 중인 결정 목록"""
        return list(self._pending_decisions.keys())

    def get_completed_decisions(self) -> List[UserDecision]:
        """완료된 결정 목록"""
        return list(self._completed_decisions.values())

    def get_decision_summary(self) -> dict:
        """결정 요약"""
        summary = {
            "pending": len(self._pending_decisions),
            "completed": len(self._completed_decisions),
            "by_type": {
                "keep": 0,
                "update": 0,
                "delete": 0,
                "replace": 0,
                "skip": 0
            }
        }

        for decision in self._completed_decisions.values():
            summary["by_type"][decision.decision_type.value] += 1

        return summary

    def clear_decisions(self):
        """결정 초기화"""
        self._pending_decisions.clear()
        self._completed_decisions.clear()
