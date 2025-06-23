package com.team5.catdogeats.auth.service.impl;

import com.team5.catdogeats.auth.dto.RotateTokenDTO;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.auth.redis.RefreshTokens;
import com.team5.catdogeats.auth.repository.RefreshTokensRedisRepository;
import com.team5.catdogeats.auth.service.JwtService;
import com.team5.catdogeats.auth.service.RefreshTokenService;
import com.team5.catdogeats.auth.service.RotateRefreshTokenService;
import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.global.exception.ExpiredTokenException;
import com.team5.catdogeats.global.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RotateRefreshTokenServiceImpl implements RotateRefreshTokenService {
    private final RefreshTokensRedisRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @JpaTransactional
    public RotateTokenDTO RotateRefreshToken(String refreshTokenId) {
        RefreshTokens token = refreshTokenRepository.findById(refreshTokenId)
                .orElseThrow(() -> new NoSuchElementException("Refresh token not found"));

        validateToken(refreshTokenId, token);
        token.markUsed();
        RefreshTokens newToken = refreshTokenRepository.save(token);

        return buildRefreshTokens(newToken);
    }

    private void validateToken(String refreshTokenId, RefreshTokens token) {
        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Expired or invalid refresh token: {}", refreshTokenId);
            refreshTokenRepository.deleteByUserId(token.getUserId());
            throw new ExpiredTokenException();
        }

        if (token.isUsed()) {
            log.warn("Token reuse detected: {}", refreshTokenId);
            refreshTokenRepository.deleteByUserId(token.getUserId());
            throw new InvalidTokenException();
        }
    }

    private RotateTokenDTO buildRefreshTokens(RefreshTokens token) {
        // 새 토큰 발급
        UserPrincipal principal = new UserPrincipal(token.getProvider(), token.getProviderId());

        Authentication authentication = jwtService.getAuthentication(principal);
        String newAccessToken = jwtService.createAccessToken(authentication);
        String newRefreshToken = refreshTokenService.createRefreshToken(authentication);

        return new RotateTokenDTO(newAccessToken, newRefreshToken, "Cookie", 60 * 60 * 24);
    }
}
