package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "badge")
@Getter
@Setter
@NoArgsConstructor
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;  // FIRST_GAME, FIRST_CORRECT, MULTI_SPROUT, etc.

    @Column(nullable = false, length = 100)
    private String name;  // Display name (Korean)

    @Column(length = 500)
    private String description;  // Quest condition description

    @Column(length = 10)
    private String emoji;  // Badge emoji icon

    @Column(length = 20)
    private String color;  // Badge color (hex)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BadgeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BadgeRarity rarity;

    @Column(name = "sort_order")
    private Integer sortOrder;  // Display order

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // 뱃지 카테고리
    public enum BadgeCategory {
        BEGINNER("입문"),
        SCORE("점수"),
        VICTORY("승리"),
        STREAK("연속"),
        TIER("티어"),
        SPECIAL("특별");

        private final String displayName;

        BadgeCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // 뱃지 희귀도
    public enum BadgeRarity {
        COMMON("#9CA3AF", "일반"),      // Gray
        RARE("#3B82F6", "레어"),        // Blue
        EPIC("#A855F7", "에픽"),        // Purple
        LEGENDARY("#F59E0B", "전설");   // Gold

        private final String color;
        private final String displayName;

        BadgeRarity(String color, String displayName) {
            this.color = color;
            this.displayName = displayName;
        }

        public String getColor() {
            return color;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
