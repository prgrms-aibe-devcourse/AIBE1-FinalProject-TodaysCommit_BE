package com.team5.catdogeats.chats.domain.mapping;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.users.domain.enums.Role;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.ZonedDateTime;

@Document(collection = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ChatMessages extends BaseEntity {

    @Id
    private String id;

    private String roomId;
    private String senderId;
    private Role senderType;
    private BehaviorType behaviorType;
    private String message;
    @Builder.Default
    private boolean isRead = false;
    private ZonedDateTime sentAt;
    private ZonedDateTime readAt;
}