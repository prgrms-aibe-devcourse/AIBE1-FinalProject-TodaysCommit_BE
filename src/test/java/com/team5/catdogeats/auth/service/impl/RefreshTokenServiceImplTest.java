package com.team5.catdogeats.auth.service.impl;

import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategy;
import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategyFactory;
import com.team5.catdogeats.auth.redis.RefreshTokens;
import com.team5.catdogeats.auth.repository.RefreshTokensRedisRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock private OAuth2ProviderStrategyFactory strategyFactory;
    @Mock private OAuth2ProviderStrategy strategy;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokensRedisRepository refreshTokenRepository;

    @InjectMocks private RefreshTokenServiceImpl refreshTokenService;

    private OAuth2AuthenticationToken authentication;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        OAuth2User oAuth2User = mock(OAuth2User.class);
        authentication = mock(OAuth2AuthenticationToken.class);

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        String provider = "google";
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn(provider);
        when(strategyFactory.getStrategy(provider)).thenReturn(strategy);
        String providerId = "12345";
        when(strategy.extractProviderId(oAuth2User)).thenReturn(providerId);

        Users user = Users.builder().id(userId).build();
        when(userRepository.findByProviderAndProviderId(provider, providerId))
                .thenReturn(Optional.of(user));
    }

    @Test
    void createRefreshToken_ShouldCreateNewTokenSuccessfully() {
        when(refreshTokenRepository.findByUserIdAndUsedIsFalse(userId)).thenReturn(List.of());

        ArgumentCaptor<RefreshTokens> captor = ArgumentCaptor.forClass(RefreshTokens.class);
        when(refreshTokenRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        String result = refreshTokenService.createRefreshToken(authentication);

        RefreshTokens saved = captor.getValue();
        assertNotNull(result);
        assertEquals(userId, saved.getUserId());
        assertFalse(saved.isUsed());
    }
}
