package com.team5.catdogeats.chats.service;

import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import org.springframework.data.domain.Page;

public interface ChatService {
    ChatMessages save(ChatMessageDTO chatMessageDTO);

    Page<ChatMessages> getRecentMessages(String roomId, int page, int size);
}
