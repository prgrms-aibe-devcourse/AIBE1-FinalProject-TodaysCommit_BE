package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.domain.dto.PublishDTO;
import com.team5.catdogeats.chats.domain.dto.SelfDTO;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.mongo.repository.ChatMessageRepository;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatMessageService;
import com.team5.catdogeats.chats.service.ChatRoomUpdateService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.global.config.MongoTransactional;
import com.team5.catdogeats.users.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserIdCacheService userIdCacheService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUpdateService chatRoomUpdateService;

    @Override
    @MongoTransactional
    public ChatMessageDTO saveAndPublish(ChatMessageDTO dto, String userId) {
        try {
            // 1) 발신자 ID: WebSocket 세션에서 직접 전달된 userId

            // 2) 전송 시간
            Instant sentAt = dto.sentAt() != null ? dto.sentAt() : Instant.now();

            // 3) 채팅방 존재 확인
            ChatRooms chatRooms = chatRoomRepository.findById(dto.roomId())
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 방입니다."));

            String targetId = getTargetId(userId, chatRooms);
            log.debug("메시지 전송 준비: senderId={}, targetId={}, roomId={}",
                    userId, targetId, dto.roomId());

            saveMessage(dto, userId, sentAt);
            updateRoomInformation(dto, userId, sentAt, targetId);
            sendingSubscribe(dto, userId, sentAt, targetId);

            return dto;
        } catch (Exception e) {
            log.error("메시지 저장 및 전송 실패", e);
            throw e;
        }
    }


    private void sendingSubscribe(ChatMessageDTO dto, String userId, Instant sentAt, String targetId) {
        // 7) 발신자에게 SelfDTO 전송
        SelfDTO self = SelfDTO.builder()
                .roomId(dto.roomId())
                .senderId(userId)
                .message(dto.message())
                .behaviorType(dto.behaviorType())
                .sentAt(sentAt)
                .isMe(true)
                .unreadCount(0)
                .build();
        redisTemplate.convertAndSend("user:" + userId, self);
        log.debug("Redis 발신자 채널 전송: user:{} -> {}", userId, self);

        // 8) 수신자에게 PublishDTO 전송
        PublishDTO publish = PublishDTO.builder()
                .roomId(dto.roomId())
                .senderId(userId)
                .message(dto.message())
                .behaviorType(dto.behaviorType())
                .sentAt(sentAt)
                .isMe(false)
                .unreadCount(1)
                .build();
        redisTemplate.convertAndSend("user:" + targetId, publish);
        log.debug("Redis 수신자 채널 전송: user:{} -> {}", targetId, publish);
    }

    private void updateRoomInformation(ChatMessageDTO dto, String userId, Instant sentAt, String targetId) {
        // 7) 채팅방 정보 업데이트 (마지막 메시지 + 수신자 안읽은 개수 증가)
        chatRoomUpdateService.updateRoomOnNewMessage(
                dto.roomId(), userId, dto.message(), dto.behaviorType(), sentAt);

//        // 8) 수신자의 현재 안읽은 메시지 개수 조회
//        int receiverUnreadCount = chatRoomUpdateService.getUnreadCount(dto.roomId(), targetId);
    }

    private void saveMessage(ChatMessageDTO dto, String userId, Instant sentAt) {
        // 6) MongoDB에 메시지 저장
        String messageId = UUID.randomUUID().toString();
        ChatMessages messages = ChatMessages.builder()
                .id(messageId)
                .roomId(dto.roomId())
                .senderId(userId)
                .message(dto.message())
                .behaviorType(dto.behaviorType())
                .sentAt(sentAt)
                .build();
        chatMessageRepository.save(messages);
        log.debug("메시지 저장 완료: id={}", messageId);
    }

    private String getTargetId(String userId, ChatRooms chatRooms) {

        // 4) 발신자 Role 조회 (userId 기준)
        String role = userIdCacheService.getCachedRoleByUserId(userId);


        // 5) 수신자 ID 결정
        if (Role.ROLE_BUYER.toString().equals(role)) {
            return chatRooms.getSellerId();
        } else if (Role.ROLE_SELLER.toString().equals(role)) {
            return chatRooms.getBuyerId();
        }
        throw new IllegalStateException("허용되지 않은 역할(Role)입니다.");
    }
}
