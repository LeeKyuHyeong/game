"""
Agent 1: DataManager
현재의 아티스트, 제목, 연도, 솔로여부, 노래재생시간, youtube_video_id를 저장하고 관리
"""
import pymysql
from typing import List, Optional, Dict
from datetime import datetime
import logging

import sys
sys.path.append('..')
from config import DB_CONFIG
from models import SongData, IntegrityCheckSession

logger = logging.getLogger("DataManager")


class DataManager:
    """DB 데이터 저장/관리 담당 에이전트"""

    def __init__(self):
        self.db_config = DB_CONFIG
        self._current_session: Optional[IntegrityCheckSession] = None
        self._song_cache: Dict[int, SongData] = {}

    def _get_connection(self):
        """DB 연결 생성"""
        return pymysql.connect(**self.db_config)

    def get_all_songs(self) -> List[SongData]:
        """모든 노래 데이터 조회"""
        conn = self._get_connection()
        try:
            cursor = conn.cursor(pymysql.cursors.DictCursor)
            cursor.execute("""
                SELECT id, artist, title, release_year, is_solo,
                       play_duration, youtube_video_id, genre_id, created_at
                FROM song
                ORDER BY id
            """)
            rows = cursor.fetchall()
            songs = []
            for row in rows:
                song = SongData(
                    id=row['id'],
                    artist=row['artist'],
                    title=row['title'],
                    release_year=row['release_year'],
                    is_solo=bool(row['is_solo']),
                    play_duration=row['play_duration'],
                    youtube_video_id=row['youtube_video_id'],
                    genre_id=row['genre_id'],
                    created_at=row['created_at']
                )
                songs.append(song)
                self._song_cache[song.id] = song
            logger.info(f"Loaded {len(songs)} songs from database")
            return songs
        finally:
            conn.close()

    def get_song_by_id(self, song_id: int) -> Optional[SongData]:
        """특정 노래 데이터 조회"""
        if song_id in self._song_cache:
            return self._song_cache[song_id]

        conn = self._get_connection()
        try:
            cursor = conn.cursor(pymysql.cursors.DictCursor)
            cursor.execute("""
                SELECT id, artist, title, release_year, is_solo,
                       play_duration, youtube_video_id, genre_id, created_at
                FROM song WHERE id = %s
            """, (song_id,))
            row = cursor.fetchone()
            if row:
                song = SongData(
                    id=row['id'],
                    artist=row['artist'],
                    title=row['title'],
                    release_year=row['release_year'],
                    is_solo=bool(row['is_solo']),
                    play_duration=row['play_duration'],
                    youtube_video_id=row['youtube_video_id'],
                    genre_id=row['genre_id'],
                    created_at=row['created_at']
                )
                self._song_cache[song_id] = song
                return song
            return None
        finally:
            conn.close()

    def get_songs_by_ids(self, song_ids: List[int]) -> List[SongData]:
        """여러 노래 데이터 조회"""
        songs = []
        for song_id in song_ids:
            song = self.get_song_by_id(song_id)
            if song:
                songs.append(song)
        return songs

    def get_songs_with_youtube_id(self) -> List[SongData]:
        """YouTube ID가 있는 노래만 조회"""
        conn = self._get_connection()
        try:
            cursor = conn.cursor(pymysql.cursors.DictCursor)
            cursor.execute("""
                SELECT id, artist, title, release_year, is_solo,
                       play_duration, youtube_video_id, genre_id, created_at
                FROM song
                WHERE youtube_video_id IS NOT NULL
                  AND youtube_video_id != ''
                ORDER BY id
            """)
            rows = cursor.fetchall()
            songs = []
            for row in rows:
                song = SongData(
                    id=row['id'],
                    artist=row['artist'],
                    title=row['title'],
                    release_year=row['release_year'],
                    is_solo=bool(row['is_solo']),
                    play_duration=row['play_duration'],
                    youtube_video_id=row['youtube_video_id'],
                    genre_id=row['genre_id'],
                    created_at=row['created_at']
                )
                songs.append(song)
                self._song_cache[song.id] = song
            logger.info(f"Loaded {len(songs)} songs with YouTube IDs")
            return songs
        finally:
            conn.close()

    def create_session(self, song_ids: Optional[List[int]] = None) -> IntegrityCheckSession:
        """정합성 체크 세션 생성"""
        import uuid
        session_id = str(uuid.uuid4())[:8]

        if song_ids is None:
            songs = self.get_songs_with_youtube_id()
            song_ids = [s.id for s in songs]

        self._current_session = IntegrityCheckSession(
            session_id=session_id,
            started_at=datetime.now(),
            songs_to_check=song_ids
        )
        logger.info(f"Created session {session_id} with {len(song_ids)} songs")
        return self._current_session

    def get_current_session(self) -> Optional[IntegrityCheckSession]:
        """현재 세션 반환"""
        return self._current_session

    def get_next_song(self) -> Optional[SongData]:
        """세션에서 다음 체크할 노래 반환"""
        if not self._current_session:
            return None

        if self._current_session.current_index >= len(self._current_session.songs_to_check):
            self._current_session.completed = True
            return None

        song_id = self._current_session.songs_to_check[self._current_session.current_index]
        return self.get_song_by_id(song_id)

    def advance_session(self):
        """세션 진행 (다음 노래로)"""
        if self._current_session:
            self._current_session.current_index += 1

    def get_session_progress(self) -> dict:
        """세션 진행 상황"""
        if not self._current_session:
            return {"status": "no_session"}

        return {
            "session_id": self._current_session.session_id,
            "current": self._current_session.current_index + 1,
            "total": len(self._current_session.songs_to_check),
            "completed": self._current_session.completed,
            "progress_percent": round(
                (self._current_session.current_index / len(self._current_session.songs_to_check)) * 100, 1
            ) if self._current_session.songs_to_check else 0
        }

    def clear_cache(self):
        """캐시 초기화"""
        self._song_cache.clear()

    def get_song_answers(self, song_id: int) -> dict:
        """노래의 정답 정보 조회"""
        conn = self._get_connection()
        try:
            cursor = conn.cursor(pymysql.cursors.DictCursor)
            cursor.execute("""
                SELECT id, song_id, answer, is_primary, created_at
                FROM song_answer
                WHERE song_id = %s
                ORDER BY is_primary DESC, id
            """, (song_id,))
            rows = cursor.fetchall()

            primary_answer = None
            all_answers = []

            for row in rows:
                all_answers.append(row['answer'])
                if row['is_primary'] == 1:
                    primary_answer = row['answer']

            return {
                "song_id": song_id,
                "primary_answer": primary_answer,
                "all_answers": all_answers,
                "answer_count": len(all_answers)
            }
        finally:
            conn.close()

    def get_songs_answers_batch(self, song_ids) -> dict:
        """여러 노래의 정답 정보 일괄 조회"""
        if not song_ids:
            return {}

        conn = self._get_connection()
        try:
            cursor = conn.cursor(pymysql.cursors.DictCursor)
            placeholders = ','.join(['%s'] * len(song_ids))
            cursor.execute(f"""
                SELECT id, song_id, answer, is_primary, created_at
                FROM song_answer
                WHERE song_id IN ({placeholders})
                ORDER BY song_id, is_primary DESC, id
            """, song_ids)
            rows = cursor.fetchall()

            result = {sid: {"song_id": sid, "primary_answer": None, "all_answers": [], "answer_count": 0}
                      for sid in song_ids}

            for row in rows:
                sid = row['song_id']
                result[sid]["all_answers"].append(row['answer'])
                if row['is_primary'] == 1:
                    result[sid]["primary_answer"] = row['answer']

            for sid in result:
                result[sid]["answer_count"] = len(result[sid]["all_answers"])

            return result
        finally:
            conn.close()

    def get_statistics(self) -> dict:
        """DB 통계 정보"""
        conn = self._get_connection()
        try:
            cursor = conn.cursor(pymysql.cursors.DictCursor)

            # 전체 노래 수
            cursor.execute("SELECT COUNT(*) as total FROM song")
            total = cursor.fetchone()['total']

            # YouTube ID 있는 노래 수
            cursor.execute("""
                SELECT COUNT(*) as with_youtube
                FROM song
                WHERE youtube_video_id IS NOT NULL AND youtube_video_id != ''
            """)
            with_youtube = cursor.fetchone()['with_youtube']

            # 솔로/그룹 분포
            cursor.execute("""
                SELECT is_solo, COUNT(*) as count
                FROM song
                GROUP BY is_solo
            """)
            solo_dist = {row['is_solo']: row['count'] for row in cursor.fetchall()}

            # 연도별 분포
            cursor.execute("""
                SELECT release_year, COUNT(*) as count
                FROM song
                WHERE release_year IS NOT NULL
                GROUP BY release_year
                ORDER BY release_year DESC
                LIMIT 10
            """)
            year_dist = {row['release_year']: row['count'] for row in cursor.fetchall()}

            return {
                "total_songs": total,
                "songs_with_youtube": with_youtube,
                "songs_without_youtube": total - with_youtube,
                "solo_songs": solo_dist.get(1, 0),
                "group_songs": solo_dist.get(0, 0),
                "top_years": year_dist
            }
        finally:
            conn.close()
