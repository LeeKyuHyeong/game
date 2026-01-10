package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_round")
@Getter
@Setter
@NoArgsConstructor
public class GameRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "song_id")
    private Song song;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "genre_id")
    private Genre genre;

    @Column(name = "play_start_time")
    private Integer playStartTime;

    @Column(name = "play_duration")
    private Integer playDuration;

    @Column(name = "user_answer", length = 255)
    private String userAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    @Column(name = "answer_time_ms")
    private Long answerTimeMs;

    @Column(name = "hint_used")
    private Boolean hintUsed = false;

    @Column(name = "hint_type", length = 20)
    private String hintType;

    private Integer score = 0;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RoundStatus status = RoundStatus.WAITING;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "gameRound", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("attemptNumber ASC")
    private List<GameRoundAttempt> attempts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum RoundStatus {
        WAITING,
        PLAYING,
        ANSWERED,
        SKIPPED,
        TIMEOUT
    }

    public String getAnswerTimeFormatted() {
        if (answerTimeMs == null) return "-";
        return String.format("%.2f초", answerTimeMs / 1000.0);
    }

    // Thymeleaf에서 isCorrect 접근을 위한 명시적 getter
    public boolean isCorrect() {
        return Boolean.TRUE.equals(isCorrect);
    }
}