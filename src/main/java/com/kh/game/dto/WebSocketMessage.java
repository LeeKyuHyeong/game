package com.kh.game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WebSocketMessage {
    private String type;
    private Object payload;
}
