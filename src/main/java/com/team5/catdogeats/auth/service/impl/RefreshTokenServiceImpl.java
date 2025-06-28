package com.team5.catdogeats.auth.service.impl;

import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategy;
import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategyFactory;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.auth.redis.RefreshTokens;
import com.team5.catdogeats.auth.repository.RefreshTokensRedisRepository;
import com.team5.catdogeats.auth.service.RefreshTokenService;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final OAuth2ProviderStrategyFactory strategyFactory;
    private final UserRepository userRepository;
    private final RefreshTokensRedisRepository refreshTokenRepository;
    private static final int MAX_TOKENS_PER_USER = 3;
    
    @Override
    @JpaTransactional
    public String createRefreshToken(Authentication authentication) {
        UserPrincipal principal = getUserPrincipal(authentication);
        log.debug("로그가 나가는지 테스트입니다");
        Users user = userRepository.findByProviderAndProviderId(principal.provider(), principal.providerId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        RefreshTokens newToken = buildRefreshTokens(principal, user);

        verification(user);
        RefreshTokens token = refreshTokenRepository.save(newToken);
        log.debug("Created refresh token: {}", token.getId());
        return token.getId();
    }

    private void verification(Users user) {
        List<RefreshTokens> tokens = refreshTokenRepository
                .findByUserIdAndUsedIsFalse(user.getId());

        log.debug("토큰 리스트가 나오는지 검증 로그 Tokens: {}", tokens);

        if (tokens.size() >= MAX_TOKENS_PER_USER) {
            for (int i = 0; i < tokens.size() - (MAX_TOKENS_PER_USER - 1); i++) {
                refreshTokenRepository.deleteById(tokens.get(i).getId());
                log.debug("Deleted refresh token: {}", tokens.get(i));
            }
        }
    }

    private RefreshTokens buildRefreshTokens(UserPrincipal principal, Users user) {
        String id = UUID.randomUUID().toString();
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expiresAt = now.plusDays(1);

        return  RefreshTokens.builder()
                .id(id)
                .provider(principal.provider())
                .providerId(principal.providerId())
                .userId(user.getId())
                .used(false)
                .expiresAt(expiresAt.toInstant())
                .createdAt(now.toInstant())
                .build();
    }

    private UserPrincipal getUserPrincipal(Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

        OAuth2ProviderStrategy strategy = strategyFactory.getStrategy(registrationId);
        String providerId = strategy.extractProviderId(oAuth2User);

        return new UserPrincipal(registrationId, providerId);
    }
}
