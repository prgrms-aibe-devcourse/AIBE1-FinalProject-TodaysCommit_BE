package com.team5.catdogeats.chats.domain.dto;

import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ChatMessageListDTO(
        String roomId,
        BehaviorType behaviorType,
        String senderId,
        String message,
        boolean isRead,
        Instant sentAt,
        boolean isMe
) {
    public static ChatMessageListDTO fromEntity(
            com.team5.catdogeats.chats.domain.mapping.ChatMessages entity,
            String currentUserId
    ) {
        boolean isMe = entity.getSenderId().equals(currentUserId);
        boolean isRead = entity.getReadAt() != null;

        return ChatMessageListDTO.builder()
                .roomId(entity.getRoomId())
                .behaviorType(entity.getBehaviorType())
                .senderId(entity.getSenderId())
                .message(entity.getMessage())
                .isRead(isRead)
                .sentAt(entity.getSentAt())
                .isMe(isMe)
                .build();
    }
}
