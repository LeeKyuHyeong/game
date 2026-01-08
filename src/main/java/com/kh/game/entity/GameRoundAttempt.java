package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_round_attempt")
@Getter
@Setter
@NoArgsConstructor
public class GameRoundAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_round_id", nullable = false)
    private GameRound gameRound;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "user_answer", nullable = false, length = 255)
    private String userAnswer;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public GameRoundAttempt(GameRound gameRound, Integer attemptNumber, String userAnswer, Boolean isCorrect) {
        this.gameRound = gameRound;
        this.attemptNumber = attemptNumber;
        this.userAnswer = userAnswer;
        this.isCorrect = isCorrect;
    }
}