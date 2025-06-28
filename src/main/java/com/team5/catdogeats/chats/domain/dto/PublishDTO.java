package com.team5.catdogeats.chats.domain.dto;

import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record PublishDTO(String roomId,
                         BehaviorType behaviorType,
                         String senderId,
                         String message,
                         boolean isRead,
                         Instant sentAt,
                         boolean isMe       // 항상 false
                        ) {
}
