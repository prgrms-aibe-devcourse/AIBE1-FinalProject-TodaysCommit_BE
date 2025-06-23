package com.team5.catdogeats.chats.controller;

import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/users/chat")
public class ChattingController {

    private final ChatService chatService;

    @GetMapping("/room/{roomId}/messages")
    public List<ChatMessages> getMessages(@PathVariable String roomId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size) {
        return chatService.getRecentMessages(roomId, page, size );
    }
}
