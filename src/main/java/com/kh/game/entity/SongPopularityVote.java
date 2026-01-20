package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 곡 대중성/매니악 평가
 * 1(대중적) ~ 5(매니악) 척도로 평가
 * 1인 1곡 1회 투표 제한
 */
@Entity
@Table(name = "song_popularity_vote",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_song_popularity_vote_song_member",
        columnNames = {"song_id", "member_id"}
    ))
@Getter
@Setter
@NoArgsConstructor
public class SongPopularityVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 대중성/매니악 평가
     * 1: 매우 대중적
     * 2: 대중적
     * 3: 보통
     * 4: 매니악
     * 5: 매우 매니악
     */
    @Column(nullable = false)
    private Integer rating;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public SongPopularityVote(Song song, Member member, Integer rating) {
        this.song = song;
        this.member = member;
        this.rating = rating;
    }
}
