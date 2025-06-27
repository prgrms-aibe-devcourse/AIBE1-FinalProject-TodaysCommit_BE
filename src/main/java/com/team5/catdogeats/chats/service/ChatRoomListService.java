package com.team5.catdogeats.chats.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.dto.ChatRoomListDTO;

import java.util.List;

public interface ChatRoomListService {
    List<ChatRoomListDTO> getAllChatRooms(UserPrincipal userPrincipal);
}
