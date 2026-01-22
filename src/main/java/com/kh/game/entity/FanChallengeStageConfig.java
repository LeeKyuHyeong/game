package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 팬 챌린지 단계별 설정
 * - HARDCORE 모드에서만 단계 시스템 적용
 * - 관리자가 단계별 곡 수, 활성화 여부를 설정
 */
@Entity
@Table(name = "fan_challenge_stage_config")
@Getter
@Setter
@NoArgsConstructor
public class FanChallengeStageConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 단계 레벨 (1, 2, 3, ...)
     */
    @Column(name = "stage_level", nullable = false, unique = true)
    private Integer stageLevel;

    /**
     * 해당 단계에 필요한 곡 수
     */
    @Column(name = "required_songs", nullable = false)
    private Integer requiredSongs;

    /**
     * 활성화 여부 (관리자가 순차적으로 개방)
     */
    @Column(name = "is_active")
    private Boolean isActive = false;

    /**
     * 활성화된 시점
     */
    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    /**
     * 단계 이름 (예: "1단계", "2단계")
     */
    @Column(name = "stage_name", length = 50)
    private String stageName;

    /**
     * 단계 이모지 (예: "🥉", "🥈", "🥇")
     */
    @Column(name = "stage_emoji", length = 10)
    private String stageEmoji;

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

    public FanChallengeStageConfig(Integer stageLevel, Integer requiredSongs, String stageName, String stageEmoji) {
        this.stageLevel = stageLevel;
        this.requiredSongs = requiredSongs;
        this.stageName = stageName;
        this.stageEmoji = stageEmoji;
        this.isActive = false;
    }

    /**
     * 단계 활성화
     */
    public void activate() {
        this.isActive = true;
        this.activatedAt = LocalDateTime.now();
    }

    /**
     * 단계 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }
}
