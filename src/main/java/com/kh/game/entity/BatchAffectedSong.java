package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 배치 실행으로 영향받은 곡 기록
 * - YouTube 검증 배치, 중복 곡 검사 배치 등에서 비활성화된 곡을 추적
 * - 복구 기능 지원
 */
@Entity
@Table(name = "batch_affected_song", indexes = {
    @Index(name = "idx_batch_affected_song_history", columnList = "history_id"),
    @Index(name = "idx_batch_affected_song_batch", columnList = "batch_id"),
    @Index(name = "idx_batch_affected_song_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class BatchAffectedSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 배치 실행 이력 (연관)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id", nullable = false)
    private BatchExecutionHistory history;

    /**
     * 배치 ID (history가 null일 때를 대비한 직접 저장)
     */
    @Column(name = "batch_id", nullable = false, length = 50)
    private String batchId;

    /**
     * 영향받은 곡
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    /**
     * 처리 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private ActionType actionType;

    /**
     * 비활성화 사유
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AffectedReason reason;

    /**
     * 상세 사유 (예: 원본 곡 ID 등)
     */
    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;

    /**
     * 복구 여부
     */
    @Column(name = "is_restored", nullable = false)
    private Boolean isRestored = false;

    /**
     * 복구 일시
     */
    @Column(name = "restored_at")
    private LocalDateTime restoredAt;

    /**
     * 복구한 관리자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restored_by")
    private Member restoredBy;

    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 처리 유형 Enum
     */
    public enum ActionType {
        DEACTIVATED("비활성화"),
        REACTIVATED("재활성화");

        private final String displayName;

        ActionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 영향받은 사유 Enum
     */
    public enum AffectedReason {
        YOUTUBE_DELETED("YouTube 영상 삭제됨", "danger"),
        YOUTUBE_EMBED_DISABLED("YouTube 임베드 불가", "warning"),
        DUPLICATE_YOUTUBE_ID("중복 YouTube ID", "info"),
        MP3_FILE_MISSING("MP3 파일 없음", "secondary");

        private final String displayName;
        private final String badgeClass;

        AffectedReason(String displayName, String badgeClass) {
            this.displayName = displayName;
            this.badgeClass = badgeClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getBadgeClass() {
            return badgeClass;
        }
    }

    /**
     * 생성자
     */
    public BatchAffectedSong(BatchExecutionHistory history, Song song,
                              ActionType actionType, AffectedReason reason, String reasonDetail) {
        this.history = history;
        this.batchId = history != null ? history.getBatchId() : null;
        this.song = song;
        this.actionType = actionType;
        this.reason = reason;
        this.reasonDetail = reasonDetail;
    }

    /**
     * 복구 처리
     */
    public void restore(Member admin) {
        this.isRestored = true;
        this.restoredAt = LocalDateTime.now();
        this.restoredBy = admin;
    }

    /**
     * 복구 취소
     */
    public void cancelRestore() {
        this.isRestored = false;
        this.restoredAt = null;
        this.restoredBy = null;
    }
}
