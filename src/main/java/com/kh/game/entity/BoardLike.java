package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_like",
    uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "member_id"}),
    indexes = {
        @Index(name = "idx_like_board", columnList = "board_id"),
        @Index(name = "idx_like_member", columnList = "member_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class BoardLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public BoardLike(Board board, Member member) {
        this.board = board;
        this.member = member;
    }
}
