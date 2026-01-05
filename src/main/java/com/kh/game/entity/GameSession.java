package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_session")
@Getter
@Setter
@NoArgsConstructor
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_uuid", length = 36)
    private String sessionUuid;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "game_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private GameType gameType;

    @Column(name = "game_mode", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private GameMode gameMode;

    @Column(name = "total_rounds")
    private Integer totalRounds;

    @Column(name = "completed_rounds")
    private Integer completedRounds = 0;

    @Column(name = "total_score")
    private Integer totalScore = 0;

    @Column(name = "correct_count")
    private Integer correctCount = 0;

    @Column(name = "skip_count")
    private Integer skipCount = 0;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private GameStatus status = GameStatus.PLAYING;

    @Column(columnDefinition = "JSON")
    private String settings;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("roundNumber ASC")
    private List<GameRound> rounds = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        startedAt = LocalDateTime.now();
    }

    public enum GameType {
        SOLO_HOST,
        SOLO_GUESS
    }

    public enum GameMode {
        RANDOM,
        GENRE_PER_ROUND,
        FIXED_GENRE
    }

    public enum GameStatus {
        PLAYING,
        COMPLETED,
        ABANDONED
    }

    public double getAccuracyRate() {
        if (completedRounds == null || completedRounds == 0) return 0;
        return (double) correctCount / completedRounds * 100;
    }

    public long getPlayTimeSeconds() {
        if (startedAt == null) return 0;
        LocalDateTime end = endedAt != null ? endedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, end).getSeconds();
    }
}