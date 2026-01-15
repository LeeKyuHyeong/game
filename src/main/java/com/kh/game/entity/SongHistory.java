package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 곡 이력 테이블
 * - 곡 추가/삭제/복구 이력을 시간순으로 추적
 * - BETWEEN 쿼리로 특정 시점의 유효 곡 수 계산 가능
 */
@Entity
@Table(name = "song_history",
        indexes = {
                @Index(name = "idx_artist_action_at", columnList = "artist, action_at"),
                @Index(name = "idx_song_id", columnList = "song_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class SongHistory {

    public enum Action {
        ADDED,      // 곡 추가
        DELETED,    // 곡 삭제 (soft delete)
        RESTORED    // 곡 복구
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(nullable = false, length = 100)
    private String artist;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Action action;

    @Column(name = "action_at", nullable = false)
    private LocalDateTime actionAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (actionAt == null) {
            actionAt = LocalDateTime.now();
        }
    }

    public SongHistory(Long songId, String artist, String title, Action action) {
        this.songId = songId;
        this.artist = artist;
        this.title = title;
        this.action = action;
        this.actionAt = LocalDateTime.now();
    }

    public SongHistory(Long songId, String artist, String title, Action action, LocalDateTime actionAt) {
        this.songId = songId;
        this.artist = artist;
        this.title = title;
        this.action = action;
        this.actionAt = actionAt;
    }
}
