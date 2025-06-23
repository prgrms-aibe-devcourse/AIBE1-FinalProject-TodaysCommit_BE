package com.team5.catdogeats.chats.domain.mapping;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.chats.domain.ChatRooms;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_messages_room_id", columnList = "room_id"),
                @Index(name = "idx_chat_messages_sender_id", columnList = "sender_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessages extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_messages_room"))
    private ChatRooms chatRoom;

    @Column(name = "sender_id", length = 36, nullable = false)
    private String senderId;  // buyers 또는 sellers 중 어느 쪽인지 구분하려면 별도 로직 필요

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private ZonedDateTime sentAt;

    @Column(name = "read_at")
    private ZonedDateTime readAt;
}