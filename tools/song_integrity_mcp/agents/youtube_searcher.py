"""
Agent 2: YouTubeSearcher
youtube_video_id를 가지고 웹서칭을 통해 실제 정보를 검색
"""
import re
import json
import urllib.request
import urllib.parse
from typing import Optional, Dict, Tuple
import logging

import sys
sys.path.append('..')
from models import SongData, YouTubeInfo

logger = logging.getLogger("YouTubeSearcher")


class YouTubeSearcher:
    """YouTube 정보 검색 담당 에이전트"""

    def __init__(self):
        self._cache: Dict[str, YouTubeInfo] = {}

    def search_by_video_id(self, video_id: str) -> Optional[YouTubeInfo]:
        """YouTube Video ID로 정보 검색"""
        if video_id in self._cache:
            logger.info(f"Cache hit for video_id: {video_id}")
            return self._cache[video_id]

        logger.info(f"Searching YouTube for video_id: {video_id}")

        # 1. oEmbed API로 기본 정보 가져오기
        oembed_info = self._get_oembed_info(video_id)

        # 2. 썸네일 체크로 비디오 가용성 확인
        is_available = self._check_video_availability(video_id)

        if not oembed_info and not is_available:
            # 비디오가 존재하지 않음
            info = YouTubeInfo(
                video_id=video_id,
                title="[Video Unavailable]",
                channel_name="Unknown",
                is_available=False
            )
            self._cache[video_id] = info
            return info

        # 3. 기본 정보 구성
        title = oembed_info.get('title', '') if oembed_info else ''
        channel = oembed_info.get('author_name', '') if oembed_info else ''
        thumbnail = oembed_info.get('thumbnail_url', '') if oembed_info else f"https://img.youtube.com/vi/{video_id}/0.jpg"

        # 4. 제목에서 아티스트/곡명 파싱
        parsed_artist, parsed_title = self._parse_title(title)

        # 5. 연도 추출 시도
        parsed_year = self._extract_year(title)

        info = YouTubeInfo(
            video_id=video_id,
            title=title,
            channel_name=channel,
            is_available=is_available,
            thumbnail_url=thumbnail,
            parsed_artist=parsed_artist,
            parsed_title=parsed_title,
            parsed_year=parsed_year
        )

        self._cache[video_id] = info
        return info

    def _get_oembed_info(self, video_id: str) -> Optional[dict]:
        """oEmbed API로 비디오 정보 가져오기"""
        try:
            url = f"https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v={video_id}&format=json"
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=10) as response:
                return json.loads(response.read().decode('utf-8'))
        except urllib.error.HTTPError as e:
            if e.code == 404:
                logger.warning(f"Video not found via oEmbed: {video_id}")
            else:
                logger.error(f"oEmbed API error for {video_id}: {e}")
            return None
        except Exception as e:
            logger.error(f"Error fetching oEmbed for {video_id}: {e}")
            return None

    def _check_video_availability(self, video_id: str) -> bool:
        """썸네일 크기로 비디오 가용성 체크"""
        try:
            # maxresdefault 썸네일 체크
            url = f"https://img.youtube.com/vi/{video_id}/maxresdefault.jpg"
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=5) as response:
                # Content-Length로 실제 이미지인지 확인
                content_length = response.headers.get('Content-Length', '0')
                # 삭제된 영상의 기본 썸네일은 매우 작음
                if int(content_length) > 1000:
                    return True

            # hqdefault로 재시도
            url = f"https://img.youtube.com/vi/{video_id}/hqdefault.jpg"
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=5) as response:
                content_length = response.headers.get('Content-Length', '0')
                return int(content_length) > 1000

        except Exception as e:
            logger.warning(f"Thumbnail check failed for {video_id}: {e}")
            return False

    def _parse_title(self, title: str) -> Tuple[Optional[str], Optional[str]]:
        """YouTube 제목에서 아티스트와 곡명 파싱"""
        if not title:
            return None, None

        # 일반적인 패턴들
        patterns = [
            # "Artist - Title" 패턴
            r'^(.+?)\s*[-–—]\s*(.+?)(?:\s*[\(\[\|]|$)',
            # "Artist 'Title'" 패턴
            r'^(.+?)\s*[\'\""](.+?)[\'\""]',
            # "Title by Artist" 패턴
            r'^(.+?)\s+by\s+(.+?)(?:\s*[\(\[\|]|$)',
            # "[Artist] Title" 패턴
            r'^\[(.+?)\]\s*(.+?)(?:\s*[\(\[\|]|$)',
        ]

        for pattern in patterns:
            match = re.search(pattern, title, re.IGNORECASE)
            if match:
                part1, part2 = match.group(1).strip(), match.group(2).strip()
                # "by" 패턴은 순서가 다름
                if 'by' in pattern.lower():
                    return part2, part1
                return part1, part2

        return None, title

    def _extract_year(self, title: str) -> Optional[int]:
        """제목에서 연도 추출"""
        if not title:
            return None

        # 4자리 연도 패턴 (1950-2030)
        year_patterns = [
            r'\((\d{4})\)',      # (2020)
            r'\[(\d{4})\]',      # [2020]
            r'(\d{4})년',        # 2020년
            r'(\d{4})\s*ver',    # 2020 ver
        ]

        for pattern in year_patterns:
            match = re.search(pattern, title)
            if match:
                year = int(match.group(1))
                if 1950 <= year <= 2030:
                    return year

        return None

    def search_for_song(self, song: SongData) -> Optional[YouTubeInfo]:
        """SongData 객체로 YouTube 검색"""
        if not song.youtube_video_id:
            logger.warning(f"No YouTube ID for song: {song.title}")
            return None
        return self.search_by_video_id(song.youtube_video_id)

    def get_video_url(self, video_id: str) -> str:
        """YouTube 비디오 URL 생성"""
        return f"https://www.youtube.com/watch?v={video_id}"

    def get_thumbnail_url(self, video_id: str, quality: str = "hqdefault") -> str:
        """썸네일 URL 생성"""
        return f"https://img.youtube.com/vi/{video_id}/{quality}.jpg"

    def clear_cache(self):
        """캐시 초기화"""
        self._cache.clear()

    def get_cache_stats(self) -> dict:
        """캐시 통계"""
        available = sum(1 for v in self._cache.values() if v.is_available)
        return {
            "total_cached": len(self._cache),
            "available": available,
            "unavailable": len(self._cache) - available
        }
