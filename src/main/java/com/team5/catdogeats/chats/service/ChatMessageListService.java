package com.team5.catdogeats.chats.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.dto.ChatMessageListDTO;
import com.team5.catdogeats.chats.domain.dto.ChatMessagePageRequestDTO;
import com.team5.catdogeats.chats.domain.dto.ChatMessagePageResponseDTO;

public interface ChatMessageListService {
    ChatMessagePageResponseDTO<ChatMessageListDTO> getMessagesWithCursor(String roomId,
                                                                         ChatMessagePageRequestDTO pageRequest,
                                                                         UserPrincipal userPrincipal);
}
