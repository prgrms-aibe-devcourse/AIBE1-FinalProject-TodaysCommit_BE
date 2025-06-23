package com.team5.catdogeats.chats.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;

import java.util.List;

public interface ChatService {
    ChatMessages save(ChatMessageDTO chatMessageDTO, UserPrincipal userPrincipal);

    List<ChatMessages> getRecentMessages(String roomId, int page, int size);
}
