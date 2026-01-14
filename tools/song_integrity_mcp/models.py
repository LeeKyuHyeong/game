"""
Song Integrity MCP - Data Models
공통 데이터 모델 정의
"""
from dataclasses import dataclass, field
from typing import Optional, List
from enum import Enum
from datetime import datetime


class DecisionType(Enum):
    """사용자 결정 유형"""
    KEEP = "keep"           # 현재 데이터 유지
    UPDATE = "update"       # 데이터 수정
    DELETE = "delete"       # 삭제
    REPLACE = "replace"     # 새 YouTube ID로 교체
    SKIP = "skip"           # 이번에는 스킵


class VerificationStatus(Enum):
    """검증 상태"""
    PENDING = "pending"
    VERIFIED = "verified"
    MISMATCH = "mismatch"
    VIDEO_UNAVAILABLE = "video_unavailable"
    ERROR = "error"


@dataclass
class SongData:
    """현재 DB에 저장된 노래 데이터"""
    id: int
    artist: str
    title: str
    release_year: Optional[int]
    is_solo: bool
    play_duration: Optional[int]  # seconds
    youtube_video_id: str
    genre_id: Optional[int] = None
    created_at: Optional[datetime] = None

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "artist": self.artist,
            "title": self.title,
            "release_year": self.release_year,
            "is_solo": self.is_solo,
            "play_duration": self.play_duration,
            "youtube_video_id": self.youtube_video_id,
            "genre_id": self.genre_id,
            "created_at": self.created_at.isoformat() if self.created_at else None
        }


@dataclass
class YouTubeInfo:
    """YouTube에서 검색한 실제 정보"""
    video_id: str
    title: str
    channel_name: str
    duration: Optional[int] = None  # seconds
    publish_date: Optional[str] = None
    view_count: Optional[int] = None
    is_available: bool = True
    thumbnail_url: Optional[str] = None
    description: Optional[str] = None

    # 파싱된 정보
    parsed_artist: Optional[str] = None
    parsed_title: Optional[str] = None
    parsed_year: Optional[int] = None

    def to_dict(self) -> dict:
        return {
            "video_id": self.video_id,
            "title": self.title,
            "channel_name": self.channel_name,
            "duration": self.duration,
            "publish_date": self.publish_date,
            "view_count": self.view_count,
            "is_available": self.is_available,
            "thumbnail_url": self.thumbnail_url,
            "description": self.description,
            "parsed_artist": self.parsed_artist,
            "parsed_title": self.parsed_title,
            "parsed_year": self.parsed_year
        }


@dataclass
class VerificationResult:
    """검증 결과"""
    original_info: YouTubeInfo
    verified: bool
    confidence: float  # 0.0 ~ 1.0
    verification_source: str  # 검증에 사용한 소스
    notes: List[str] = field(default_factory=list)

    def to_dict(self) -> dict:
        return {
            "original_info": self.original_info.to_dict(),
            "verified": self.verified,
            "confidence": self.confidence,
            "verification_source": self.verification_source,
            "notes": self.notes
        }


@dataclass
class ComparisonResult:
    """비교 결과"""
    song_data: SongData
    youtube_info: Optional[YouTubeInfo]
    verification_result: Optional[VerificationResult]

    # 비교 결과
    status: VerificationStatus = VerificationStatus.PENDING
    artist_match: bool = False
    title_match: bool = False
    year_match: bool = False
    duration_match: bool = False

    # 차이점
    differences: List[str] = field(default_factory=list)
    recommendations: List[str] = field(default_factory=list)

    def to_dict(self) -> dict:
        return {
            "song_data": self.song_data.to_dict(),
            "youtube_info": self.youtube_info.to_dict() if self.youtube_info else None,
            "verification_result": self.verification_result.to_dict() if self.verification_result else None,
            "status": self.status.value,
            "artist_match": self.artist_match,
            "title_match": self.title_match,
            "year_match": self.year_match,
            "duration_match": self.duration_match,
            "differences": self.differences,
            "recommendations": self.recommendations
        }


@dataclass
class UserDecision:
    """사용자 결정"""
    song_id: int
    decision_type: DecisionType
    new_youtube_id: Optional[str] = None
    update_fields: Optional[dict] = None
    reason: Optional[str] = None

    def to_dict(self) -> dict:
        return {
            "song_id": self.song_id,
            "decision_type": self.decision_type.value,
            "new_youtube_id": self.new_youtube_id,
            "update_fields": self.update_fields,
            "reason": self.reason
        }


@dataclass
class IntegrityCheckSession:
    """정합성 체크 세션"""
    session_id: str
    started_at: datetime
    songs_to_check: List[int] = field(default_factory=list)
    current_index: int = 0
    results: List[ComparisonResult] = field(default_factory=list)
    decisions: List[UserDecision] = field(default_factory=list)
    completed: bool = False

    def to_dict(self) -> dict:
        return {
            "session_id": self.session_id,
            "started_at": self.started_at.isoformat(),
            "songs_to_check": self.songs_to_check,
            "current_index": self.current_index,
            "total_songs": len(self.songs_to_check),
            "results_count": len(self.results),
            "decisions_count": len(self.decisions),
            "completed": self.completed
        }
