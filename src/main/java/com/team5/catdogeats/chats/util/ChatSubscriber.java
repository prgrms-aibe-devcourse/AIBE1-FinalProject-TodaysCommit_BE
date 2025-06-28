package com.team5.catdogeats.chats.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.chats.domain.dto.PublishDTO;
import com.team5.catdogeats.chats.domain.dto.SelfDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSubscriber implements MessageListener {

    private final SimpMessagingTemplate template;
    private final ObjectMapper mapper;

    /** Redis → STOMP 중계 */
    @Override
    public void onMessage(Message redisMsg, byte[] pattern) {
        // Redis 채널에서 받은 원본 채널명 (예: "user:<userId>")
        String channel = new String(redisMsg.getChannel(), StandardCharsets.UTF_8);
        // "user:" 접두어 제거하여 pure userId로 변환
        String principalName = channel.startsWith("user:")
                ? channel.substring("user:".length())
                : channel;

        // 메시지 바디(직렬화된 JSON)
        String json = new String(redisMsg.getBody(), StandardCharsets.UTF_8);

        try {
            // JSON → DTO 변환 (isMe flag 확인)
            Object dto;
            if (json.contains("\"isMe\":true")) {
                dto = mapper.readValue(json, SelfDTO.class);
            } else {
                dto = mapper.readValue(json, PublishDTO.class);
            }

            // STOMP user-destination으로 전송 (principalName == userId)
            template.convertAndSendToUser(
                    principalName,
                    "/queue/chat",
                    dto
            );

            log.debug("Relayed to /user/{}/queue/chat → {}", principalName, dto);
        } catch (Exception e) {
            log.error("Redis → WS relay error for channel: {}, message: {}", channel, json, e);
        }
    }
}
