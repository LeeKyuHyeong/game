"""
Agent 8: AnswerMatcher
YouTube 제목에서 노래 제목을 추출하고 song_answer 테이블의 대표 정답과 비교
"""
import re
from typing import Optional, List, Dict, Tuple
import logging
from difflib import SequenceMatcher

import sys
sys.path.append('..')
from models import SongData, YouTubeInfo

logger = logging.getLogger("AnswerMatcher")


class AnswerMatchResult:
    """정답 매칭 결과"""
    def __init__(self, song_id: int):
        self.song_id = song_id
        self.youtube_title: Optional[str] = None
        self.extracted_song_title: Optional[str] = None
        self.primary_answer: Optional[str] = None
        self.all_answers: List[str] = []
        self.match_status: str = "pending"  # matched, partial, no_match, error
        self.similarity_score: float = 0.0
        self.matched_answer: Optional[str] = None
        self.extraction_method: str = ""
        self.notes: List[str] = []

    def to_dict(self) -> dict:
        return {
            "song_id": self.song_id,
            "youtube_title": self.youtube_title,
            "extracted_song_title": self.extracted_song_title,
            "primary_answer": self.primary_answer,
            "all_answers": self.all_answers,
            "match_status": self.match_status,
            "similarity_score": round(self.similarity_score, 3),
            "matched_answer": self.matched_answer,
            "extraction_method": self.extraction_method,
            "notes": self.notes
        }


class AnswerMatcher:
    """YouTube 제목과 song_answer 매칭 담당 에이전트"""

    # 제거할 키워드들 (먼저 제거)
    REMOVE_PATTERNS = [
        r"\(Color\s*Coded.*?\)",
        r"\(Han.*?Rom.*?Eng.*?\)",
        r"\(Eng.*?Rom.*?Han.*?\)",
        r"\[Color\s*Coded.*?\]",
        r"\[Han.*?Rom.*?Eng.*?\]",
        r"\(Official\s*MV\)",
        r"\(Official\s*Video\)",
        r"\(Official\s*Music\s*Video\)",
        r"\(MV\)",
        r"\(M/V\)",
        r"\[MV\]",
        r"\[Official\s*MV\]",
        r"Lyrics\s*Video",
        r"Color\s*Coded\s*Lyrics",
        r"\|\s*by\s+\w+",
        r"\|.*$",
        r"//.*$",
        r"\s*[-~]\s*가사.*$",
        r"\[가사.*?\]",
        r"\(가사.*?\)",
        r"가사\s*(첨부|비디오|영상)",
    ]

    # 아티스트 구분자들
    SEPARATORS = [" - ", " – ", " — ", " : ", " _ "]

    SIMILARITY_THRESHOLD = 0.6
    PARTIAL_THRESHOLD = 0.4

    def __init__(self):
        self._cache: Dict[int, AnswerMatchResult] = {}

    def extract_song_title(self, youtube_title: str) -> Tuple[str, str]:
        """
        YouTube 제목에서 노래 제목만 추출 (아티스트 제외)
        Returns: (추출된 제목, 추출 방법)
        """
        if not youtube_title:
            return "", "empty_input"

        title = youtube_title

        # 1. 불필요한 키워드/태그 제거
        for pattern in self.REMOVE_PATTERNS:
            title = re.sub(pattern, "", title, flags=re.IGNORECASE)

        # Lyrics 단어 제거
        title = re.sub(r"\s+Lyrics?\s*$", "", title, flags=re.IGNORECASE)
        title = re.sub(r"\s+Lyrics?\s+", " ", title, flags=re.IGNORECASE)

        title = title.strip()

        # 2. 따옴표 안의 제목 추출 시도 (가장 정확)
        quote_match = re.search(r"['\"\'\"\「\『]([^'\"\'\"\」\』]+)['\"\'\"\」\』]", title)
        if quote_match:
            candidate = quote_match.group(1).strip()
            if len(candidate) >= 2:
                return candidate, "quoted"

        # 3. 특수 아티스트명 패턴 처리 (괄호 포함 아티스트: (G)I-DLE 등)
        special_artist_match = re.match(r"^\([A-Za-z]\)[A-Za-z\-]+\s*[-–—]\s*(.+)$", title)
        if special_artist_match:
            song_title = special_artist_match.group(1).strip()
            song_title = re.sub(r"\s*[\(\[\{].*", "", song_title).strip()
            if song_title and len(song_title) >= 2:
                return song_title, "special_artist"

        # 4. 구분자로 아티스트와 제목 분리
        for sep in self.SEPARATORS:
            if sep in title:
                parts = title.split(sep)
                if len(parts) >= 2:
                    first_part = parts[0].strip()
                    second_part = parts[1].strip()

                    # 아티스트명 패턴 체크 함수
                    def is_artist_pattern(text):
                        """짧은 대문자/숫자 패턴인지 체크 (10CM, 2NE1 등)"""
                        clean = re.sub(r"\s*[\(\[\{].*", "", text).strip()
                        if not clean or " " in clean:
                            return False
                        # 숫자+문자 패턴만 아티스트로 인식 (10CM, 2NE1)
                        # 순수 대문자 패턴(BTS, BLUE, FIRE 등)은 제목일 수도 있으므로 제외
                        if re.match(r"^\d+[A-Za-z]+$", clean):
                            return True
                        return False

                    first_is_artist = is_artist_pattern(first_part)
                    second_is_artist = is_artist_pattern(second_part)

                    # Title - Artist 패턴: 두번째가 아티스트이고 첫번째가 아티스트 아닌 경우만
                    # 둘 다 아티스트처럼 보이면 "Artist - Title" 기본 패턴 사용
                    if second_is_artist and not first_is_artist:
                        song_part = first_part
                    else:
                        song_part = second_part

                    # 괄호 앞까지 제목 추출
                    eng_title = re.sub(r"\s*[\(\[\{].*", "", song_part).strip()

                    # feat. 제거
                    eng_title = re.sub(r"\s*feat\..*$", "", eng_title, flags=re.IGNORECASE).strip()

                    if eng_title and len(eng_title) >= 2:
                        return eng_title, f"separator:{sep.strip()}"

                    # 영문 제목이 없으면 괄호 안의 한글 제목 추출
                    korean_in_paren = re.search(r"\(([가-힣\s]+)\)", song_part)
                    if korean_in_paren:
                        return korean_in_paren.group(1).strip(), "korean_in_paren"

        # 5. "by Artist" 패턴 처리
        by_match = re.search(r"^(.+?)\s+(?:by|BY|By)\s+", title)
        if by_match:
            candidate = by_match.group(1).strip()
            candidate = re.sub(r"\s*[\(\[\{].*", "", candidate).strip()
            if candidate and len(candidate) >= 2:
                return candidate, "by_pattern"

        # 6. 괄호 안의 한글 제목 추출 (전체 제목에서)
        korean_title = re.search(r"\(([가-힣\s]+)\)", title)
        if korean_title:
            candidate = korean_title.group(1).strip()
            if len(candidate) >= 2:
                return candidate, "korean_paren"

        # 7. 마지막: 괄호/대괄호 앞 부분만 사용 + 아티스트명 제거 시도
        title = re.sub(r"\s*[\(\[\{].*", "", title).strip()

        # 공백으로 분리해서 첫 단어가 아티스트명인지 체크
        words = title.split()
        if len(words) >= 2:
            # 첫 단어가 짧은(<=6) 대문자/숫자 패턴이면 아티스트명일 가능성
            first_word = words[0]
            if len(first_word) <= 6:
                if (re.match(r"^[A-Z]{2,6}$", first_word) or  # BTS, YB, TWICE
                    re.match(r"^\d+[A-Za-z]+$", first_word)):  # 10CM, 2NE1
                    potential_title = " ".join(words[1:])
                    if potential_title and len(potential_title) >= 2:
                        return potential_title, "artist_removed"

        title = re.sub(r"\s+", " ", title).strip()

        if title and len(title) >= 2:
            return title, "cleaned"

        return youtube_title, "original"

    def normalize_for_comparison(self, text: str) -> str:
        """비교를 위한 정규화"""
        if not text:
            return ""
        text = text.lower()
        # 특수문자 제거 (한글, 영문, 숫자만 유지)
        text = re.sub(r"[^\w가-힣]", "", text)
        return text

    def calculate_similarity(self, s1: str, s2: str) -> float:
        """두 문자열의 유사도 계산"""
        if not s1 or not s2:
            return 0.0

        n1 = self.normalize_for_comparison(s1)
        n2 = self.normalize_for_comparison(s2)

        if not n1 or not n2:
            return 0.0

        # 완전 일치
        if n1 == n2:
            return 1.0

        # 포함 관계
        if n1 in n2 or n2 in n1:
            return 0.9

        # SequenceMatcher 유사도
        return SequenceMatcher(None, n1, n2).ratio()

    def match_with_answers(
        self,
        extracted_title: str,
        primary_answer: str,
        all_answers: List[str]
    ) -> Tuple[str, float, Optional[str]]:
        """
        추출된 제목과 정답들 비교
        Returns: (match_status, similarity_score, matched_answer)
        """
        if not extracted_title:
            return "error", 0.0, None

        best_score = 0.0
        best_answer = None

        # 1. 대표 정답과 비교
        if primary_answer:
            score = self.calculate_similarity(extracted_title, primary_answer)
            if score > best_score:
                best_score = score
                best_answer = primary_answer

        # 2. 모든 정답과 비교
        for answer in all_answers:
            score = self.calculate_similarity(extracted_title, answer)
            if score > best_score:
                best_score = score
                best_answer = answer

        # 상태 결정
        if best_score >= self.SIMILARITY_THRESHOLD:
            return "matched", best_score, best_answer
        elif best_score >= self.PARTIAL_THRESHOLD:
            return "partial", best_score, best_answer
        else:
            return "no_match", best_score, best_answer

    def analyze(
        self,
        song_id: int,
        youtube_title: str,
        primary_answer: str,
        all_answers: List[str]
    ) -> AnswerMatchResult:
        """전체 분석 수행"""
        logger.info(f"Analyzing song ID {song_id}: YouTube title = {youtube_title[:50] if youtube_title else 'None'}...")

        result = AnswerMatchResult(song_id)
        result.youtube_title = youtube_title
        result.primary_answer = primary_answer
        result.all_answers = all_answers

        if not youtube_title:
            result.match_status = "error"
            result.notes.append("YouTube 제목이 없습니다")
            return result

        # 제목 추출
        extracted, method = self.extract_song_title(youtube_title)
        result.extracted_song_title = extracted
        result.extraction_method = method

        if not extracted:
            result.match_status = "error"
            result.notes.append("제목을 추출할 수 없습니다")
            return result

        # 정답 매칭
        status, score, matched = self.match_with_answers(
            extracted, primary_answer, all_answers
        )
        result.match_status = status
        result.similarity_score = score
        result.matched_answer = matched

        # 노트 추가
        if status == "matched":
            result.notes.append(f"'{extracted}' ↔ '{matched}' (유사도: {score:.1%})")
        elif status == "partial":
            result.notes.append(f"부분 일치: '{extracted}' ↔ '{matched}' (유사도: {score:.1%})")
            result.notes.append("수동 확인 권장")
        else:
            result.notes.append(f"불일치: '{extracted}' vs 대표정답 '{primary_answer}'")
            result.notes.append(f"최고 유사도: {score:.1%}")

        # 캐시 저장
        self._cache[song_id] = result

        return result

    def batch_analyze(
        self,
        items: List[Dict]
    ) -> List[AnswerMatchResult]:
        """
        배치 분석
        items: [{"song_id": int, "youtube_title": str, "primary_answer": str, "all_answers": [str]}]
        """
        results = []
        for item in items:
            result = self.analyze(
                song_id=item["song_id"],
                youtube_title=item.get("youtube_title", ""),
                primary_answer=item.get("primary_answer", ""),
                all_answers=item.get("all_answers", [])
            )
            results.append(result)
        return results

    def get_summary(self, results: List[AnswerMatchResult]) -> dict:
        """결과 요약"""
        summary = {
            "total": len(results),
            "matched": 0,
            "partial": 0,
            "no_match": 0,
            "error": 0,
            "average_similarity": 0.0
        }

        scores = []
        for r in results:
            if r.match_status == "matched":
                summary["matched"] += 1
            elif r.match_status == "partial":
                summary["partial"] += 1
            elif r.match_status == "no_match":
                summary["no_match"] += 1
            else:
                summary["error"] += 1

            if r.similarity_score > 0:
                scores.append(r.similarity_score)

        if scores:
            summary["average_similarity"] = round(sum(scores) / len(scores), 3)

        return summary

    def clear_cache(self):
        """캐시 초기화"""
        self._cache.clear()

    def get_cached_result(self, song_id: int) -> Optional[AnswerMatchResult]:
        """캐시된 결과 조회"""
        return self._cache.get(song_id)
