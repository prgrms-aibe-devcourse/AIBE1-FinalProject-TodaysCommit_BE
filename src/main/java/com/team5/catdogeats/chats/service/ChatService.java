package com.team5.catdogeats.chats.service;

import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;

import java.security.Principal;
import java.util.List;

public interface ChatService {
    ChatMessages save(ChatMessageDTO chatMessageDTO, Principal principal);

    List<ChatMessages> getRecentMessages(String roomId, int page, int size);
}
