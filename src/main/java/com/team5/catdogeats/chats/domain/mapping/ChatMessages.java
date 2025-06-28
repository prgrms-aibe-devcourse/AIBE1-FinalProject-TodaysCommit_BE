package com.team5.catdogeats.chats.domain.mapping;

import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.users.domain.enums.Role;
import jakarta.validation.constraints.Size;
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
    @Size(max = 1000, message = "메시지는 1000자 이내로 작성해주세요.")
    private String message;
    private Instant sentAt;
    private Instant readAt;
}