package com.team5.catdogeats.chats.domain.dto;

import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ChatMessageDTO(
        String id,
        String roomId,
        BehaviorType behaviorType,
        String message,
        boolean isRead,
        Instant sentAt
        ) {
}