package com.kh.game.service;

import com.kh.game.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastRoomUpdate(String roomCode, Map<String, Object> payload) {
        send(roomCode, "ROOM_UPDATE", payload);
    }

    public void broadcastGameStart(String roomCode) {
        send(roomCode, "GAME_START", Map.of());
    }

    public void broadcastRoundUpdate(String roomCode, Map<String, Object> roundInfo) {
        send(roomCode, "ROUND_UPDATE", roundInfo);
    }

    public void broadcastRoundResult(String roomCode, Map<String, Object> roundInfo) {
        send(roomCode, "ROUND_RESULT", roundInfo);
    }

    public void broadcastGameFinish(String roomCode) {
        send(roomCode, "GAME_FINISH", Map.of());
    }

    public void broadcastChat(String roomCode, Map<String, Object> chatData) {
        send(roomCode, "CHAT", chatData);
    }

    public void broadcastKick(String roomCode, Long targetMemberId, String nickname) {
        send(roomCode, "KICKED", Map.of("targetMemberId", targetMemberId, "nickname", nickname));
    }

    public void broadcastRestart(String roomCode) {
        send(roomCode, "RESTART", Map.of());
    }

    private void send(String roomCode, String type, Object payload) {
        String destination = "/topic/room/" + roomCode;
        messagingTemplate.convertAndSend(destination, new WebSocketMessage(type, payload));
        log.debug("WS broadcast [{}] → {}", type, destination);
    }
}
