package com.kh.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "daily_stats", uniqueConstraints = {
        @UniqueConstraint(columnNames = "stat_date")
})
public class DailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "total_games")
    private Integer totalGames = 0;

    @Column(name = "total_participants")
    private Integer totalParticipants = 0;

    @Column(name = "total_rooms")
    private Integer totalRooms = 0;

    @Column(name = "total_chats")
    private Integer totalChats = 0;

    @Column(name = "new_members")
    private Integer newMembers = 0;

    @Column(name = "active_members")
    private Integer activeMembers = 0;

    @Column(name = "total_correct_answers")
    private Integer totalCorrectAnswers = 0;

    @Column(name = "total_rounds_played")
    private Integer totalRoundsPlayed = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public DailyStats(LocalDate statDate) {
        this.statDate = statDate;
    }

    public double getAccuracyRate() {
        if (totalRoundsPlayed == null || totalRoundsPlayed == 0) return 0;
        return (double) totalCorrectAnswers / totalRoundsPlayed * 100;
    }
}
