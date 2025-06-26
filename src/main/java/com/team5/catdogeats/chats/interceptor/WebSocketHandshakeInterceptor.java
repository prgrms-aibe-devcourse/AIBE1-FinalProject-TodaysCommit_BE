package com.team5.catdogeats.chats.interceptor;

import com.team5.catdogeats.auth.util.JwtUtils;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtUtils jwtUtils;
    private final UserIdCacheService userIdCacheService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            String token = jwtUtils.extractToken(httpRequest);
            log.debug("  – CONNECT 토큰 = {}", token);

            if(StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
                attributes.put("token", token);
                log.debug("1번 이터센터에 토큰이 나오는가 {}", token);
                Claims claims = jwtUtils.parseToken(token);
                String provider = claims.get("provider", String.class);
                String providerId = claims.getSubject();
                String userId = userIdCacheService.getCachedUserId(provider, providerId);
                if (userId == null) {
                    userIdCacheService.cacheUserIdAndRole(provider, providerId);
                    userId = userIdCacheService.getCachedUserId(provider, providerId);
                }
                attributes.put("userId", userId);
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        if (exception == null) {
            log.info("WebSocket 연결 성공");
        } else {
            log.warn("WebSocket 연결 실패: {}", exception.getMessage());
        }
    }
}
