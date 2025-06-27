package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatRoomService;
import com.team5.catdogeats.chats.service.ChatRoomUpdateService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.global.config.MongoTransactional;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.SellersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserIdCacheService userIdCacheService;
    private final SellersRepository sellersRepository;
    private final ChatRoomUpdateService chatRoomUpdateService;

    public ChatRooms createRoom(UserPrincipal principal, String vendorName) {
        try {
        String buyerId  = validateUserPrincipal(principal);
        String sellerId = sellersRepository.findByVendorName(vendorName)
                .orElseThrow(() -> new NoSuchElementException("판매자 정보를 찾을 수 없습니다."))
                .getUserId();

        if (buyerId.equals(sellerId)) {
            throw new IllegalArgumentException("본인과의 채팅 방은 생성할 수 없습니다.");
        }


            // 4) buyerId/sellerId 순서 그대로 find or create
            return chatRoomRepository
                    .findByBuyerIdAndSellerId(buyerId, sellerId)
                    .orElseGet(() -> {
                        ChatRooms room = ChatRooms.builder()
                                .buyerId(buyerId)
                                .sellerId(sellerId)
                                .createdAt(Instant.now())
                                .build();
                        return chatRoomRepository.save(room);
                    });
        } catch (Exception e) {
            log.error("Error creating chat room", e);
            throw e;
        }
    }

    @MongoTransactional
    public void markMessagesAsRead(String roomId, String userId) {
        try {
            String role = userIdCacheService.getCachedRoleByUserId(userId);
            // 채팅방 참여자 검증
            if (Objects.equals(Role.ROLE_SELLER.toString(), role)) {
                chatRoomRepository.findByIdAndSellerId(roomId, userId)
                        .orElseThrow(() -> new NoSuchElementException("유저 정보가 없습니다."));
            } else if (Objects.equals(Role.ROLE_BUYER.toString(), role)) {
                chatRoomRepository.findByIdAndBuyerId(roomId, userId)
                        .orElseThrow(() -> new NoSuchElementException("유저 정보가 없습니다."));
            } else {
                throw new IllegalStateException("권한 정보가 올바르지 않습니다.");
            }
            // 안읽은 메시지 개수 초기화 및 마지막 읽음 시간 업데이트
            chatRoomUpdateService.markMessagesAsRead(roomId, userId);

            log.debug("메시지 읽음 처리 완료: roomId={}, userId={}", roomId, userId);
        } catch (Exception e) {
            log.error("메시지 읽음 처리 실패: roomId={}, userId={}", roomId, userId, e);
            throw e;
        }
    }


    private String validateUserPrincipal(UserPrincipal userPrincipal) {
        String userId = userIdCacheService.getCachedUserId(userPrincipal.provider(), userPrincipal.providerId());
        if (userId == null) {
            userIdCacheService.cacheUserIdAndRole(userPrincipal.provider(), userPrincipal.providerId());
            userId = userIdCacheService.getCachedUserId(userPrincipal.provider(), userPrincipal.providerId());
            if (userId == null) {
                throw new IllegalStateException("userId 캐싱에 실패했습니다.");
            }
        }
        return userId;
    }
}
