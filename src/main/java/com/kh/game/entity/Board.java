package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "board", indexes = {
    @Index(name = "idx_board_category", columnList = "category"),
    @Index(name = "idx_board_member", columnList = "member_id"),
    @Index(name = "idx_board_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoardCategory category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "comment_count")
    private Integer commentCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoardStatus status = BoardStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoardComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoardLike> likes = new ArrayList<>();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum BoardCategory {
        REQUEST("곡 추천/요청"),
        OPINION("의견/후기"),
        QUESTION("질문"),
        FREE("자유");

        private final String description;

        BoardCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum BoardStatus {
        ACTIVE("활성"),
        DELETED("삭제됨"),
        HIDDEN("숨김");

        private final String description;

        BoardStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public Board(Member member, BoardCategory category, String title, String content) {
        this.member = member;
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }

    public void updateLikeCount(int delta) {
        this.likeCount = Math.max(0, (this.likeCount == null ? 0 : this.likeCount) + delta);
    }

    public void updateCommentCount(int delta) {
        this.commentCount = Math.max(0, (this.commentCount == null ? 0 : this.commentCount) + delta);
    }
}
