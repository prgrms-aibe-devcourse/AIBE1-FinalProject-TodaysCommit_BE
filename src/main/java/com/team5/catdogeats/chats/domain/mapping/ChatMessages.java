package com.team5.catdogeats.chats.domain.mapping;

import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.users.domain.enums.Role;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ChatMessages {

    private String id;
    private String roomId;
    private String senderId;
    private Role senderType;
    private BehaviorType behaviorType;
    private String message;
    @Builder.Default
    private boolean isRead = false;
    private Instant sentAt;
    private Instant readAt;
}