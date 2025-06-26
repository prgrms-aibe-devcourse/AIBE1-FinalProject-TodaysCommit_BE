package com.team5.catdogeats.chats.domain.dto;

import com.team5.catdogeats.chats.domain.enums.BehaviorType;

import java.time.Instant;

public record ChatMessageDTO(
        String id,
        String roomId,
        BehaviorType behaviorType,
        String message,
        boolean isRead,
        Instant sentAt
        ) {
}