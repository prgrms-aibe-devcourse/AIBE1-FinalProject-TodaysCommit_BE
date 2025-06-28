package com.team5.catdogeats.auth.util;

import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategy;
import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategyFactory;
import com.team5.catdogeats.auth.dto.TokenDTO;
import com.team5.catdogeats.global.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenFactory {
    private final OAuth2ProviderStrategyFactory strategyFactory;
    private final JwtConfig jwtConfig;

    public TokenDTO createFromAuthentication(Authentication authentication) {
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        OAuth2ProviderStrategy strategy = strategyFactory.getStrategy(registrationId);
        String providerId = strategy.extractProviderId(oAuth2User);

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expiration = now.plus(jwtConfig.getExpiration(), ChronoUnit.MILLIS);

        return new TokenDTO(providerId, authorities, registrationId, now, expiration);
    }
}
