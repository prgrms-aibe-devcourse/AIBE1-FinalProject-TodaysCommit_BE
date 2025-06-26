package com.team5.catdogeats.global.config;

import com.team5.catdogeats.chats.interceptor.WebSocketAuthChannelInterceptor;
import com.team5.catdogeats.chats.interceptor.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    private final WebSocketAuthChannelInterceptor channelInterceptor;

    @Value("${websocket.endpoint}")
    private String wsEndpoint;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/sub", "/queue");
        config.setApplicationDestinationPrefixes("/pub");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint(wsEndpoint)
                .setAllowedOriginPatterns("*")
                // 커스텀 핸드셰이크 핸들러 등록
                .setHandshakeHandler(createHandshakeHandler())
                .addInterceptors(handshakeInterceptor);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(channelInterceptor);
    }

    /**
     * 핸드셰이크 시 STOMP 세션의 Principal을 userId로 설정하기 위한 핸들러
     */
    private HandshakeHandler createHandshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request,
                                              WebSocketHandler wsHandler,
                                              Map<String, Object> attributes) {
                // HandshakeInterceptor에서 attributes에 저장한 "userId" 값을 꺼내서 Principal로 반환
                Object uid = attributes.get("userId");
                if (uid instanceof String) {
                    return () -> (String) uid;
                }
                // fallback: 기존 세션 principal 사용
                return super.determineUser(request, wsHandler, attributes);
            }
        };
    }
}
