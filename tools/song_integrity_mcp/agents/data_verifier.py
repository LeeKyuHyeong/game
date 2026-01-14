"""
Agent 3: DataVerifier
검색하는 인원의 데이터를 다시 한번 웹서칭을 통해 정합성을 올릴 검증
"""
import re
import json
import urllib.request
from typing import Optional, List, Tuple
import logging

import sys
sys.path.append('..')
from models import YouTubeInfo, VerificationResult

logger = logging.getLogger("DataVerifier")


class DataVerifier:
    """검색 결과 검증 담당 에이전트"""

    def __init__(self):
        self._verification_cache = {}

    def verify(self, youtube_info: YouTubeInfo) -> VerificationResult:
        """YouTube 정보 검증"""
        if youtube_info.video_id in self._verification_cache:
            return self._verification_cache[youtube_info.video_id]

        logger.info(f"Verifying video: {youtube_info.video_id}")

        notes = []
        confidence = 0.0
        verification_source = "multiple_sources"

        # 1. 비디오 가용성 검증
        if not youtube_info.is_available:
            result = VerificationResult(
                original_info=youtube_info,
                verified=False,
                confidence=1.0,
                verification_source="availability_check",
                notes=["Video is unavailable or deleted"]
            )
            self._verification_cache[youtube_info.video_id] = result
            return result

        # 2. oEmbed 재검증
        oembed_verified, oembed_note = self._verify_via_oembed(youtube_info.video_id)
        notes.append(oembed_note)
        if oembed_verified:
            confidence += 0.3

        # 3. 썸네일 다중 해상도 체크
        thumbnail_verified, thumbnail_note = self._verify_thumbnails(youtube_info.video_id)
        notes.append(thumbnail_note)
        if thumbnail_verified:
            confidence += 0.2

        # 4. 제목 파싱 품질 검증
        parsing_score, parsing_notes = self._verify_title_parsing(youtube_info)
        notes.extend(parsing_notes)
        confidence += parsing_score * 0.3

        # 5. 채널명 신뢰도 체크
        channel_score, channel_note = self._verify_channel(youtube_info)
        notes.append(channel_note)
        confidence += channel_score * 0.2

        verified = confidence >= 0.5

        result = VerificationResult(
            original_info=youtube_info,
            verified=verified,
            confidence=min(confidence, 1.0),
            verification_source=verification_source,
            notes=notes
        )

        self._verification_cache[youtube_info.video_id] = result
        return result

    def _verify_via_oembed(self, video_id: str) -> Tuple[bool, str]:
        """oEmbed API로 재검증"""
        try:
            url = f"https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v={video_id}&format=json"
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=10) as response:
                data = json.loads(response.read().decode('utf-8'))
                if data.get('title') and data.get('author_name'):
                    return True, "oEmbed verification: PASSED"
                return False, "oEmbed verification: incomplete data"
        except urllib.error.HTTPError as e:
            return False, f"oEmbed verification: FAILED (HTTP {e.code})"
        except Exception as e:
            return False, f"oEmbed verification: ERROR ({str(e)[:50]})"

    def _verify_thumbnails(self, video_id: str) -> Tuple[bool, str]:
        """다중 해상도 썸네일 검증"""
        qualities = ['maxresdefault', 'sddefault', 'hqdefault', 'mqdefault', 'default']
        available_count = 0

        for quality in qualities:
            try:
                url = f"https://img.youtube.com/vi/{video_id}/{quality}.jpg"
                req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
                with urllib.request.urlopen(req, timeout=5) as response:
                    content_length = int(response.headers.get('Content-Length', '0'))
                    if content_length > 1000:
                        available_count += 1
            except:
                pass

        if available_count >= 3:
            return True, f"Thumbnail verification: PASSED ({available_count}/{len(qualities)} available)"
        elif available_count >= 1:
            return True, f"Thumbnail verification: PARTIAL ({available_count}/{len(qualities)} available)"
        else:
            return False, "Thumbnail verification: FAILED (no valid thumbnails)"

    def _verify_title_parsing(self, info: YouTubeInfo) -> Tuple[float, List[str]]:
        """제목 파싱 품질 검증"""
        notes = []
        score = 0.0

        if not info.title:
            return 0.0, ["Title parsing: NO TITLE"]

        # 파싱된 아티스트가 있는지
        if info.parsed_artist:
            score += 0.4
            notes.append(f"Parsed artist: {info.parsed_artist}")
        else:
            notes.append("Parsed artist: NOT FOUND")

        # 파싱된 제목이 있는지
        if info.parsed_title:
            score += 0.4
            notes.append(f"Parsed title: {info.parsed_title}")
        else:
            notes.append("Parsed title: NOT FOUND")

        # 연도가 추출되었는지
        if info.parsed_year:
            score += 0.2
            notes.append(f"Parsed year: {info.parsed_year}")

        return score, notes

    def _verify_channel(self, info: YouTubeInfo) -> Tuple[float, str]:
        """채널명 신뢰도 검증"""
        if not info.channel_name:
            return 0.0, "Channel verification: NO CHANNEL"

        channel = info.channel_name.lower()

        # 공식 채널 패턴
        official_patterns = [
            'official', 'vevo', 'records', 'entertainment', 'music',
            '뮤직', '엔터테인먼트', '레코드', '공식'
        ]

        for pattern in official_patterns:
            if pattern in channel:
                return 1.0, f"Channel verification: OFFICIAL ({info.channel_name})"

        # 토픽 채널
        if '- topic' in channel:
            return 0.9, f"Channel verification: TOPIC CHANNEL ({info.channel_name})"

        # 일반 채널
        return 0.5, f"Channel verification: REGULAR ({info.channel_name})"

    def cross_verify(self, info: YouTubeInfo, search_results: List[dict]) -> VerificationResult:
        """외부 검색 결과와 교차 검증"""
        notes = []
        confidence = 0.0

        if not search_results:
            notes.append("Cross-verification: NO EXTERNAL RESULTS")
            return self.verify(info)

        # 기본 검증 수행
        base_result = self.verify(info)
        confidence = base_result.confidence

        # 외부 결과와 제목 비교
        info_title_normalized = self._normalize_string(info.title)

        match_count = 0
        for result in search_results[:5]:
            result_title = self._normalize_string(result.get('title', ''))
            if self._similarity(info_title_normalized, result_title) > 0.7:
                match_count += 1

        if match_count > 0:
            confidence += 0.2
            notes.append(f"Cross-verification: {match_count} matching results found")
        else:
            notes.append("Cross-verification: No matching results")

        return VerificationResult(
            original_info=info,
            verified=confidence >= 0.5,
            confidence=min(confidence, 1.0),
            verification_source="cross_verification",
            notes=base_result.notes + notes
        )

    def _normalize_string(self, s: str) -> str:
        """문자열 정규화"""
        if not s:
            return ""
        # 소문자 변환, 특수문자 제거
        s = s.lower()
        s = re.sub(r'[^\w\s가-힣]', '', s)
        s = re.sub(r'\s+', ' ', s).strip()
        return s

    def _similarity(self, s1: str, s2: str) -> float:
        """두 문자열의 유사도 계산 (간단한 방식)"""
        if not s1 or not s2:
            return 0.0

        words1 = set(s1.split())
        words2 = set(s2.split())

        if not words1 or not words2:
            return 0.0

        intersection = words1 & words2
        union = words1 | words2

        return len(intersection) / len(union)

    def clear_cache(self):
        """캐시 초기화"""
        self._verification_cache.clear()
