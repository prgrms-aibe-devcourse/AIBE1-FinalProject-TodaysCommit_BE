package com.team5.catdogeats.chats.domain.dto;

import com.team5.catdogeats.chats.domain.enums.BehaviorType;

public record ChatMessageDTO(BehaviorType behaviorType,
                             String roomId,
                             String message) {
}
