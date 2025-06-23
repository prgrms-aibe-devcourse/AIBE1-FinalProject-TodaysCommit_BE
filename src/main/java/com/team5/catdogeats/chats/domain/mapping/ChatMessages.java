package com.team5.catdogeats.chats.domain.mapping;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.users.domain.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
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
    private String senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 11)
    private Role senderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "behavior_type", nullable = false, length = 6)
    private BehaviorType behaviorType;

    @Size(max = 1000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private ZonedDateTime sentAt;

    @Column(name = "read_at")
    private ZonedDateTime readAt;
}