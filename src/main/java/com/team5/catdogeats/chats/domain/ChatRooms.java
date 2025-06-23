package com.team5.catdogeats.chats.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ChatRooms extends BaseEntity {

    @Id
    private String id;
    private String buyerId;
    private String sellerId;
}