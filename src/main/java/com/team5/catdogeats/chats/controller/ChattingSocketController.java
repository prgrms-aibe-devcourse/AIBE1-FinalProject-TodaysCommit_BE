package com.team5.catdogeats.chats.controller;

import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChattingSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/v1/chat/message")
    public void onMessage(ChatMessageDTO dto, Principal principal ) {
        if (principal == null) {
            return;
        }

        ChatMessages saved = chatService.save(dto, principal);
        messagingTemplate.convertAndSend("/sub/chat/room/" + saved.getRoomId(), dto);
    }
}
