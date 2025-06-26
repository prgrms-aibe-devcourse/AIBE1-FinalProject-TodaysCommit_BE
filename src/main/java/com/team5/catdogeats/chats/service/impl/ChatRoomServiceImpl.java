package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatRoomService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.users.repository.SellersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserIdCacheService userIdCacheService;
    private final SellersRepository sellersRepository;

    public ChatRooms createRoom(UserPrincipal principal, String vendorName) {
        try {
        String buyerId  = validateUserPrincipal(principal);      // 로그인-사용자 = Buyer
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
