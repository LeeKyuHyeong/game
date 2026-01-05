package com.kh.game.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GameSettings {

    private Integer timeLimit = 30;              // 제한시간(초)
    private Boolean hintEnabled = true;          // 힌트 사용 가능 여부
    private String hintType = "INITIAL";         // 힌트 타입 (INITIAL: 초성, YEAR: 연도, ARTIST: 아티스트)
    private Integer yearFrom = null;             // 연도 범위 시작
    private Integer yearTo = null;               // 연도 범위 끝
    private Boolean soloOnly = false;            // 솔로곡만
    private Boolean groupOnly = false;           // 그룹곡만
    private Long fixedGenreId = null;            // 고정 장르 ID
    private Boolean skipAllowed = true;          // 스킵 허용
    private Integer maxSkips = 3;                // 최대 스킵 횟수
    private Integer scorePerCorrect = 100;       // 정답당 기본 점수
    private Integer timeBonusRate = 10;          // 시간 보너스 비율
    private Boolean autoNextRound = true;        // 자동 다음 라운드
}