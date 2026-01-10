package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "bad_word")
public class BadWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String word;  // 금지어

    @Column(length = 100)
    private String replacement;  // 대체 문자 (null이면 ***로 대체)

    @Column(nullable = false)
    private Boolean isActive = true;  // 활성화 여부

    @CreationTimestamp
    private LocalDateTime createdAt;

    public BadWord(String word) {
        this.word = word;
    }

    public BadWord(String word, String replacement) {
        this.word = word;
        this.replacement = replacement;
    }
}