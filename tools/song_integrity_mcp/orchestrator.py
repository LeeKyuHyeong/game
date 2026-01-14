"""
Song Integrity MCP - Orchestrator
7개 에이전트를 조율하는 오케스트레이터
"""
import logging
from typing import Optional, List, Dict
from datetime import datetime

from models import (
    SongData, YouTubeInfo, VerificationResult, ComparisonResult,
    UserDecision, DecisionType, VerificationStatus, IntegrityCheckSession
)
from agents import (
    DataManager, YouTubeSearcher, DataVerifier,
    DataComparator, SearchHelper, DecisionAdvisor, DBUpdater, AnswerMatcher
)

logger = logging.getLogger("Orchestrator")


class IntegrityCheckOrchestrator:
    """
    노래 데이터 정합성 체크 오케스트레이터

    팀 구성:
    1. DataManager - DB 데이터 저장/관리
    2. YouTubeSearcher - YouTube 정보 검색
    3. DataVerifier - 검색 결과 검증
    4. DataComparator - 데이터 비교
    5. SearchHelper - 검색/검증 보조
    6. DecisionAdvisor - 사용자 결정 요청
    7. DBUpdater - 운영DB 적용
    """

    def __init__(self):
        # 에이전트 초기화
        self.data_manager = DataManager()
        self.youtube_searcher = YouTubeSearcher()
        self.data_verifier = DataVerifier()
        self.data_comparator = DataComparator()
        self.search_helper = SearchHelper()
        self.decision_advisor = DecisionAdvisor()
        self.db_updater = DBUpdater()
        self.answer_matcher = AnswerMatcher()

        # 상태
        self._current_session: Optional[IntegrityCheckSession] = None
        self._results: List[ComparisonResult] = []

        logger.info("Orchestrator initialized with all 7 agents")

    def start_session(self, song_ids: Optional[List[int]] = None) -> dict:
        """정합성 체크 세션 시작"""
        logger.info("Starting new integrity check session")

        # 1. DataManager가 세션 생성
        self._current_session = self.data_manager.create_session(song_ids)

        return {
            "status": "session_started",
            "session_id": self._current_session.session_id,
            "total_songs": len(self._current_session.songs_to_check),
            "message": f"세션이 시작되었습니다. {len(self._current_session.songs_to_check)}개 노래 체크 예정"
        }

    def check_single_song(self, song_id: int) -> dict:
        """단일 노래 정합성 체크 (전체 워크플로우)"""
        logger.info(f"Checking song ID: {song_id}")

        result = {
            "song_id": song_id,
            "steps": []
        }

        # 1. DataManager: DB에서 노래 정보 가져오기
        song = self.data_manager.get_song_by_id(song_id)
        if not song:
            return {"error": f"Song ID {song_id} not found"}

        result["steps"].append({
            "agent": "DataManager",
            "action": "get_song",
            "data": song.to_dict()
        })

        # 2. YouTubeSearcher: YouTube 정보 검색
        youtube_info = self.youtube_searcher.search_for_song(song)
        result["steps"].append({
            "agent": "YouTubeSearcher",
            "action": "search",
            "data": youtube_info.to_dict() if youtube_info else None
        })

        # 3. DataVerifier: 검색 결과 검증
        verification = None
        if youtube_info:
            verification = self.data_verifier.verify(youtube_info)
            result["steps"].append({
                "agent": "DataVerifier",
                "action": "verify",
                "data": verification.to_dict()
            })

        # 4. DataComparator: 비교 수행
        comparison = self.data_comparator.compare(song, youtube_info, verification)
        result["steps"].append({
            "agent": "DataComparator",
            "action": "compare",
            "data": comparison.to_dict()
        })

        # 5. SearchHelper: 대체 영상 검색 (불일치 또는 비가용 시)
        alternatives = []
        if comparison.status in [VerificationStatus.MISMATCH, VerificationStatus.VIDEO_UNAVAILABLE]:
            alternatives = self.search_helper.search_alternative_videos(
                song.artist, song.title, max_results=5
            )
            result["steps"].append({
                "agent": "SearchHelper",
                "action": "search_alternatives",
                "data": alternatives
            })

        # 6. DecisionAdvisor: 권장사항 분석 및 포맷팅
        recommendation = self.decision_advisor.analyze_and_recommend(comparison, alternatives)
        formatted = self.decision_advisor.format_for_user(recommendation)
        result["steps"].append({
            "agent": "DecisionAdvisor",
            "action": "analyze",
            "data": recommendation,
            "formatted": formatted
        })

        # 결과 저장
        self._results.append(comparison)
        result["recommendation"] = recommendation
        result["display"] = formatted

        return result

    def process_user_decision(
        self,
        song_id: int,
        decision_type: str,
        new_youtube_id: Optional[str] = None,
        update_fields: Optional[dict] = None,
        reason: Optional[str] = None,
        apply_immediately: bool = False
    ) -> dict:
        """사용자 결정 처리"""
        logger.info(f"Processing decision for song {song_id}: {decision_type}")

        # DecisionType 변환
        try:
            dt = DecisionType(decision_type)
        except ValueError:
            return {"error": f"Invalid decision type: {decision_type}"}

        # 6. DecisionAdvisor: 결정 기록
        decision = self.decision_advisor.record_decision(
            song_id=song_id,
            decision_type=dt,
            new_youtube_id=new_youtube_id,
            update_fields=update_fields,
            reason=reason
        )

        result = {
            "decision_recorded": True,
            "decision": decision.to_dict()
        }

        # 7. DBUpdater: 즉시 적용 옵션
        if apply_immediately:
            update_result = self.db_updater.apply_decision(decision)
            result["applied"] = update_result

        return result

    def check_next(self) -> Optional[dict]:
        """세션에서 다음 노래 체크"""
        if not self._current_session:
            return {"error": "No active session"}

        song = self.data_manager.get_next_song()
        if not song:
            return {
                "status": "session_completed",
                "message": "모든 노래 체크 완료",
                "summary": self.get_session_summary()
            }

        result = self.check_single_song(song.id)
        self.data_manager.advance_session()

        result["progress"] = self.data_manager.get_session_progress()
        return result

    def apply_all_decisions(self, dry_run: bool = True) -> dict:
        """모든 결정 일괄 적용"""
        decisions = self.decision_advisor.get_completed_decisions()

        if not decisions:
            return {"error": "No decisions to apply"}

        # DBUpdater 설정
        self.db_updater.set_dry_run(dry_run)

        # 미리보기
        preview = self.db_updater.preview_changes(decisions)

        if dry_run:
            return {
                "mode": "dry_run",
                "preview": preview,
                "message": "Dry-run 모드입니다. apply_immediately=True로 실제 적용"
            }

        # 실제 적용
        results = self.db_updater.apply_decisions_batch(decisions)

        return {
            "mode": "applied",
            "results": results,
            "summary": self.db_updater.get_execution_summary()
        }

    def get_session_summary(self) -> dict:
        """세션 요약"""
        if not self._results:
            return {"message": "No results yet"}

        comparison_summary = self.data_comparator.get_summary(self._results)
        decision_summary = self.decision_advisor.get_decision_summary()

        return {
            "comparison": comparison_summary,
            "decisions": decision_summary,
            "session": self._current_session.to_dict() if self._current_session else None
        }

    def get_db_statistics(self) -> dict:
        """DB 통계"""
        return self.data_manager.get_statistics()

    def clear_all_caches(self):
        """모든 캐시 초기화"""
        self.data_manager.clear_cache()
        self.youtube_searcher.clear_cache()
        self.data_verifier.clear_cache()
        self.search_helper.clear_cache()
        self.decision_advisor.clear_decisions()
        self.db_updater.clear_log()
        self._results.clear()
        logger.info("All caches cleared")

    def check_answer_match(self, song_id: int, youtube_title: Optional[str] = None) -> dict:
        """YouTube 제목과 song_answer 대표 정답 비교"""
        logger.info(f"Checking answer match for song ID: {song_id}")

        # 1. 노래 정보 조회
        song = self.data_manager.get_song_by_id(song_id)
        if not song:
            return {"error": f"Song ID {song_id} not found"}

        # 2. YouTube 제목 가져오기
        if not youtube_title:
            youtube_info = self.youtube_searcher.search_for_song(song)
            if youtube_info:
                youtube_title = youtube_info.title
            else:
                return {"error": "Could not fetch YouTube title"}

        # 3. 정답 정보 조회
        answers = self.data_manager.get_song_answers(song_id)

        # 4. AnswerMatcher로 분석
        result = self.answer_matcher.analyze(
            song_id=song_id,
            youtube_title=youtube_title,
            primary_answer=answers.get("primary_answer", ""),
            all_answers=answers.get("all_answers", [])
        )

        return {
            "song_info": {
                "id": song.id,
                "artist": song.artist,
                "title": song.title
            },
            "youtube_title": youtube_title,
            "answers": answers,
            "match_result": result.to_dict()
        }

    def check_answer_match_batch(self, song_ids: List[int]) -> dict:
        """여러 노래의 YouTube 제목과 song_answer 대표 정답 일괄 비교"""
        logger.info(f"Batch checking answer match for {len(song_ids)} songs")

        results = []
        summary = {"total": len(song_ids), "matched": 0, "partial": 0, "no_match": 0, "error": 0}

        for song_id in song_ids:
            try:
                result = self.check_answer_match(song_id)
                if "error" in result:
                    summary["error"] += 1
                    results.append({"song_id": song_id, "error": result["error"]})
                else:
                    match_status = result["match_result"]["match_status"]
                    if match_status == "matched":
                        summary["matched"] += 1
                    elif match_status == "partial":
                        summary["partial"] += 1
                    else:
                        summary["no_match"] += 1
                    results.append(result)
            except Exception as e:
                summary["error"] += 1
                results.append({"song_id": song_id, "error": str(e)})

        return {
            "summary": summary,
            "results": results
        }

    def get_agent_status(self) -> dict:
        """모든 에이전트 상태"""
        return {
            "DataManager": {
                "session": self.data_manager.get_session_progress(),
                "cache_size": len(self.data_manager._song_cache)
            },
            "YouTubeSearcher": self.youtube_searcher.get_cache_stats(),
            "DataVerifier": {
                "cache_size": len(self.data_verifier._verification_cache)
            },
            "SearchHelper": {
                "cache_size": len(self.search_helper._search_cache)
            },
            "DecisionAdvisor": self.decision_advisor.get_decision_summary(),
            "DBUpdater": self.db_updater.get_execution_summary()
        }
