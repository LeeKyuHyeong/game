"""
Agent 7: DBUpdater
다 정리된 데이터를 실제로 운영DB에 적용할 인원
"""
import pymysql
from typing import List, Optional, Dict
from datetime import datetime
import logging

import sys
sys.path.append('..')
from config import DB_CONFIG
from models import UserDecision, DecisionType, SongData

logger = logging.getLogger("DBUpdater")


class DBUpdater:
    """운영 DB 적용 담당 에이전트"""

    def __init__(self):
        self.db_config = DB_CONFIG
        self._execution_log: List[dict] = []
        self._dry_run_mode = True  # 기본값은 안전 모드

    def _get_connection(self):
        """DB 연결 생성"""
        return pymysql.connect(**self.db_config)

    def set_dry_run(self, enabled: bool):
        """Dry-run 모드 설정"""
        self._dry_run_mode = enabled
        logger.info(f"Dry-run mode: {'ENABLED' if enabled else 'DISABLED'}")

    def apply_decision(self, decision: UserDecision) -> dict:
        """단일 결정 적용"""
        result = {
            "song_id": decision.song_id,
            "action": decision.decision_type.value,
            "success": False,
            "message": "",
            "dry_run": self._dry_run_mode,
            "timestamp": datetime.now().isoformat()
        }

        try:
            if decision.decision_type == DecisionType.KEEP:
                result["success"] = True
                result["message"] = "No changes needed"

            elif decision.decision_type == DecisionType.SKIP:
                result["success"] = True
                result["message"] = "Skipped"

            elif decision.decision_type == DecisionType.UPDATE:
                result = self._apply_update(decision, result)

            elif decision.decision_type == DecisionType.DELETE:
                result = self._apply_delete(decision, result)

            elif decision.decision_type == DecisionType.REPLACE:
                result = self._apply_replace(decision, result)

        except Exception as e:
            result["success"] = False
            result["message"] = f"Error: {str(e)}"
            logger.error(f"Error applying decision for song {decision.song_id}: {e}")

        self._execution_log.append(result)
        return result

    def _apply_update(self, decision: UserDecision, result: dict) -> dict:
        """UPDATE 적용"""
        if not decision.update_fields:
            result["message"] = "No update fields specified"
            return result

        allowed_fields = {'artist', 'title', 'release_year', 'is_solo', 'play_duration', 'youtube_video_id'}
        fields_to_update = {k: v for k, v in decision.update_fields.items() if k in allowed_fields}

        if not fields_to_update:
            result["message"] = "No valid fields to update"
            return result

        # SET 절 생성
        set_clause = ", ".join([f"{field} = %s" for field in fields_to_update.keys()])
        values = list(fields_to_update.values()) + [decision.song_id]

        sql = f"UPDATE song SET {set_clause} WHERE id = %s"

        if self._dry_run_mode:
            result["success"] = True
            result["message"] = f"[DRY-RUN] Would execute: {sql}"
            result["sql"] = sql
            result["values"] = values
            logger.info(f"[DRY-RUN] Update song {decision.song_id}: {fields_to_update}")
        else:
            conn = self._get_connection()
            try:
                cursor = conn.cursor()
                cursor.execute(sql, values)
                conn.commit()
                result["success"] = True
                result["message"] = f"Updated {cursor.rowcount} row(s)"
                result["affected_rows"] = cursor.rowcount
                logger.info(f"Updated song {decision.song_id}: {fields_to_update}")
            finally:
                conn.close()

        return result

    def _apply_delete(self, decision: UserDecision, result: dict) -> dict:
        """DELETE 적용"""
        sql = "DELETE FROM song WHERE id = %s"

        if self._dry_run_mode:
            result["success"] = True
            result["message"] = f"[DRY-RUN] Would execute: {sql}"
            result["sql"] = sql
            result["values"] = [decision.song_id]
            logger.info(f"[DRY-RUN] Delete song {decision.song_id}")
        else:
            conn = self._get_connection()
            try:
                # 관련 데이터 먼저 삭제 (song_answer)
                cursor = conn.cursor()
                cursor.execute("DELETE FROM song_answer WHERE song_id = %s", (decision.song_id,))
                cursor.execute(sql, (decision.song_id,))
                conn.commit()
                result["success"] = True
                result["message"] = f"Deleted song and {cursor.rowcount} related records"
                logger.info(f"Deleted song {decision.song_id}")
            finally:
                conn.close()

        return result

    def _apply_replace(self, decision: UserDecision, result: dict) -> dict:
        """YouTube ID 교체 적용"""
        if not decision.new_youtube_id:
            result["message"] = "No new YouTube ID specified"
            return result

        sql = "UPDATE song SET youtube_video_id = %s WHERE id = %s"
        values = [decision.new_youtube_id, decision.song_id]

        if self._dry_run_mode:
            result["success"] = True
            result["message"] = f"[DRY-RUN] Would execute: {sql}"
            result["sql"] = sql
            result["values"] = values
            result["new_youtube_id"] = decision.new_youtube_id
            logger.info(f"[DRY-RUN] Replace YouTube ID for song {decision.song_id}: {decision.new_youtube_id}")
        else:
            conn = self._get_connection()
            try:
                cursor = conn.cursor()
                cursor.execute(sql, values)
                conn.commit()
                result["success"] = True
                result["message"] = f"Replaced YouTube ID (affected {cursor.rowcount} row)"
                result["new_youtube_id"] = decision.new_youtube_id
                logger.info(f"Replaced YouTube ID for song {decision.song_id}: {decision.new_youtube_id}")
            finally:
                conn.close()

        return result

    def apply_decisions_batch(self, decisions: List[UserDecision]) -> List[dict]:
        """여러 결정 일괄 적용"""
        results = []
        for decision in decisions:
            result = self.apply_decision(decision)
            results.append(result)
        return results

    def preview_changes(self, decisions: List[UserDecision]) -> dict:
        """변경 사항 미리보기"""
        preview = {
            "total": len(decisions),
            "keep": [],
            "update": [],
            "delete": [],
            "replace": [],
            "skip": []
        }

        for decision in decisions:
            entry = {
                "song_id": decision.song_id,
                "reason": decision.reason
            }

            if decision.decision_type == DecisionType.UPDATE:
                entry["fields"] = decision.update_fields
            elif decision.decision_type == DecisionType.REPLACE:
                entry["new_youtube_id"] = decision.new_youtube_id

            preview[decision.decision_type.value].append(entry)

        return preview

    def rollback_last(self) -> dict:
        """마지막 실행 롤백 (제한적)"""
        if not self._execution_log:
            return {"success": False, "message": "No execution log found"}

        last = self._execution_log[-1]
        if last.get("dry_run"):
            return {"success": False, "message": "Cannot rollback dry-run"}

        # 롤백은 복잡하므로 현재는 지원하지 않음
        return {
            "success": False,
            "message": "Rollback not supported. Please restore from backup.",
            "last_action": last
        }

    def get_execution_log(self) -> List[dict]:
        """실행 로그 반환"""
        return self._execution_log

    def get_execution_summary(self) -> dict:
        """실행 요약"""
        summary = {
            "total_executions": len(self._execution_log),
            "successful": 0,
            "failed": 0,
            "dry_run_count": 0,
            "by_action": {
                "keep": 0,
                "update": 0,
                "delete": 0,
                "replace": 0,
                "skip": 0
            }
        }

        for log in self._execution_log:
            if log.get("success"):
                summary["successful"] += 1
            else:
                summary["failed"] += 1

            if log.get("dry_run"):
                summary["dry_run_count"] += 1

            action = log.get("action", "unknown")
            if action in summary["by_action"]:
                summary["by_action"][action] += 1

        return summary

    def clear_log(self):
        """실행 로그 초기화"""
        self._execution_log.clear()

    def backup_song(self, song_id: int) -> Optional[dict]:
        """특정 노래 백업"""
        conn = self._get_connection()
        try:
            cursor = conn.cursor(pymysql.cursors.DictCursor)
            cursor.execute("SELECT * FROM song WHERE id = %s", (song_id,))
            row = cursor.fetchone()
            if row:
                # datetime 객체를 문자열로 변환
                for key, value in row.items():
                    if isinstance(value, datetime):
                        row[key] = value.isoformat()
                return row
            return None
        finally:
            conn.close()

    def restore_song(self, backup_data: dict) -> dict:
        """백업에서 노래 복원"""
        if self._dry_run_mode:
            return {
                "success": True,
                "message": "[DRY-RUN] Would restore song from backup",
                "backup_data": backup_data
            }

        # 복원 로직 (INSERT OR UPDATE)
        conn = self._get_connection()
        try:
            cursor = conn.cursor()

            # 기존 레코드 확인
            cursor.execute("SELECT id FROM song WHERE id = %s", (backup_data['id'],))
            exists = cursor.fetchone()

            if exists:
                # UPDATE
                fields = [k for k in backup_data.keys() if k != 'id']
                set_clause = ", ".join([f"{f} = %s" for f in fields])
                values = [backup_data[f] for f in fields] + [backup_data['id']]
                cursor.execute(f"UPDATE song SET {set_clause} WHERE id = %s", values)
            else:
                # INSERT
                fields = list(backup_data.keys())
                placeholders = ", ".join(["%s"] * len(fields))
                values = [backup_data[f] for f in fields]
                cursor.execute(
                    f"INSERT INTO song ({', '.join(fields)}) VALUES ({placeholders})",
                    values
                )

            conn.commit()
            return {"success": True, "message": "Song restored from backup"}
        except Exception as e:
            return {"success": False, "message": f"Restore failed: {e}"}
        finally:
            conn.close()
