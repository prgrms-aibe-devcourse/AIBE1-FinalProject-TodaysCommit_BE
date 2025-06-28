package com.team5.catdogeats.chats.controller;

import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.domain.dto.ReadReceiptDTO;
import com.team5.catdogeats.chats.service.ChatMessageService;
import com.team5.catdogeats.chats.service.ChatRoomUpdateService;
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

    private final ChatMessageService chatMessageService;
    private final ChatRoomUpdateService chatRoomUpdateService;

    @MessageMapping("/chat/message")
    public void onMessage(@Payload ChatMessageDTO dto, Principal principal) {
        log.debug("🔔 메시지 수신됨. DTO: {}, Principal: {}", dto, principal);
        if (principal == null) {
            log.warn("유저 정보가 없습니다.");
            return;
        }

        // 세션에 설정된 Principal.getName() == userId(UUID)
        String userId = principal.getName();


        // 실제 저장/발행 호출
        chatMessageService.saveAndPublish(dto, userId);
    }

    @MessageMapping("/chat/read")
    public void markAsRead(@Payload ReadReceiptDTO readReceipt, Principal principal) {
        log.debug("메시지 읽음");
        if (principal == null) {
            log.warn("유저 정보가 없습니다.");
            return;
        }

        try {
            String userId = principal.getName();
            log.debug("읽음 처리 요청: userId={}, roomId={}", userId, readReceipt.roomId());

            chatRoomUpdateService.markMessagesAsRead(readReceipt.roomId(), userId);

        } catch (Exception e) {
            log.error("읽음 처리 중 오류 발생: roomId={}", readReceipt.roomId(), e);
        }
    }
}
