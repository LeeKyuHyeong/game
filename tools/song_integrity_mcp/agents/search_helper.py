"""
Agent 5: SearchHelper
비교를 돕기위해 검색하고 검증을 도와주는 인원
"""
import re
import json
import urllib.request
import urllib.parse
from typing import Optional, List, Dict, Tuple
import logging

import sys
sys.path.append('..')
from models import SongData, YouTubeInfo

logger = logging.getLogger("SearchHelper")


class SearchHelper:
    """검색/검증 보조 담당 에이전트"""

    def __init__(self):
        self._search_cache: Dict[str, List[dict]] = {}

    def search_alternative_videos(
        self,
        artist: str,
        title: str,
        max_results: int = 5
    ) -> List[dict]:
        """대체 YouTube 영상 검색 (Lyrics 영상 우선)"""
        cache_key = f"{artist}_{title}"
        if cache_key in self._search_cache:
            return self._search_cache[cache_key]

        # Lyrics 영상 우선 검색
        query = f"{artist} {title} lyrics"
        logger.info(f"Searching for alternatives (lyrics priority): {query}")

        results = self._youtube_search(query, max_results * 2)  # 더 많이 검색

        # Lyrics 영상을 상위로 정렬
        results = self._sort_by_lyrics_priority(results)
        results = results[:max_results]

        self._search_cache[cache_key] = results
        return results

    def _sort_by_lyrics_priority(self, results: List[dict]) -> List[dict]:
        """Lyrics 영상을 우선순위로 정렬"""
        lyrics_videos = []
        other_videos = []

        lyrics_keywords = ['lyrics', 'lyric', '가사', 'color coded', 'han/rom/eng']
        official_keywords = ['official mv', 'official video', 'm/v', '뮤직비디오', 'music video']

        for video in results:
            title_lower = video.get('title', '').lower()

            # Lyrics 영상 판별
            is_lyrics = any(kw in title_lower for kw in lyrics_keywords)
            is_official_mv = any(kw in title_lower for kw in official_keywords)

            if is_lyrics and not is_official_mv:
                lyrics_videos.append(video)
            else:
                other_videos.append(video)

        # Lyrics 영상 먼저, 나머지는 뒤에
        return lyrics_videos + other_videos

    def _youtube_search(self, query: str, max_results: int = 5) -> List[dict]:
        """YouTube 검색 (HTML 파싱 방식)"""
        try:
            encoded_query = urllib.parse.quote(query)
            url = f"https://www.youtube.com/results?search_query={encoded_query}"

            req = urllib.request.Request(
                url,
                headers={
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                    'Accept-Language': 'ko-KR,ko;q=0.9,en;q=0.8'
                }
            )

            with urllib.request.urlopen(req, timeout=15) as response:
                html = response.read().decode('utf-8')

            # ytInitialData에서 검색 결과 추출
            match = re.search(r'var ytInitialData = ({.*?});</script>', html)
            if not match:
                match = re.search(r'ytInitialData\s*=\s*({.*?});', html)

            if not match:
                logger.warning("Could not find ytInitialData in response")
                return []

            data = json.loads(match.group(1))
            results = self._parse_search_results(data, max_results)
            return results

        except Exception as e:
            logger.error(f"YouTube search error: {e}")
            return []

    def _parse_search_results(self, data: dict, max_results: int) -> List[dict]:
        """YouTube 검색 결과 파싱"""
        results = []

        try:
            contents = (
                data.get('contents', {})
                .get('twoColumnSearchResultsRenderer', {})
                .get('primaryContents', {})
                .get('sectionListRenderer', {})
                .get('contents', [])
            )

            for section in contents:
                items = (
                    section.get('itemSectionRenderer', {})
                    .get('contents', [])
                )

                for item in items:
                    video = item.get('videoRenderer', {})
                    if not video:
                        continue

                    video_id = video.get('videoId')
                    if not video_id:
                        continue

                    title_runs = video.get('title', {}).get('runs', [])
                    title = ''.join(r.get('text', '') for r in title_runs)

                    channel_runs = (
                        video.get('ownerText', {})
                        .get('runs', [])
                    )
                    channel = ''.join(r.get('text', '') for r in channel_runs)

                    # 조회수
                    view_text = video.get('viewCountText', {}).get('simpleText', '')
                    views = self._parse_view_count(view_text)

                    # 길이
                    length_text = video.get('lengthText', {}).get('simpleText', '')
                    duration = self._parse_duration(length_text)

                    results.append({
                        'video_id': video_id,
                        'title': title,
                        'channel': channel,
                        'views': views,
                        'duration': duration,
                        'url': f"https://www.youtube.com/watch?v={video_id}"
                    })

                    if len(results) >= max_results:
                        break

                if len(results) >= max_results:
                    break

        except Exception as e:
            logger.error(f"Parse error: {e}")

        return results

    def _parse_view_count(self, text: str) -> Optional[int]:
        """조회수 텍스트 파싱"""
        if not text:
            return None

        # "조회수 1,234회" 또는 "1.2M views" 등
        text = text.replace(',', '').replace(' ', '')

        multiplier = 1
        if 'k' in text.lower() or '천' in text:
            multiplier = 1000
        elif 'm' in text.lower() or '만' in text:
            multiplier = 10000
        elif 'b' in text.lower() or '억' in text:
            multiplier = 100000000

        numbers = re.findall(r'[\d.]+', text)
        if numbers:
            try:
                return int(float(numbers[0]) * multiplier)
            except:
                pass
        return None

    def _parse_duration(self, text: str) -> Optional[int]:
        """재생시간 텍스트 파싱 (초 단위)"""
        if not text:
            return None

        # "3:45" 또는 "1:23:45"
        parts = text.split(':')
        try:
            if len(parts) == 2:
                return int(parts[0]) * 60 + int(parts[1])
            elif len(parts) == 3:
                return int(parts[0]) * 3600 + int(parts[1]) * 60 + int(parts[2])
        except:
            pass
        return None

    def verify_video_exists(self, video_id: str) -> Tuple[bool, str]:
        """비디오 존재 여부 확인"""
        try:
            url = f"https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v={video_id}&format=json"
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=10) as response:
                data = json.loads(response.read().decode('utf-8'))
                return True, data.get('title', 'Unknown')
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return False, "Video not found"
            return False, f"HTTP Error: {e.code}"
        except Exception as e:
            return False, str(e)

    def find_best_match(
        self,
        song: SongData,
        alternatives: List[dict]
    ) -> Optional[dict]:
        """가장 적합한 대체 영상 찾기"""
        if not alternatives:
            return None

        best_match = None
        best_score = 0

        for alt in alternatives:
            score = self._calculate_match_score(song, alt)
            if score > best_score:
                best_score = score
                best_match = alt

        if best_score > 0.5:  # 최소 50% 이상 일치
            return best_match
        return None

    def _calculate_match_score(self, song: SongData, candidate: dict) -> float:
        """매칭 점수 계산 (Lyrics 영상 우선)"""
        score = 0.0

        title = candidate.get('title', '').lower()
        channel = candidate.get('channel', '').lower()

        # 아티스트 매칭 (30%)
        artist_lower = song.artist.lower()
        if artist_lower in title or artist_lower in channel:
            score += 0.3
        elif any(word in title or word in channel for word in artist_lower.split()):
            score += 0.15

        # 제목 매칭 (30%)
        title_lower = song.title.lower()
        if title_lower in title:
            score += 0.3
        elif any(word in title for word in title_lower.split() if len(word) > 1):
            score += 0.15

        # Lyrics 영상 보너스 (25%) - 가장 중요!
        lyrics_keywords = ['lyrics', 'lyric', '가사', 'color coded', 'han/rom/eng']
        if any(kw in title for kw in lyrics_keywords):
            score += 0.25

        # 공식 MV 페널티 (-10%) - 공식 MV는 피함
        official_mv_keywords = ['official mv', 'official video', 'm/v', '뮤직비디오', 'music video']
        if any(kw in title for kw in official_mv_keywords):
            score -= 0.1

        # 유명 가사 채널 보너스 (10%)
        lyrics_channels = ['jaeguchi', '7clouds', 'syrebralvibes', 'cakes & eclairs',
                          'lyrics', '가사', 'dan music', 'unique vibes']
        if any(ch in channel for ch in lyrics_channels):
            score += 0.1

        # 조회수 보너스 (5%)
        views = candidate.get('views', 0)
        if views and views > 1000000:
            score += 0.05
        elif views and views > 100000:
            score += 0.025

        return min(max(score, 0.0), 1.0)

    def get_video_info_from_search(self, video_id: str, alternatives: List[dict]) -> Optional[dict]:
        """검색 결과에서 특정 비디오 정보 찾기"""
        for alt in alternatives:
            if alt.get('video_id') == video_id:
                return alt
        return None

    def suggest_search_queries(self, song: SongData) -> List[str]:
        """검색 쿼리 제안 (Lyrics 우선)"""
        queries = [
            f"{song.artist} {song.title} lyrics",
            f"{song.artist} {song.title} 가사",
            f"{song.artist} {song.title} color coded",
            f"{song.artist} {song.title}",
            f"{song.title} {song.artist} lyrics",
        ]

        if song.release_year:
            queries.append(f"{song.artist} {song.title} {song.release_year} lyrics")

        return queries

    def clear_cache(self):
        """캐시 초기화"""
        self._search_cache.clear()
