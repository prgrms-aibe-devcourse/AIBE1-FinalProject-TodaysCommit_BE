package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.repository.ChatMessageRepository;
import com.team5.catdogeats.chats.service.ChatService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatMessages save(ChatMessageDTO dto, Principal principal) {
        Users user = validate((OAuth2AuthenticationToken) principal);

        ChatMessages message = ChatMessages.builder()
                .roomId(dto.roomId())
                .senderId(user.getId())
                .senderType(user.getRole())
                .behaviorType(dto.behaviorType())
                .message(dto.message())
                .sentAt(ZonedDateTime.now())
                .isRead(false)
                .build();

        return chatMessageRepository.save(message);
    }


    public List<ChatMessages> getRecentMessages(String roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        return chatMessageRepository.findByRoomIdOrderBySentAtDesc(roomId, pageable);
    }

    private Users validate(OAuth2AuthenticationToken principal) {
        OAuth2User users = principal.getPrincipal();

        String providerId = users.getAttribute("providerId");
        String provider = users.getAttribute("provider");
        Collection<? extends GrantedAuthority> authorities = users.getAuthorities();

        String role = authorities.stream()
                .map(GrantedAuthority::getAuthority).findFirst().orElse(null);

        if (Role.ROLE_WITHDRAWN.toString().equals(role) || Role.ROLE_TEMP.toString().equals(role)) {
            throw new IllegalStateException("인증 정보가 올바르지 않은 유저입니다.");
        }

        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new IllegalStateException("인증 정보가 올바르지 않은 유저입니다."));
    }

}
