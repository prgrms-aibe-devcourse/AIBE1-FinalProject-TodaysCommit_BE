package com.team5.catdogeats.chats.domain;

import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@CompoundIndex(name = "uk_room_participants", def = "{'buyerId': 1, 'sellerId': 1}", unique = true)
@CompoundIndex(name = "idx_buyer_lastmessage", def = "{'buyerId': 1, 'lastMessageAt': -1}")
@CompoundIndex(name = "idx_seller_lastmessage", def = "{'sellerId': 1, 'lastMessageAt': -1}")
@CompoundIndex(name = "idx_buyer_unread", def = "{'buyerId': 1, 'buyerUnreadCount': -1}")
@CompoundIndex(name = "idx_seller_unread", def = "{'sellerId': 1, 'sellerUnreadCount': -1}")
public class ChatRooms {

    private String id;
    private String buyerId;
    private String sellerId;
    private Instant createdAt;
    private Instant updatedAt;


    // 1. 채팅방 읽음 여부를 위한 필드
    private Instant buyerLastReadAt;    // 구매자가 마지막으로 읽은 시간
    private Instant sellerLastReadAt;   // 판매자가 마지막으로 읽은 시간

    // 2. 마지막 메시지 정보를 위한 필드 (실시간 조회 최적화)
    private String lastMessage;         // 마지막 메시지 내용
    @Indexed
    private Instant lastMessageAt;      // 마지막 메시지 전송 시간
    private String lastSenderId;        // 마지막 메시지 발신자 ID
    private BehaviorType lastBehaviorType; // 마지막 메시지 행동 타입

    // 3. 상대방 이름 표시를 위한 필드 (비정규화 - 실시간성을 위해 필수)
    private String buyerName;           // 구매자 이름
    private String sellerName;          // 판매자 이름

    // 4. 안읽은 메시지 수 (실시간 알림을 위해 중요)
    @Builder.Default
    private int buyerUnreadCount = 0;   // 구매자 기준 안읽은 메시지 수
    @Builder.Default
    private int sellerUnreadCount = 0;  // 판매자 기준 안읽은 메시지 수

    // 5. 실시간 상태 관리를 위한 추가 필드
    @Builder.Default
    private boolean buyerOnline = false;  // 구매자 온라인 상태
    @Builder.Default
    private boolean sellerOnline = false; // 판매자 온라인 상태

    private Instant buyerLastSeenAt;    // 구매자 마지막 접속 시간
    private Instant sellerLastSeenAt;   // 판매자 마지막 접속 시간

}