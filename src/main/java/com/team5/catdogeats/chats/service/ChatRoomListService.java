package com.team5.catdogeats.chats.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.dto.ChatRoomListDTO;
import com.team5.catdogeats.chats.domain.dto.ChatRoomPageRequestDTO;
import com.team5.catdogeats.chats.domain.dto.ChatRoomPageResponseDTO;

public interface ChatRoomListService {
    ChatRoomPageResponseDTO<ChatRoomListDTO> getChatRooms(UserPrincipal userPrincipal,
                                                             ChatRoomPageRequestDTO pageRequest);
}
