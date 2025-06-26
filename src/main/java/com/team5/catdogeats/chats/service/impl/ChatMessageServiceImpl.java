package com.team5.catdogeats.chats.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.domain.dto.PublishDTO;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.mongo.repository.ChatMessageRepository;
import com.team5.catdogeats.chats.service.ChatMessageService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.chats.util.MakeKeyString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserIdCacheService userIdCacheService;

    @Override
    public ChatMessageDTO saveAndPublish(ChatMessageDTO dto, UserPrincipal userPrincipal) {
        try {
            String userId = userIdCacheService.getCachedUserId(userPrincipal.provider(),
                                                            userPrincipal.providerId());
            if (userId == null) throw new IllegalStateException("유저 정보를 찯을 수 없습니다.");

            // 1. MongoDB 영속
            String id = UUID.randomUUID().toString();
            ChatMessages messages = ChatMessages.builder()
                                    .id(id)
                                    .roomId(dto.roomId())
                                    .senderId(userId)
                                    .message(dto.message())
                                    .behaviorType(dto.behaviorType())
                                    .sentAt(dto.sentAt())
                                    .isRead(false)
                                    .sentAt(dto.sentAt() != null ? dto.sentAt() : Instant.now())
                                    .build();
            chatMessageRepository.save(messages);

            // 2. Redis Pub/Sub 발행
            PublishDTO publish = PublishDTO.builder()
                    .roomId(dto.roomId())
                    .senderId(userId)  // 👈 senderId 추가!
                    .message(dto.message())
                    .behaviorType(dto.behaviorType())
                    .sentAt(dto.sentAt() != null ? dto.sentAt() : Instant.now())
                    .build();
            String channel = MakeKeyString.makeRoomId("chat-room" , dto.roomId());
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(publish));
            return dto;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis publish failed");
        }

    }

}
