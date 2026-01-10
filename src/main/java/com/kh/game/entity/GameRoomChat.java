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
@Table(name = "game_room_chat")
public class GameRoomChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_room_id", nullable = false)
    private GameRoom gameRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType = MessageType.CHAT;

    private Integer roundNumber;  // 정답일 경우 라운드 번호

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum MessageType {
        CHAT,           // 일반 채팅
        CORRECT_ANSWER, // 정답 메시지
        SYSTEM          // 시스템 메시지
    }

    // 생성자
    public GameRoomChat(GameRoom gameRoom, Member member, String message, MessageType messageType) {
        this.gameRoom = gameRoom;
        this.member = member;
        this.message = message;
        this.messageType = messageType;
    }

    // 일반 채팅 생성
    public static GameRoomChat chat(GameRoom gameRoom, Member member, String message) {
        return new GameRoomChat(gameRoom, member, message, MessageType.CHAT);
    }

    // 정답 메시지 생성
    public static GameRoomChat correctAnswer(GameRoom gameRoom, Member member, String message, int roundNumber) {
        GameRoomChat chat = new GameRoomChat(gameRoom, member, message, MessageType.CORRECT_ANSWER);
        chat.setRoundNumber(roundNumber);
        return chat;
    }

    // 시스템 메시지 생성
    public static GameRoomChat system(GameRoom gameRoom, Member member, String message) {
        return new GameRoomChat(gameRoom, member, message, MessageType.SYSTEM);
    }
}