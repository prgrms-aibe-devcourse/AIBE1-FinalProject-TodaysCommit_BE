package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.chats.util.ChatRoomLockHelper;
import com.team5.catdogeats.users.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomUpdateServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock private UserIdCacheService userIdCacheService;
    @Mock private ChatRoomLockHelper lockHelper;

    @InjectMocks
    private ChatRoomUpdateServiceImpl chatRoomUpdateService;

    @Test
    @DisplayName("구매자 메시지로 인한 방 업데이트 성공")
    void updateRoomOnNewMessage_Success_BuyerMessage() {
        // Given
        String roomId = "room123";
        String senderId = "buyer123";
        String message = "안녕하세요";
        BehaviorType behaviorType = BehaviorType.TALK;
        Instant sentAt = Instant.now();

        ChatRooms chatRoom = ChatRooms.builder()
                .id(roomId)
                .buyerId(senderId)
                .sellerId("seller123")
                .build();

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(chatRoom));
        when(userIdCacheService.getCachedRoleByUserId(senderId))
                .thenReturn(Role.ROLE_BUYER.toString());
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return null;
        }).when(lockHelper).executeWithLock(eq(roomId), any(Runnable.class));

        // When
        chatRoomUpdateService.updateRoomOnNewMessage(roomId, senderId, message, behaviorType, sentAt);

        // Then
        verify(chatRoomRepository).updateLastMessageAndIncrementSellerUnread(
                roomId, message, sentAt, senderId, behaviorType, 1);
    }

    @Test
    @DisplayName("메시지 읽음 처리 성공 - 구매자")
    void markMessagesAsRead_Success_Buyer() {
        // Given
        String roomId = "room123";
        String userId = "buyer123";

        ChatRooms chatRoom = ChatRooms.builder()
                .id(roomId)
                .buyerId(userId)
                .sellerId("seller123")
                .build();

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(chatRoom));
        when(userIdCacheService.getCachedRoleByUserId(userId))
                .thenReturn(Role.ROLE_BUYER.toString());
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return null;
        }).when(lockHelper).executeWithLock(eq(roomId), any(Runnable.class));

        // When
        chatRoomUpdateService.markMessagesAsRead(roomId, userId);

        // Then
        verify(chatRoomRepository).resetBuyerUnreadCountAndUpdateLastReadAt(
                eq(roomId), any(Instant.class));
    }

    @Test
    @DisplayName("안읽은 메시지 개수 조회 성공")
    void getUnreadCount_Success() {
        // Given
        String roomId = "room123";
        String userId = "buyer123";
        int expectedUnreadCount = 5;

        ChatRooms chatRoom = ChatRooms.builder()
                .id(roomId)
                .buyerId(userId)
                .buyerUnreadCount(expectedUnreadCount)
                .build();

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(chatRoom));
        when(userIdCacheService.getCachedRoleByUserId(userId))
                .thenReturn(Role.ROLE_BUYER.toString());

        // When
        int result = chatRoomUpdateService.getUnreadCount(roomId, userId);

        // Then
        assertThat(result).isEqualTo(expectedUnreadCount);
    }
}