package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.mongo.repository.ChatMessageRepository;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatRoomUpdateService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.users.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// 1. ChatMessageServiceImpl 테스트 예시
@ExtendWith(MockitoExtension.class)
class ChatMessageServiceImplTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RedisTemplate<String, Object> redisTemplate;
    @Mock private UserIdCacheService userIdCacheService;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomUpdateService chatRoomUpdateService;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ChatMessageServiceImpl chatMessageService;



    @Test
    @DisplayName("메시지 저장 및 발송 성공 - 구매자가 판매자에게")
    void saveAndPublish_Success_BuyerToSeller() {
        // Given
        String userId = "buyer123";
        String roomId = "room123";
        ChatMessageDTO dto = ChatMessageDTO.builder()
                .roomId(roomId)
                .message("안녕하세요")
                .behaviorType(BehaviorType.TALK)
                .sentAt(Instant.now())
                .build();

        ChatRooms chatRoom = ChatRooms.builder()
                .id(roomId)
                .buyerId(userId)
                .sellerId("seller123")
                .build();

        ChatMessages dummySavedMessage = ChatMessages.builder()
                .id("msg1")
                .roomId(roomId)
                .senderId(userId)
                .message("안녕하세요")
                .behaviorType(BehaviorType.TALK)
                .sentAt(dto.sentAt())
                .build();


        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(chatRoom));
        when(userIdCacheService.getCachedRoleByUserId(userId))
                .thenReturn(Role.ROLE_BUYER.toString());
        when(chatMessageRepository.save(any(ChatMessages.class)))
                .thenReturn(dummySavedMessage);
        when(redisTemplate.convertAndSend(anyString(), any())).thenReturn(null);

        // When
        ChatMessageDTO result = chatMessageService.saveAndPublish(dto, userId);

        // Then
        assertThat(result).isNotNull();
        verify(chatMessageRepository).save(any(ChatMessages.class));
        verify(chatRoomUpdateService).updateRoomOnNewMessage(
                eq(roomId), eq(userId), eq("안녕하세요"),
                eq(BehaviorType.TALK), any(Instant.class));
        verify(redisTemplate, times(2)).convertAndSend(anyString(), any());
    }

    @Test
    @DisplayName("존재하지 않는 채팅방에 메시지 전송 시 예외 발생")
    void saveAndPublish_ThrowsException_WhenRoomNotFound() {
        // Given
        String userId = "user123";
        String roomId = "nonexistent";
        ChatMessageDTO dto = ChatMessageDTO.builder()
                .roomId(roomId)
                .message("메시지")
                .behaviorType(BehaviorType.TALK)
                .build();

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatMessageService.saveAndPublish(dto, userId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("허용되지 않은 역할일 때 예외 발생")
    void saveAndPublish_ThrowsException_WhenInvalidRole() {
        // Given
        String userId = "user123";
        String roomId = "room123";
        ChatMessageDTO dto = ChatMessageDTO.builder()
                .roomId(roomId)
                .message("메시지")
                .behaviorType(BehaviorType.TALK)
                .build();

        ChatRooms chatRoom = ChatRooms.builder()
                .id(roomId)
                .buyerId("buyer123")
                .sellerId("seller123")
                .build();

        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(chatRoom));
        when(userIdCacheService.getCachedRoleByUserId(userId))
                .thenReturn("INVALID_ROLE");

        // When & Then
        assertThatThrownBy(() -> chatMessageService.saveAndPublish(dto, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("허용되지 않은 역할(Role)입니다.");
    }
}
