package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatRoomUpdateService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.global.config.MongoTransactional;
import com.team5.catdogeats.users.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomUpdateServiceImpl implements ChatRoomUpdateService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserIdCacheService userIdCacheService;

    @Override
    @MongoTransactional
    public void updateRoomOnNewMessage(String roomId, String senderId, String message,
                                       BehaviorType behaviorType, Instant sentAt) {
        try {
            // 채팅방 존재 확인
            ChatRooms chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 채팅방입니다: " + roomId));

            // 발신자 역할 조회
            String senderRole = userIdCacheService.getCachedRoleByUserId(senderId);

            if (Role.ROLE_BUYER.toString().equals(senderRole)) {
                // 구매자가 보낸 메시지 → 판매자의 안읽은 개수 증가
                chatRoomRepository.updateLastMessageAndIncrementSellerUnread(
                        roomId, message, sentAt, senderId, behaviorType, 1);
                log.debug("구매자 → 판매자 메시지: roomId={}, 판매자 안읽은 개수 +1", roomId);

            } else if (Role.ROLE_SELLER.toString().equals(senderRole)) {
                // 판매자가 보낸 메시지 → 구매자의 안읽은 개수 증가
                chatRoomRepository.updateLastMessageAndIncrementBuyerUnread(
                        roomId, message, sentAt, senderId, behaviorType, 1);
                log.debug("판매자 → 구매자 메시지: roomId={}, 구매자 안읽은 개수 +1", roomId);

            } else {
                throw new IllegalStateException("허용되지 않은 역할입니다: " + senderRole);
            }

        } catch (Exception e) {
            log.error("채팅방 업데이트 실패: roomId={}, senderId={}", roomId, senderId, e);
            throw e;
        }
    }

    /**
     * 메시지 읽음 처리
     * - 사용자의 안읽은 메시지 개수 초기화
     * - 마지막 읽음 시간 업데이트
     */
    @Override
    @MongoTransactional
    public void markMessagesAsRead(String roomId, String userId) {
        try {
            // 채팅방 존재 확인
            ChatRooms chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 채팅방입니다: " + roomId));

            // 사용자 역할 조회
            String userRole = userIdCacheService.getCachedRoleByUserId(userId);
            Instant readAt = Instant.now();

            if (Role.ROLE_BUYER.toString().equals(userRole)) {
                // 구매자가 메시지를 읽음
                chatRoomRepository.resetBuyerUnreadCount(roomId);
                chatRoomRepository.updateBuyerLastReadAt(roomId, readAt);
                log.debug("구매자 메시지 읽음 처리: roomId={}, userId={}", roomId, userId);

            } else if (Role.ROLE_SELLER.toString().equals(userRole)) {
                // 판매자가 메시지를 읽음
                chatRoomRepository.resetSellerUnreadCount(roomId);
                chatRoomRepository.updateSellerLastReadAt(roomId, readAt);
                log.debug("판매자 메시지 읽음 처리: roomId={}, userId={}", roomId, userId);

            } else {
                throw new IllegalStateException("허용되지 않은 역할입니다: " + userRole);
            }

        } catch (Exception e) {
            log.error("메시지 읽음 처리 실패: roomId={}, userId={}", roomId, userId, e);
            throw e;
        }
    }

    /**
     * 사용자의 특정 채팅방 안읽은 메시지 개수 조회
     */
    @Override
    public int getUnreadCount(String roomId, String userId) {
        try {
            ChatRooms chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 채팅방입니다: " + roomId));

            String userRole = userIdCacheService.getCachedRoleByUserId(userId);

            if (Role.ROLE_BUYER.toString().equals(userRole)) {
                return chatRoom.getBuyerUnreadCount();
            } else if (Role.ROLE_SELLER.toString().equals(userRole)) {
                return chatRoom.getSellerUnreadCount();
            } else {
                throw new IllegalStateException("허용되지 않은 역할입니다: " + userRole);
            }

        } catch (Exception e) {
            log.error("안읽은 메시지 개수 조회 실패: roomId={}, userId={}", roomId, userId, e);
            return 0;
        }
    }

    /**
     * 사용자의 전체 안읽은 메시지 개수 조회
     */
    @Override
    public int getTotalUnreadCount(String userId) {
        try {
            String userRole = userIdCacheService.getCachedRoleByUserId(userId);

            if (Role.ROLE_BUYER.toString().equals(userRole)) {
                return chatRoomRepository.findByBuyerIdOrderByLastMessageAtDesc(userId)
                        .stream()
                        .mapToInt(ChatRooms::getBuyerUnreadCount)
                        .sum();

            } else if (Role.ROLE_SELLER.toString().equals(userRole)) {
                return chatRoomRepository.findBySellerIdOrderByLastMessageAtDesc(userId)
                        .stream()
                        .mapToInt(ChatRooms::getSellerUnreadCount)
                        .sum();

            } else {
                throw new IllegalStateException("허용되지 않은 역할입니다: " + userRole);
            }

        } catch (Exception e) {
            log.error("전체 안읽은 메시지 개수 조회 실패: userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 채팅방의 마지막 메시지 정보만 업데이트 (안읽은 개수 변경 없이)
     */
    @Override
    @MongoTransactional
    public void updateLastMessageOnly(String roomId, String message, Instant sentAt,
                                      String senderId, BehaviorType behaviorType) {
        try {
            chatRoomRepository.updateLastMessage(roomId, message, sentAt, senderId, behaviorType);
            log.debug("마지막 메시지 정보 업데이트: roomId={}", roomId);

        } catch (Exception e) {
            log.error("마지막 메시지 업데이트 실패: roomId={}", roomId, e);
            throw e;
        }
    }



}
