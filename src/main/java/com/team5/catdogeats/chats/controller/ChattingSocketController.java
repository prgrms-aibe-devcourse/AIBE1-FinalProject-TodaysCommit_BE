package com.team5.catdogeats.chats.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChattingSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/message")
    public void onMessage(ChatMessageDTO dto, Principal principal) {
        log.debug("🔔 메시지 수신됨. DTO: {}, Principal: {}", dto, principal);

        if (principal == null) {
            return;
        }

        if (!(principal instanceof Authentication auth)) {
            log.warn("principal is not Authentication, skipping");
            return;
        }

        // 실제 UserPrincipal 꺼내기
        Object raw = auth.getPrincipal();
        if (!(raw instanceof UserPrincipal userPrincipal)) {
            log.warn("principal.getPrincipal() is not UserPrincipal, skipping");
            return;
        }

        ChatMessages saved = chatService.save(dto, userPrincipal);
        messagingTemplate.convertAndSend("/sub/chat/room/" + saved.getRoomId(), dto);
    }
}
