package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 홈 화면 메뉴 설정 엔티티
 * 관리자가 각 메뉴 항목의 활성화/비활성화를 관리할 수 있도록 함
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "menu_config")
public class MenuConfig {

    @Id
    @Column(length = 50)
    private String menuId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Column(length = 50)
    private String category;  // SOLO, MULTI 등 메뉴 카테고리

    @Column(length = 100)
    private String icon;  // 메뉴 아이콘

    @Column(length = 200)
    private String url;  // 메뉴 링크 URL

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public MenuConfig(String menuId, String name, String description,
                      String category, String icon, String url,
                      int displayOrder, boolean enabled) {
        this.menuId = menuId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.icon = icon;
        this.url = url;
        this.displayOrder = displayOrder;
        this.enabled = enabled;
    }
}
