package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.dto.ChatRoomListDTO;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatRoomListService;
import com.team5.catdogeats.chats.service.ChatRoomUpdateService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.users.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomListServiceImpl implements ChatRoomListService {
    private final ChatRoomUpdateService chatRoomUpdateService;
    private final UserIdCacheService userIdCacheService;
    private final ChatRoomRepository chatRoomRepository;

    public List<ChatRoomListDTO> getAllChatRooms(UserPrincipal userPrincipal) {
        try {
            // 사용자 역할 조회
            String userId = userIdCacheService.getCachedUserId(userPrincipal.provider(), userPrincipal.providerId());
            String userRole = userIdCacheService.getCachedRoleByUserId(userId);

            List<ChatRooms> chatRooms;

            if (Role.ROLE_BUYER.toString().equals(userRole)) {
                // 구매자인 경우: buyerId로 조회
                chatRooms = chatRoomRepository.findByBuyerIdOrderByLastMessageAtDesc(userId);
            } else if (Role.ROLE_SELLER.toString().equals(userRole)) {
                // 판매자인 경우: sellerId로 조회
                chatRooms = chatRoomRepository.findBySellerIdOrderByLastMessageAtDesc(userId);
            } else {
                throw new IllegalStateException("허용되지 않은 역할입니다: " + userRole);
            }

            // ChatRooms -> ChatRoomListDTO 변환
            return chatRooms.stream()
                    .map(room -> ChatRoomListDTO.convertToChatRoomListDTO(room, userId, userRole))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("전체 채팅방 목록 조회 실패");
            throw e;
        }
    }



    /**
     * 사용자의 전체 안읽은 메시지 개수 조회
     */
    public int getTotalUnreadCount(String userId) {
        return chatRoomUpdateService.getTotalUnreadCount(userId);
    }

    /**
     * 특정 채팅방의 안읽은 메시지 개수 조회
     */
    public int getRoomUnreadCount(String roomId, String userId) {
        return chatRoomUpdateService.getUnreadCount(roomId, userId);
    }

}
