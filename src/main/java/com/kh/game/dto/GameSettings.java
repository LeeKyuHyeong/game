package com.kh.game.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GameSettings {

    // ========== 싱글 플레이어 설정 ==========
    private Integer timeLimit = 30;              // 제한시간(초)
    private Boolean hintEnabled = true;          // 힌트 사용 가능 여부
    private String hintType = "INITIAL";         // 힌트 타입 (INITIAL: 초성, YEAR: 연도, ARTIST: 아티스트)
    private Integer yearFrom = null;             // 연도 범위 시작 (레거시, 하위 호환성)
    private Integer yearTo = null;               // 연도 범위 끝 (레거시, 하위 호환성)
    private List<Integer> selectedYears = new ArrayList<>();  // 선택된 연도 목록
    private Boolean soloOnly = false;            // 솔로곡만
    private Boolean groupOnly = false;           // 그룹곡만
    private Long fixedGenreId = null;            // 고정 장르 ID
    private String fixedArtistName = null;       // 고정 아티스트명 (레거시, 하위 호환성)
    private List<String> selectedArtists = new ArrayList<>(); // 선택된 아티스트 목록
    private Boolean skipAllowed = true;          // 스킵 허용
    private Integer maxSkips = 3;                // 최대 스킵 횟수
    private Integer scorePerCorrect = 100;       // 정답당 기본 점수
    private Integer timeBonusRate = 10;          // 시간 보너스 비율
    private Boolean autoNextRound = true;        // 자동 다음 라운드
    private Boolean hideEmptyGenres = false;     // 남은곡 없는 장르 숨기기 (레거시)
    private Boolean hideEmptyOptions = false;    // 남은곡 없는 옵션 숨기기 (아티스트/연도/장르)

    // ========== 멀티게임 설정 ==========
    private String roomName;                     // 방 이름
    private Integer maxPlayers = 8;              // 최대 인원
    private Integer totalRounds = 10;            // 총 라운드
    private Boolean privateRoom = false;         // 비공개 방 여부
    private String gameMode = "RANDOM";          // 게임 모드 (RANDOM, FIXED_GENRE, FIXED_ARTIST)

    // boolean getter는 is로 시작해야 함
    public boolean isPrivateRoom() {
        return privateRoom != null && privateRoom;
    }
}