package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.repository.ChatMessageRepository;
import com.team5.catdogeats.chats.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatMessageRepository chatMessageRepository;

    public ChatMessages save(ChatMessageDTO dto) {
        ChatMessages message = ChatMessages.builder()
                .roomId(dto.roomId())
                .senderId(dto.senderId())
                .senderType(dto.senderType())
                .behaviorType(dto.behaviorType())
                .message(dto.message())
                .sentAt(ZonedDateTime.now())
                .isRead(false)
                .build();

        return chatMessageRepository.save(message);
    }

    public Page<ChatMessages> getRecentMessages(String roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        return chatMessageRepository.findByRoomIdOrderBySentAtDesc(roomId, pageable);
    }

}
