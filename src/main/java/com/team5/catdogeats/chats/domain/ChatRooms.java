package com.team5.catdogeats.chats.domain;

import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@CompoundIndex(name = "uk_room_participants", def = "{'buyerId': 1, 'sellerId': 1}", unique = true)
public class ChatRooms {

    private String id;
    private String buyerId;
    private String sellerId;
    private Instant createdAt;
    private Instant updatedAt;
}