package com.team5.catdogeats.auth.service.impl;

import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategy;
import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategyFactory;
import com.team5.catdogeats.auth.service.JwtService;
import com.team5.catdogeats.global.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {
    private final OAuth2ProviderStrategyFactory oAuth2ProviderStrategyFactory;
    private final JwtConfig jwtConfig;

    @Override
    public String generateAccessToken(Authentication authentication) {
        try {

            String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            OAuth2ProviderStrategy strategy = oAuth2ProviderStrategyFactory.getStrategy(registrationId);
            String providerId = strategy.extractProviderId(oAuth2User);

            String authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));

            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            ZonedDateTime expiration = now.plus(jwtConfig.getExpiration(), ChronoUnit.MILLIS);

            return Jwts.builder()
                    .subject(providerId)
                    .claim("authorities",authorities)
                    .claim("provider", registrationId)
                    .issuedAt(Date.from(now.toInstant()))
                    .expiration(Date.from(expiration.toInstant()))
                    .issuer("cake7-auth-server") // ✅ 발급자 설정
                    .audience().add("cake7-client").and()
                    .signWith(jwtConfig.secretKey(), Jwts.SIG.HS256)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Authentication getAuthentication(String subject) {
        return null;
    }
}
