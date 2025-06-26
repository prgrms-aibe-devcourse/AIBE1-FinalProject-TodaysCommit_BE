package com.team5.catdogeats.chats.interceptor;

import com.team5.catdogeats.auth.dto.UserPrincipal;
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
        log.debug("message: {}", message);
        log.debug("▶ preSend() called, STOMP Command = {}", cmd);
        if (StompCommand.CONNECT.equals(cmd)
                || StompCommand.SUBSCRIBE.equals(cmd)
                || StompCommand.SEND.equals(cmd)) {

            String token = (String) Objects.requireNonNull(accessor.getSessionAttributes()).get("token");
            Authentication auth = extractor(token);

            // Step 3: SecurityContext에 저장
            accessor.setUser(auth); // principal 전달용
            SecurityContextHolder.getContext().setAuthentication(auth);

            cacheUserIdIfAbsent(token);


            if (StompCommand.SEND.equals(cmd)) {
                log.debug("  – SEND frame, Principal = {}", accessor.getUser());
            }
            // you can also log SUBSCRIBE if you like:
            if (StompCommand.SUBSCRIBE.equals(cmd)) {
                log.debug("  – SUBSCRIBE to {}", accessor.getDestination());
            }
            }
            return message;
        }


    private void validate(String role) {
        if (Objects.equals(role, Role.ROLE_WITHDRAWN.toString()) || Objects.equals(role, Role.ROLE_TEMP.toString())) {
            throw new IllegalStateException("인증 정보가 올바르지 않은 유저입니다.");
        }
    }


    private Authentication extractor(String token) {
        Claims claims = jwtUtils.parseToken(token);
        String providerId = claims.getSubject();
        String provider = (String) claims.get("provider");
        String role = (String) claims.get("authorities");
        UserPrincipal userPrincipal = new UserPrincipal(provider, providerId);
        validate(role);
        List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(role.split(","))
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(userPrincipal, token, grantedAuthorities);
    }

    private void cacheUserIdIfAbsent(String token) {
        Claims claims = jwtUtils.parseToken(token);
        String providerId = claims.getSubject();
        String provider = (String) claims.get("provider");

        if (userIdCacheService.getCachedUserId(provider, providerId) == null) {
            userIdCacheService.cacheUserId(provider, providerId);
        }
        log.debug("Cached user id: {}", userIdCacheService.getCachedUserId(provider, providerId));
    }

}
