package com.team5.catdogeats.chats.interceptor;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.auth.service.JwtService;
import com.team5.catdogeats.auth.util.JwtUtils;
import com.team5.catdogeats.chats.domain.dto.InterceptorDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
    private final JwtService jwtService;
    private final JwtUtils jwtUtils;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String token = (String) Objects.requireNonNull(accessor.getSessionAttributes()).get("token");
            InterceptorDTO dto = extractor(token);

            validate(dto);

            // Step 2: 토큰을 검증하고 Authentication 생성
            Authentication auth = jwtService.getAuthentication(dto.userPrincipal());

            // Step 3: SecurityContext에 저장
            accessor.setUser(auth); // principal 전달용
            SecurityContextHolder.getContext().setAuthentication(auth);
            }
            return message;
        }

    private void validate(InterceptorDTO dto) {
        if (Objects.equals(dto.role(), Role.ROLE_WITHDRAWN.toString()) || Objects.equals(dto.role(), Role.ROLE_TEMP.toString())) {
            throw new IllegalStateException("인증 정보가 올바르지 않은 유저입니다.");
        }
    }


    private InterceptorDTO extractor(String token) {
        Claims claims = jwtUtils.parseToken(token);
        String providerId = claims.getSubject();
        String provider = (String) claims.get("provider");
        String authorities = (String) claims.get("authorities");
        UserPrincipal userPrincipal = new UserPrincipal(provider, providerId);
        return new InterceptorDTO(provider, providerId, authorities, userPrincipal);
    }

}
