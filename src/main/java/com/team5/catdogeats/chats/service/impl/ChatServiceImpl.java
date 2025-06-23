package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.mongo.repository.ChatMessageRepository;
import com.team5.catdogeats.chats.service.ChatService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatMessages save(ChatMessageDTO dto, UserPrincipal principal) {
       try {
           Users user = validate(principal);
           log.debug("✅ [인증 유저] ID: {}, ROLE: {}", user.getId(), user.getRole());

           ChatMessages message = ChatMessages.builder()
                   .roomId(dto.roomId())
                   .senderId(user.getId())
                   .senderType(user.getRole())
                   .behaviorType(dto.behaviorType())
                   .message(dto.message())
                   .sentAt(Instant.now())
                   .isRead(false)
                   .build();

           return chatMessageRepository.save(message);
       } catch (Exception e) {
           log.error("❌ [채팅 저장 실패]", e);
           throw e;
       }

    }


    public List<ChatMessages> getRecentMessages(String roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        return chatMessageRepository.findByRoomIdOrderBySentAtDesc(roomId, pageable);
    }

    private Users validate(UserPrincipal principal) {

        Users user = userRepository.findByProviderAndProviderId(principal.provider(), principal.providerId())
                .orElseThrow(() -> new IllegalStateException("인증 정보가 올바르지 않은 유저입니다."));

        if (Role.ROLE_TEMP.equals(user.getRole()) || Role.ROLE_WITHDRAWN.equals(user.getRole())){
            throw new IllegalStateException("인증 정보가 올바르지 않은 유저입니다.");
        }
        return user;
    }

}
