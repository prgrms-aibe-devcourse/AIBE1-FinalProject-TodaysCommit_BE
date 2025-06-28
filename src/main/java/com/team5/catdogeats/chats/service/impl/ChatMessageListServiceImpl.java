package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.dto.ChatMessageListDTO;
import com.team5.catdogeats.chats.domain.dto.ChatMessagePageRequestDTO;
import com.team5.catdogeats.chats.domain.dto.ChatMessagePageResponseDTO;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.mongo.repository.ChatMessageRepository;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatMessageListService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageListServiceImpl implements ChatMessageListService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserIdCacheService userIdCacheService;

    @Override
    public ChatMessagePageResponseDTO<ChatMessageListDTO> getMessagesWithCursor(String roomId,
                                                                                ChatMessagePageRequestDTO pageRequest,
                                                                                UserPrincipal userPrincipal) {
        String currentUserId = getUserId(userPrincipal);

        Instant cursor = pageRequest.getCursorAsInstant();
        int size = pageRequest.size();
        Pageable pageable = PageRequest.of(0, size + 1); // +1 for hasNext 판단

        List<ChatMessages> messages = (cursor != null)
                ? chatMessageRepository.findByRoomIdAndSentAtLessThanOrderBySentAtDesc(roomId, cursor, pageable)
                : chatMessageRepository.findByRoomIdOrderBySentAtDesc(roomId, pageable);
        boolean hasNext = messages.size() > size;

        if (hasNext) {
            messages = messages.subList(0, size);
        }

        List<ChatMessageListDTO> result = messages.stream()
                .map(msg -> ChatMessageListDTO.fromEntity(msg, currentUserId))
                .toList();


        String nextCursor = (hasNext && !messages.isEmpty())
                ? messages.get(messages.size() - 1).getSentAt().toString()
                : null;

        return ChatMessagePageResponseDTO.of(result, nextCursor, hasNext, size);

    }

    private String getUserId(UserPrincipal userPrincipal) {
        String userId = userIdCacheService.getCachedUserId(userPrincipal.provider(), userPrincipal.providerId());
        if (userId == null) {
            userIdCacheService.cacheUserIdAndRole(userPrincipal.provider(), userPrincipal.providerId());
            userId = userIdCacheService.getCachedUserId(userPrincipal.provider(), userPrincipal.providerId());
        }
        return userId;
    }
}

