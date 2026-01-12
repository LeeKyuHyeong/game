package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "song_report", indexes = {
    @Index(name = "idx_song_report_status", columnList = "status"),
    @Index(name = "idx_song_report_song", columnList = "song_id")
})
@Getter
@Setter
@NoArgsConstructor
public class SongReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;  // null 가능 (게스트 신고)

    @Column(name = "session_id", length = 100)
    private String sessionId;  // 게스트 중복 신고 방지용

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    @Column(length = 500)
    private String description;  // 기타 선택 시 상세 내용

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "admin_note", length = 500)
    private String adminNote;  // 관리자 처리 메모

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private Member processedBy;  // 처리한 관리자

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 신고 유형
    public enum ReportType {
        AD("광고 있음"),
        UNPLAYABLE("재생 불가"),
        OTHER("기타");

        private final String description;

        ReportType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 신고 상태
    public enum ReportStatus {
        PENDING("대기"),
        CONFIRMED("확인됨"),
        REJECTED("반려"),
        RESOLVED("해결됨");

        private final String description;

        ReportStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public SongReport(Song song, Member member, String sessionId, ReportType reportType) {
        this.song = song;
        this.member = member;
        this.sessionId = sessionId;
        this.reportType = reportType;
    }

    public void process(ReportStatus newStatus, String adminNote, Member admin) {
        this.status = newStatus;
        this.adminNote = adminNote;
        this.processedBy = admin;
        this.processedAt = LocalDateTime.now();
    }
}
