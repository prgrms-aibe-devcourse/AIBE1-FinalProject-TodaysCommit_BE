package com.team5.catdogeats.chats.service;

import com.team5.catdogeats.chats.domain.enums.BehaviorType;

import java.time.Instant;

public interface ChatRoomUpdateService {

    void updateRoomOnNewMessage(String roomId, String senderId, String message,
                                BehaviorType behaviorType, Instant sentAt);


    void markMessagesAsRead(String roomId, String userId);

    int getUnreadCount(String roomId, String userId);

    int getTotalUnreadCount(String userId);

}

