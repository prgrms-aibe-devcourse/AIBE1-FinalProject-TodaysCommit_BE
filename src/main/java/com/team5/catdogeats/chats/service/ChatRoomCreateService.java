package com.team5.catdogeats.chats.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;

public interface ChatRoomCreateService {
    ChatRooms createRoom(UserPrincipal userPrincipal, String vendorName);
}
