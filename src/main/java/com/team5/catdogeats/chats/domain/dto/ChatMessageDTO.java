package com.team5.catdogeats.chats.domain.dto;

import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.users.domain.enums.Role;

public record ChatMessageDTO(BehaviorType behaviorType,
                             String roomId,
                             String senderId,
                             String message,
                             Role senderType) {
}
