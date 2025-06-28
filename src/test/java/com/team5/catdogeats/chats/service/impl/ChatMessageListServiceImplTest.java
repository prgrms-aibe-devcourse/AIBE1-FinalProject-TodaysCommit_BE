package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.dto.ChatMessageListDTO;
import com.team5.catdogeats.chats.domain.dto.ChatMessagePageRequestDTO;
import com.team5.catdogeats.chats.domain.dto.ChatMessagePageResponseDTO;
import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.mongo.repository.ChatMessageRepository;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageListServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserIdCacheService userIdCacheService;

    @InjectMocks
    private ChatMessageListServiceImpl chatMessageListService;

    @Test
    void testGetMessagesWithCursor_shouldReturnPagedMessages() {
        // given
        String roomId = "room-123";
        String userId = "user-abc";
        String provider = "kakao";
        String providerId = "12345678";
        Instant now = Instant.now();
        Instant cursor = now.minusSeconds(60); // 1분 전

        ChatMessagePageRequestDTO pageRequest = new ChatMessagePageRequestDTO(cursor.toString(), 2);
        UserPrincipal principal = new UserPrincipal(provider, providerId);

        ChatMessages message1 = ChatMessages.builder()
                .id("m1")
                .roomId(roomId)
                .senderId(userId)
                .behaviorType(BehaviorType.TALK)
                .message("안녕")
                .sentAt(now.minusSeconds(50))
                .readAt(now)
                .build();

        ChatMessages message2 = ChatMessages.builder()
                .id("m2")
                .roomId(roomId)
                .senderId("other-user")
                .behaviorType(BehaviorType.TALK)
                .message("반가워요")
                .sentAt(now.minusSeconds(55))
                .readAt(null)
                .build();

        List<ChatMessages> mockMessages = List.of(message1, message2);

        // when
        when(userIdCacheService.getCachedUserId(provider, providerId)).thenReturn(userId);
        when(chatMessageRepository.findByRoomIdAndSentAtLessThanOrderBySentAtDesc(eq(roomId), any(), any()))
                .thenReturn(mockMessages);

        ChatMessagePageResponseDTO<ChatMessageListDTO> response =
                chatMessageListService.getMessagesWithCursor(roomId, pageRequest, principal);

        // then
        assertThat(response.contents()).hasSize(2);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.contents().get(0).isMe()).isTrue();
        assertThat(response.contents().get(1).isMe()).isFalse();
    }
}
