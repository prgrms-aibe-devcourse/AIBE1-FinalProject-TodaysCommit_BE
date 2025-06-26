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
//    // 클라이언트에서 서버로 보낼 때 사용 (최소 정보만)
//    public static ChatMessageDTO forSending(BehaviorType behaviorType, String roomId, String message) {
//        return new ChatMessageDTO(null, behaviorType, roomId, null, message, false, null);
//    }

//    // 서버에서 저장된 데이터를 클라이언트로 보낼 때 사용
//    public static ChatMessageDTO from(ChatMessages saved) {
//        return new ChatMessageDTO(
//                saved.getId(),
//                saved.getBehaviorType(),
//                saved.getRoomId(),
//                saved.getSenderId(),
//                saved.getMessage(),
//                saved.isRead(),
//                saved.getSentAt()
//        );
//    }
}