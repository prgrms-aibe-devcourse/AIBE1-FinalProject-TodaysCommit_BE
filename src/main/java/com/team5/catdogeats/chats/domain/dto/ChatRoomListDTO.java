package com.team5.catdogeats.chats.domain.dto;

import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.users.domain.enums.Role;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ChatRoomListDTO(String roomId,
                              String opponentId,
                              String opponentName,
                              String lastMessage,
                              Instant lastMessageAt,
                              String lastSenderId,
                              BehaviorType lastBehaviorType,
                              int unreadCount,
                              Instant opponentLastSeenAt,
                              Instant createdAt,
                              Instant updatedAt) {
    public static ChatRoomListDTO convertToChatRoomListDTO(ChatRooms room, String userId, String userRole) {
        // 상대방 정보 결정
        String opponentId;
        String opponentName;
        int unreadCount;
        Instant opponentLastSeenAt;

        if (Role.ROLE_BUYER.toString().equals(userRole)) {
            // 현재 사용자가 구매자인 경우, 상대방은 판매자
            opponentId = room.getSellerId();
            opponentName = room.getSellerName();
            unreadCount = room.getBuyerUnreadCount();
            opponentLastSeenAt = room.getSellerLastSeenAt();
        } else if (Role.ROLE_SELLER.toString().equals(userRole)){
            // 현재 사용자가 판매자인 경우, 상대방은 구매자
            opponentId = room.getBuyerId();
            opponentName = room.getBuyerName();
            unreadCount = room.getSellerUnreadCount();
            opponentLastSeenAt = room.getBuyerLastSeenAt();
        } else {
            return null;
        }

        return ChatRoomListDTO.builder()
                .roomId(room.getId())
                .opponentId(opponentId)
                .opponentName(opponentName)
                .lastMessage(room.getLastMessage())
                .lastMessageAt(room.getLastMessageAt())
                .lastSenderId(room.getLastSenderId())
                .lastBehaviorType(room.getLastBehaviorType())
                .unreadCount(unreadCount)
                .opponentLastSeenAt(opponentLastSeenAt)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
