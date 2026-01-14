package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_badge",
       uniqueConstraints = @UniqueConstraint(
               name = "UK_member_badge",
               columnNames = {"member_id", "badge_id"}))
@Getter
@Setter
@NoArgsConstructor
public class MemberBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "earned_at", nullable = false)
    private LocalDateTime earnedAt;

    @Column(name = "is_new")
    private Boolean isNew = true;  // For "new badge" indicator

    @PrePersist
    protected void onCreate() {
        earnedAt = LocalDateTime.now();
    }

    public MemberBadge(Member member, Badge badge) {
        this.member = member;
        this.badge = badge;
    }
}
