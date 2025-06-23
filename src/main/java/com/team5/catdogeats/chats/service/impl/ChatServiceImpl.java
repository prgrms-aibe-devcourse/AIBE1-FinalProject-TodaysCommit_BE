package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.repository.ChatMessageRepository;
import com.team5.catdogeats.chats.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendMessage(ChatMessageDTO chatMessageDTO) {
        ChatRooms room = chatRoomRepository.findById(chatMessageDTO.roomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));
        ChatMessages saved = chatMessageRepository.save(ChatMessages.builder()
                .chatRoom(room)
                .senderId(chatMessageDTO.senderId())
                .message(chatMessageDTO.message())
                .sentAt(ZonedDateTime.now())
                .behaviorType(chatMessageDTO.behaviorType())
                .senderType(chatMessageDTO.senderType())
                .isRead(false)
                .build());
    }
}
