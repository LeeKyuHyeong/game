package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 장르 챌린지 기록
 * - 장르별 전곡 도전 모드
 * - 정답수/콤보/시간 기록
 * - 영구 랭킹 (리셋 없음)
 */
@Entity
@Table(name = "genre_challenge_record",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_member_genre_difficulty",
                columnNames = {"member_id", "genre_id", "difficulty"}))
@Getter
@Setter
@NoArgsConstructor
public class GenreChallengeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private GenreChallengeDifficulty difficulty = GenreChallengeDifficulty.HARDCORE;

    /**
     * 도전 당시 총 곡 수
     */
    @Column(name = "total_songs")
    private Integer totalSongs;

    /**
     * 최다 정답 수
     */
    @Column(name = "correct_count")
    private Integer correctCount = 0;

    /**
     * 최다 콤보 (연속 정답)
     */
    @Column(name = "max_combo")
    private Integer maxCombo = 0;

    /**
     * 최고 기록 시간 (ms)
     */
    @Column(name = "best_time_ms")
    private Long bestTimeMs;

    /**
     * 기록 달성 시점
     */
    @Column(name = "achieved_at")
    private LocalDateTime achievedAt;

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

    public GenreChallengeRecord(Member member, Genre genre, Integer totalSongs) {
        this.member = member;
        this.genre = genre;
        this.totalSongs = totalSongs;
        this.difficulty = GenreChallengeDifficulty.HARDCORE;
    }

    public GenreChallengeRecord(Member member, Genre genre, Integer totalSongs, GenreChallengeDifficulty difficulty) {
        this.member = member;
        this.genre = genre;
        this.totalSongs = totalSongs;
        this.difficulty = difficulty;
    }

    /**
     * 클리어율 계산 (달성 시점 기준)
     */
    public double getClearRate() {
        if (totalSongs == null || totalSongs == 0) return 0;
        return (double) correctCount / totalSongs * 100;
    }
}
