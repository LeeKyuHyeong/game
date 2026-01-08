package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "game_room_participant",
        uniqueConstraints = @UniqueConstraint(columnNames = {"game_room_id", "member_id"}))
public class GameRoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_room_id", nullable = false)
    private GameRoom gameRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Boolean isReady = false;  // 준비 상태

    @Column(nullable = false)
    private Integer score = 0;  // 현재 게임 점수

    @Column(nullable = false)
    private Integer correctCount = 0;  // 정답 수

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantStatus status = ParticipantStatus.JOINED;

    // 현재 라운드 답변 관련
    private String currentAnswer;  // 현재 라운드 제출한 답

    @Column(nullable = false)
    private Boolean hasAnswered = false;  // 현재 라운드 답변 제출 여부

    @Column(nullable = false)
    private Integer currentRoundScore = 0;  // 현재 라운드 획득 점수

    @Column(nullable = false)
    private Boolean currentRoundCorrect = false;  // 현재 라운드 정답 여부

    private LocalDateTime answerTime;  // 답변 제출 시간

    @CreationTimestamp
    private LocalDateTime joinedAt;

    public enum ParticipantStatus {
        JOINED,     // 참가중
        PLAYING,    // 게임중
        LEFT        // 나감
    }

    // 생성자
    public GameRoomParticipant(GameRoom gameRoom, Member member) {
        this.gameRoom = gameRoom;
        this.member = member;
    }

    // 준비 토글
    public void toggleReady() {
        this.isReady = !this.isReady;
    }

    // 점수 추가
    public void addScore(int points) {
        this.score += points;
    }

    // 정답 카운트 증가
    public void incrementCorrect() {
        this.correctCount++;
    }

    // 점수 리셋
    public void resetScore() {
        this.score = 0;
        this.correctCount = 0;
    }

    // 라운드 답변 초기화
    public void resetRoundAnswer() {
        this.currentAnswer = null;
        this.hasAnswered = false;
        this.currentRoundScore = 0;
        this.currentRoundCorrect = false;
        this.answerTime = null;
    }

    // 답변 제출
    public void submitAnswer(String answer, boolean isCorrect, int earnedScore) {
        this.currentAnswer = answer;
        this.hasAnswered = true;
        this.currentRoundCorrect = isCorrect;
        this.currentRoundScore = earnedScore;
        this.answerTime = LocalDateTime.now();

        if (isCorrect) {
            this.score += earnedScore;
            this.correctCount++;
        }
    }
}