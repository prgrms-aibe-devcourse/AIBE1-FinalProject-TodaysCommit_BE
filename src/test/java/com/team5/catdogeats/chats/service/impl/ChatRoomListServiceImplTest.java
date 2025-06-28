package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.dto.ChatRoomListDTO;
import com.team5.catdogeats.chats.domain.dto.ChatRoomPageRequestDTO;
import com.team5.catdogeats.chats.domain.dto.ChatRoomPageResponseDTO;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.users.domain.enums.Role;
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
class ChatRoomListServiceImplTest {

    @Mock
    private UserIdCacheService userIdCacheService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private ChatRoomListServiceImpl chatRoomListService;

    @Test
    void getChatRooms_shouldReturnCursorPagedResult_forBuyer() {
        // given
        String userId = "buyer-123";
        String provider = "kakao";
        String providerId = "abcdef";
        UserPrincipal principal = new UserPrincipal(provider, providerId);

        Instant now = Instant.now();
        Instant cursor = now.minusSeconds(60);

        ChatRoomPageRequestDTO pageRequest = new ChatRoomPageRequestDTO(cursor.toString(), 2);

        ChatRooms room1 = ChatRooms.builder()
                .id("r1")
                .buyerId(userId)
                .sellerId("seller-1")
                .lastMessageAt(now.minusSeconds(50))
                .buyerUnreadCount(0)
                .sellerUnreadCount(1)
                .build();

        ChatRooms room2 = ChatRooms.builder()
                .id("r2")
                .buyerId(userId)
                .sellerId("seller-2")
                .lastMessageAt(now.minusSeconds(55))
                .buyerUnreadCount(2)
                .sellerUnreadCount(0)
                .build();

        ChatRooms extraRoom = ChatRooms.builder()
                .id("r3")
                .buyerId(userId)
                .sellerId("seller-3")
                .lastMessageAt(now.minusSeconds(70))
                .build();

        List<ChatRooms> mockRooms = List.of(room1, room2, extraRoom); // size = 3 > page size = 2

        // when
        when(userIdCacheService.getCachedUserId(provider, providerId)).thenReturn(userId);
        when(userIdCacheService.getCachedRoleByUserId(userId)).thenReturn(Role.ROLE_BUYER.toString());
        when(chatRoomRepository.findByBuyerIdAndLastMessageAtLessThanOrderByLastMessageAtDesc(eq(userId), any(), any()))
                .thenReturn(mockRooms);

        ChatRoomPageResponseDTO<ChatRoomListDTO> response =
                chatRoomListService.getChatRooms(principal, pageRequest);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(room2.getLastMessageAt().toString());
    }
}
