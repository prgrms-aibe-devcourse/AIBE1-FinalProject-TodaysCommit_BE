package com.team5.catdogeats.auth.service.impl;

import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategy;
import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategyFactory;
import com.team5.catdogeats.auth.dto.AuthenticationDTO;
import com.team5.catdogeats.auth.dto.TokenDTO;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.auth.service.JwtService;
import com.team5.catdogeats.auth.util.TokenFactory;
import com.team5.catdogeats.global.config.JwtConfig;
import com.team5.catdogeats.global.exception.TokenErrorException;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {
    private final TokenFactory tokenFactory;
    private final OAuth2ProviderStrategyFactory strategyFactory;
    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;

    @Override
    public String createAccessToken(Authentication authentication) {
        try {
            TokenDTO dto = tokenFactory.createFromAuthentication(authentication);
            return getCompact(dto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Authentication getAuthentication(UserPrincipal userPrincipal) {
        try {

            Users user = userRepository.findByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                    .orElseThrow(() -> new NoSuchElementException("User not found for provider: " + userPrincipal.provider() + ", id: " + userPrincipal.providerId()));

            AuthenticationDTO authDTO = getUser(user.getRole().toString(), user.getProvider());

            OAuth2User oAuth2User = new DefaultOAuth2User(
                    authDTO.authorities(),
                    authDTO.providerStrategy().buildUserAttributes(user),
                    user.getUserNameAttribute()
            );

            return new OAuth2AuthenticationToken(oAuth2User, authDTO.authorities(), user.getProvider());

        } catch (TokenErrorException e) {
            log.error("Error getting authentication: {}", e.getMessage());
            throw new TokenErrorException("Error getting authentication: " + e.getMessage());
        }
    }

    private AuthenticationDTO getUser(String role, String provider) {
        List<SimpleGrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(role));

        OAuth2ProviderStrategy strategy = strategyFactory.getStrategy(provider);

        return new AuthenticationDTO(authorities, strategy);
    }

    private String getCompact(TokenDTO tokenDTO) {
        return Jwts.builder()
                .subject(tokenDTO.providerId())
                .claim("authorities", tokenDTO.authorities())
                .claim("provider", tokenDTO.registrationId())
                .issuedAt(Date.from(tokenDTO.now().toInstant()))
                .expiration(Date.from(tokenDTO.expiration().toInstant()))
                .issuer("cake7-auth-server") // ✅ 발급자 설정
                .audience().add("cake7-client").and()
                .signWith(jwtConfig.secretKey(), Jwts.SIG.HS256)
                .compact();
    }
}
