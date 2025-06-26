package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatRoomService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserIdCacheService userIdCacheService;
    private final SellersRepository sellersRepository;

    public ChatRooms CreateRoom(UserPrincipal userPrincipal, String vendorName) {
        try {

            String userId = validateUserPrincipal(userPrincipal);

            Sellers seller = sellersRepository.findByVendorName(vendorName)
                    .orElseThrow(() -> new NoSuchElementException("판매자 정보를 찾을 수 없습니다."));
            String sellerId = seller.getUserId();

            // 1) 자기 자신과의 채팅 방 생성 방지
            if (userId.equals(sellerId)) {
                throw new IllegalArgumentException("본인과의 채팅 방은 생성할 수 없습니다.");
            }

            List<String> ids = new ArrayList<>(List.of(userId, sellerId));
            ids.sort(Comparator.naturalOrder());
            String firstId = ids.get(0);
            String secondId = ids.get(1);

            // 2) 기존 방 조회
            return chatRoomRepository
                    .findByBuyerIdAndSellerId(firstId, secondId)
                    .orElseGet(() -> {
                        ChatRooms room = ChatRooms.builder()
                                .buyerId(firstId)
                                .sellerId(secondId)
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
            userIdCacheService.cacheUserId(userPrincipal.provider(), userPrincipal.providerId());
            userId = userIdCacheService.getCachedUserId(userPrincipal.provider(), userPrincipal.providerId());
            if (userId == null) {
                throw new IllegalStateException("userId 캐싱에 실패했습니다.");
            }
        }
        return userId;
    }
}
