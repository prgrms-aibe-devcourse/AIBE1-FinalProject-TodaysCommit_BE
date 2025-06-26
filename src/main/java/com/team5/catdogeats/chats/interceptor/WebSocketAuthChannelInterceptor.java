package com.team5.catdogeats.chats.interceptor;

import com.team5.catdogeats.auth.util.JwtUtils;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.users.domain.enums.Role;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
    private final UserIdCacheService userIdCacheService;
    private final JwtUtils jwtUtils;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand cmd = accessor.getCommand();
        log.debug("▶ preSend() called, STOMP Command = {}", cmd);

        if (StompCommand.CONNECT.equals(cmd)) {
            // 1) 세션 속성에서 토큰 추출
            String token = (String) Objects.requireNonNull(accessor.getSessionAttributes()).get("token");
            log.debug("CONNECT 토큰 확인: {}", token);

            // 2) JWT 파싱 및 유효성 검증
            Claims claims = jwtUtils.parseToken(token);
            String provider = claims.get("provider", String.class);
            String providerId = claims.getSubject();
            String role = claims.get("authorities", String.class);
            validateRole(role);

            // 3) Redis 캐시된 userId 확보 (없으면 저장)
            String userId = userIdCacheService.getCachedUserId(provider, providerId);
            if (userId == null) {
                userIdCacheService.cacheUserIdAndRole(provider, providerId);
                userId = userIdCacheService.getCachedUserId(provider, providerId);
            }
            log.debug("Authenticated userId: {}", userId);

            // 4) 권한 리스트 구성
            List<SimpleGrantedAuthority> authorities = Arrays.stream(role.split(","))
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            // 5) Authentication 생성 (principal = userId)
            Authentication auth = new UsernamePasswordAuthenticationToken(userId, token, authorities);
            log.debug("auth: {}", auth.getName());
            accessor.setUser(auth);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        return message;
    }

    private void validateRole(String role) {
        if (Role.ROLE_WITHDRAWN.toString().equals(role) || Role.ROLE_TEMP.toString().equals(role)) {
            throw new IllegalStateException("인증 정보가 올바르지 않은 유저입니다.");
        }
    }
}
