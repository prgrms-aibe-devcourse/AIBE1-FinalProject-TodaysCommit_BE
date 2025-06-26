package com.team5.catdogeats.chats.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.chats.domain.dto.PublishDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // ⭐ 반드시 public + 시그니처 일치
    public void onMessage(String messageJson) {
        try {
            PublishDTO dto = objectMapper.readValue(messageJson, PublishDTO.class);

            // Redis pub 시에 roomId를 같이 넣었으므로 dto에서 바로 꺼내면 채널 인자 필요X
            messagingTemplate.convertAndSend(
                    "/sub/chat/room/" + dto.roomId(),   // 브라우저가 구독 중인 경로
                    dto);

            log.debug("Redis → WS relay : {}", dto);
        } catch (Exception e) {
            log.error("Redis subscriber error", e);
        }
    }
}
