package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatRoomUpdateService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomCreateServiceImplTest {


    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock private UserIdCacheService userIdCacheService;
    @Mock private SellersRepository sellersRepository;
    @Mock
    private ChatRoomUpdateService chatRoomUpdateService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ChatRoomCreateServiceImpl chatRoomCreateService;

    @Test
    @DisplayName("새 채팅방 생성 성공")
    void createRoom_Success() {
        // Given
        UserPrincipal principal = new UserPrincipal("google", "12345");

        String vendorName = "테스트상점";
        String buyerId = "buyer123";
        String sellerId = "seller123";

        Sellers seller = Sellers.builder()
                .userId(sellerId)
                .vendorName(vendorName)
                .build();

        when(userIdCacheService.getCachedUserId("google", "12345"))
                .thenReturn(buyerId);
        when(userRepository.findNameById(buyerId))
                .thenReturn(Optional.of("구매자"));
        when(sellersRepository.findByVendorName(vendorName))
                .thenReturn(Optional.of(seller));
        when(chatRoomRepository.findByBuyerIdAndSellerId(buyerId, sellerId))
                .thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRooms.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ChatRooms result = chatRoomCreateService.createRoom(principal, vendorName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBuyerId()).isEqualTo(buyerId);
        assertThat(result.getSellerId()).isEqualTo(sellerId);
        verify(chatRoomRepository).save(any(ChatRooms.class));
    }

    @Test
    @DisplayName("기존 채팅방이 존재할 때 기존 방 반환")
    void createRoom_Success_ExistingRoom() {
        // Given
        UserPrincipal principal = new UserPrincipal("google", "12345");

        String vendorName = "테스트상점";
        String buyerId = "buyer123";
        String sellerId = "seller123";

        ChatRooms existingRoom = ChatRooms.builder()
                .id("existing123")
                .buyerId(buyerId)
                .sellerId(sellerId)
                .build();

        Sellers seller = Sellers.builder()
                .userId(sellerId)
                .vendorName(vendorName)
                .build();

        when(userIdCacheService.getCachedUserId("google", "12345"))
                .thenReturn(buyerId);
        when(userRepository.findNameById(buyerId))
                .thenReturn(Optional.of("구매자"));
        when(sellersRepository.findByVendorName(vendorName))
                .thenReturn(Optional.of(seller));
        when(chatRoomRepository.findByBuyerIdAndSellerId(buyerId, sellerId))
                .thenReturn(Optional.of(existingRoom));

        // When
        ChatRooms result = chatRoomCreateService.createRoom(principal, vendorName);

        // Then
        assertThat(result).isEqualTo(existingRoom);
        verify(chatRoomRepository, never()).save(any(ChatRooms.class));
    }

    @Test
    @DisplayName("본인과 채팅방 생성 시도 시 예외 발생")
    void createRoom_ThrowsException_WhenSelfChat() {
        // Given
        UserPrincipal principal = new UserPrincipal("google", "12345");

        String vendorName = "내상점";
        String userId = "user123";

        Sellers seller = Sellers.builder()
                .userId(userId) // 같은 사용자
                .vendorName(vendorName)
                .build();

        when(userIdCacheService.getCachedUserId("google", "12345"))
                .thenReturn(userId);
        when(userRepository.findNameById(userId))
                .thenReturn(Optional.of("사용자"));
        when(sellersRepository.findByVendorName(vendorName))
                .thenReturn(Optional.of(seller));

        // When & Then
        assertThatThrownBy(() -> chatRoomCreateService.createRoom(principal, vendorName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인과의 채팅 방은 생성할 수 없습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 판매자로 채팅방 생성 시도 시 예외 발생")
    void createRoom_ThrowsException_WhenSellerNotFound() {
        // Given
        UserPrincipal principal = new UserPrincipal("google", "12345");

        String vendorName = "존재하지않는상점";
        String buyerId = "buyer123";

        when(userIdCacheService.getCachedUserId("google", "12345"))
                .thenReturn(buyerId);
        when(userRepository.findNameById(buyerId))
                .thenReturn(Optional.of("구매자"));
        when(sellersRepository.findByVendorName(vendorName))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatRoomCreateService.createRoom(principal, vendorName))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("판매자 정보를 찾을 수 없습니다.");
    }
}



