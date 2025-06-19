package com.team5.catdogeats.auth.service.impl;

import com.team5.catdogeats.auth.dto.RotateTokenDTO;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.auth.redis.RefreshTokens;
import com.team5.catdogeats.auth.repository.RefreshTokensRedisRepository;
import com.team5.catdogeats.auth.service.JwtService;
import com.team5.catdogeats.auth.service.RefreshTokenService;
import com.team5.catdogeats.global.exception.ExpiredTokenException;
import com.team5.catdogeats.global.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RotateRefreshTokenServiceImplTest {

    @Mock
    private RefreshTokensRedisRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private RotateRefreshTokenServiceImpl rotateService;

    private UUID tokenId;
    private RefreshTokens validToken;

    @BeforeEach
    void setUp() {
        tokenId = UUID.randomUUID();
        validToken = RefreshTokens.builder()
                .id(tokenId)
                .provider("google")
                .providerId("12345")
                .userId(UUID.randomUUID())
                .used(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void rotateRefreshToken_ShouldReturnNewTokens_WhenValidToken() {
        // given
        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(validToken));
        when(refreshTokenRepository.save(any(RefreshTokens.class))).thenReturn(validToken);

        Authentication authentication = mock(Authentication.class);
        when(jwtService.getAuthentication(any(UserPrincipal.class))).thenReturn(authentication);
        when(jwtService.createAccessToken(authentication)).thenReturn("new-access-token");

        UUID newRefreshTokenId = UUID.randomUUID();
        when(refreshTokenService.createRefreshToken(authentication)).thenReturn(newRefreshTokenId);

        // when
        RotateTokenDTO result = rotateService.RotateRefreshToken(tokenId);

        // then
        assertNotNull(result);
        assertEquals("new-access-token", result.newAccessToken());
        assertEquals(newRefreshTokenId, result.newRefreshToken()); // UUID 비교
    }


    @Test
    void rotateRefreshToken_ShouldThrowExpiredTokenException_WhenTokenIsExpired() {
        // given
        validToken = validToken.toBuilder().expiresAt(Instant.now().minusSeconds(10)).build();
        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(validToken));

        // when & then
        assertThrows(ExpiredTokenException.class, () -> rotateService.RotateRefreshToken(tokenId));
    }

    @Test
    void rotateRefreshToken_ShouldThrowInvalidTokenException_WhenTokenIsUsed() {
        // given
        validToken = validToken.toBuilder().used(true).build();
        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(validToken));

        // when & then
        assertThrows(InvalidTokenException.class, () -> rotateService.RotateRefreshToken(tokenId));
    }

    @Test
    void rotateRefreshToken_ShouldThrowNoSuchElementException_WhenTokenNotFound() {
        // given
        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NoSuchElementException.class, () -> rotateService.RotateRefreshToken(tokenId));
    }
}
