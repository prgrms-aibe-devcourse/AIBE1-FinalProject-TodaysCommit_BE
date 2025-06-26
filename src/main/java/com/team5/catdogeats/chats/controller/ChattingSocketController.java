package com.team5.catdogeats.chats.controller;

import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.service.ChatMessageService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChattingSocketController {

    private final ChatMessageService chatService;
    private final UserIdCacheService userIdCacheService;

    @MessageMapping("/chat/message")
    public void onMessage(@Payload ChatMessageDTO dto, Principal principal) {
        log.debug("🔔 메시지 수신됨. DTO: {}, Principal: {}", dto, principal);
        if (principal == null) {
            log.warn("principal is null, skipping");
            return;
        }

        // 세션에 설정된 Principal.getName() == userId(UUID)
        String userId = principal.getName();


        // 실제 저장/발행 호출
        chatService.saveAndPublish(dto, userId);
    }
}
