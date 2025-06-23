package com.team5.catdogeats.chats.service;

import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;

public interface ChatService {
    void sendMessage(ChatMessageDTO chatMessageDTO);
}
