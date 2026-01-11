package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(name = "total_games")
    private Integer totalGames = 0;

    @Column(name = "total_score")
    private Integer totalScore = 0;

    @Column(name = "total_correct")
    private Integer totalCorrect = 0;

    @Column(name = "total_rounds")
    private Integer totalRounds = 0;

    @Column(name = "total_skip")
    private Integer totalSkip = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role = MemberRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "session_token", length = 64)
    private String sessionToken;  // 현재 유효한 세션 토큰

    @Column(name = "session_created_at")
    private LocalDateTime sessionCreatedAt;  // 세션 생성 시간

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MemberRole {
        USER, ADMIN
    }

    public enum MemberStatus {
        ACTIVE, INACTIVE, BANNED
    }

    // 통계 메서드
    public double getAccuracyRate() {
        if (totalRounds == null || totalRounds == 0) return 0;
        return (double) totalCorrect / totalRounds * 100;
    }

    public double getAverageScore() {
        if (totalGames == null || totalGames == 0) return 0;
        return (double) totalScore / totalGames;
    }

    // 게임 결과 반영
    public void addGameResult(int score, int correct, int rounds, int skip) {
        this.totalGames = (this.totalGames == null ? 0 : this.totalGames) + 1;
        this.totalScore = (this.totalScore == null ? 0 : this.totalScore) + score;
        this.totalCorrect = (this.totalCorrect == null ? 0 : this.totalCorrect) + correct;
        this.totalRounds = (this.totalRounds == null ? 0 : this.totalRounds) + rounds;
        this.totalSkip = (this.totalSkip == null ? 0 : this.totalSkip) + skip;
    }
}