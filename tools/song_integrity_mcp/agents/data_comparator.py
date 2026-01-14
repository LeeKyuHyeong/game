"""
Agent 4: DataComparator
현재의 데이터와 실제 정보를 비교 (Lyrics 영상 우선)
"""
import re
from typing import Optional, List, Tuple
import logging
from difflib import SequenceMatcher

import sys
sys.path.append('..')
from models import (
    SongData, YouTubeInfo, VerificationResult,
    ComparisonResult, VerificationStatus
)

logger = logging.getLogger("DataComparator")


class DataComparator:
    """데이터 비교 담당 에이전트 (Lyrics 영상 우선)"""

    TITLE_SIMILARITY_THRESHOLD = 0.6
    ARTIST_SIMILARITY_THRESHOLD = 0.6
    YEAR_TOLERANCE = 1
    DURATION_TOLERANCE = 5

    OFFICIAL_MV_KEYWORDS = ["official mv", "official video", "m/v", "뮤직비디오",
                           "music video", "official music", "official m/v"]
    LYRICS_KEYWORDS = ["lyrics", "lyric", "가사", "color coded", "han/rom/eng"]

    def compare(self, song_data: SongData, youtube_info: Optional[YouTubeInfo],
                verification_result: Optional[VerificationResult] = None) -> ComparisonResult:
        logger.info(f"Comparing song ID {song_data.id}: {song_data.artist} - {song_data.title}")
        result = ComparisonResult(song_data=song_data, youtube_info=youtube_info, verification_result=verification_result)

        if not youtube_info:
            result.status = VerificationStatus.ERROR
            result.differences.append("YouTube 정보를 가져올 수 없음")
            result.recommendations.append("YouTube Video ID 확인 필요")
            return result

        if not youtube_info.is_available:
            result.status = VerificationStatus.VIDEO_UNAVAILABLE
            result.differences.append("YouTube 비디오가 삭제되었거나 비공개 상태")
            result.recommendations.append("새로운 YouTube Video ID 필요")
            return result

        result.artist_match = self._compare_artist(song_data, youtube_info)
        result.title_match = self._compare_title(song_data, youtube_info)
        result.year_match = self._compare_year(song_data, youtube_info)
        result.duration_match = self._compare_duration(song_data, youtube_info)

        self._collect_differences(result, song_data, youtube_info)

        is_official_mv = self._is_official_mv(youtube_info.title)
        is_lyrics = self._is_lyrics_video(youtube_info.title)

        if is_official_mv and not is_lyrics:
            result.status = VerificationStatus.MISMATCH
            result.differences.append("현재 영상이 공식 MV입니다 - Lyrics 영상으로 교체 권장")
        elif result.artist_match and result.title_match:
            result.status = VerificationStatus.VERIFIED
        else:
            result.status = VerificationStatus.MISMATCH

        if verification_result and verification_result.confidence < 0.5:
            result.recommendations.append("검증 신뢰도가 낮음 - 수동 확인 권장")

        self._generate_recommendations(result, is_official_mv, is_lyrics)
        return result

    def _is_official_mv(self, title: str) -> bool:
        if not title:
            return False
        title_lower = title.lower()
        return any(kw in title_lower for kw in self.OFFICIAL_MV_KEYWORDS)

    def _is_lyrics_video(self, title: str) -> bool:
        if not title:
            return False
        title_lower = title.lower()
        return any(kw in title_lower for kw in self.LYRICS_KEYWORDS)

    def _compare_artist(self, song: SongData, youtube: YouTubeInfo) -> bool:
        if not youtube.parsed_artist:
            return self._is_similar(song.artist, youtube.channel_name, self.ARTIST_SIMILARITY_THRESHOLD)
        return self._is_similar(song.artist, youtube.parsed_artist, self.ARTIST_SIMILARITY_THRESHOLD)

    def _compare_title(self, song: SongData, youtube: YouTubeInfo) -> bool:
        if youtube.parsed_title:
            if self._is_similar(song.title, youtube.parsed_title, self.TITLE_SIMILARITY_THRESHOLD):
                return True
        return self._is_similar(song.title, youtube.title, self.TITLE_SIMILARITY_THRESHOLD)

    def _compare_year(self, song: SongData, youtube: YouTubeInfo) -> bool:
        if song.release_year is None or youtube.parsed_year is None:
            return True
        return abs(song.release_year - youtube.parsed_year) <= self.YEAR_TOLERANCE

    def _compare_duration(self, song: SongData, youtube: YouTubeInfo) -> bool:
        if song.play_duration is None or youtube.duration is None:
            return True
        return abs(song.play_duration - youtube.duration) <= self.DURATION_TOLERANCE

    def _is_similar(self, s1: str, s2: str, threshold: float) -> bool:
        if not s1 or not s2:
            return False
        s1_norm = self._normalize(s1)
        s2_norm = self._normalize(s2)
        if s1_norm == s2_norm:
            return True
        if s1_norm in s2_norm or s2_norm in s1_norm:
            return True
        from difflib import SequenceMatcher
        return SequenceMatcher(None, s1_norm, s2_norm).ratio() >= threshold

    def _normalize(self, s: str) -> str:
        s = s.lower()
        s = re.sub(r"\([^)]*\)", "", s)
        s = re.sub(r"\[[^\]]*\]", "", s)
        s = re.sub(r"[^\w\s가-힣]", "", s)
        s = re.sub(r"\s+", " ", s).strip()
        return s

    def _collect_differences(self, result: ComparisonResult, song: SongData, youtube: YouTubeInfo):
        if not result.artist_match:
            parsed = youtube.parsed_artist or youtube.channel_name
            result.differences.append(f"아티스트 불일치: DB[{song.artist}] vs YouTube[{parsed}]")
        if not result.title_match:
            parsed = youtube.parsed_title or youtube.title
            result.differences.append(f"제목 불일치: DB[{song.title}] vs YouTube[{parsed}]")

    def _generate_recommendations(self, result: ComparisonResult, is_official_mv: bool = False, is_lyrics: bool = False):
        if is_official_mv and not is_lyrics:
            result.recommendations.append("현재 공식 MV 영상입니다")
            result.recommendations.append("Lyrics 영상으로 교체를 권장합니다")
            return
        if result.status == VerificationStatus.VERIFIED:
            if is_lyrics:
                result.recommendations.append("Lyrics 영상 - 정합성 확인됨")
            else:
                result.recommendations.append("정합성 확인됨")
        elif result.status == VerificationStatus.MISMATCH:
            result.recommendations.append("Lyrics 영상으로 교체 권장")
        elif result.status == VerificationStatus.VIDEO_UNAVAILABLE:
            result.recommendations.append("Lyrics 영상 검색 권장")

    def get_similarity_score(self, s1: str, s2: str) -> float:
        if not s1 or not s2:
            return 0.0
        return SequenceMatcher(None, self._normalize(s1), self._normalize(s2)).ratio()

    def batch_compare(self, comparisons):
        return [self.compare(s, y, v) for s, y, v in comparisons]

    def get_summary(self, results) -> dict:
        summary = {"total": len(results), "verified": 0, "mismatch": 0, "video_unavailable": 0, "error": 0,
                   "official_mv_count": 0, "lyrics_count": 0}
        for r in results:
            if r.status == VerificationStatus.VERIFIED:
                summary["verified"] += 1
            elif r.status == VerificationStatus.MISMATCH:
                summary["mismatch"] += 1
            elif r.status == VerificationStatus.VIDEO_UNAVAILABLE:
                summary["video_unavailable"] += 1
            else:
                summary["error"] += 1
            if r.youtube_info:
                if self._is_official_mv(r.youtube_info.title):
                    summary["official_mv_count"] += 1
                if self._is_lyrics_video(r.youtube_info.title):
                    summary["lyrics_count"] += 1
        return summary
