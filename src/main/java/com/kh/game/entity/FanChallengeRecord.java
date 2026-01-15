package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "fan_challenge_record",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_member_artist_difficulty",
                columnNames = {"member_id", "artist", "difficulty"}))
@Getter
@Setter
@NoArgsConstructor
public class FanChallengeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String artist;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FanChallengeDifficulty difficulty = FanChallengeDifficulty.HARDCORE;

    @Column(name = "total_songs")
    private Integer totalSongs;

    @Column(name = "correct_count")
    private Integer correctCount = 0;

    @Column(name = "is_perfect_clear")
    private Boolean isPerfectClear = false;

    /**
     * 현재 시점 기준 퍼펙트 여부
     * - 주간 배치에서 갱신
     * - 곡 수 변경 시 false로 변경
     */
    @Column(name = "is_current_perfect")
    private Boolean isCurrentPerfect = false;

    @Column(name = "best_time_ms")
    private Long bestTimeMs;

    @Column(name = "achieved_at")
    private LocalDateTime achievedAt;

    /**
     * 마지막 퍼펙트 검사 시점
     * - 주간 배치 실행 시 갱신
     */
    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

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

    public FanChallengeRecord(Member member, String artist, Integer totalSongs) {
        this.member = member;
        this.artist = artist;
        this.totalSongs = totalSongs;
        this.difficulty = FanChallengeDifficulty.HARDCORE;
    }

    public FanChallengeRecord(Member member, String artist, Integer totalSongs, FanChallengeDifficulty difficulty) {
        this.member = member;
        this.artist = artist;
        this.totalSongs = totalSongs;
        this.difficulty = difficulty;
    }

    public double getClearRate() {
        if (totalSongs == null || totalSongs == 0) return 0;
        return (double) correctCount / totalSongs * 100;
    }
}
