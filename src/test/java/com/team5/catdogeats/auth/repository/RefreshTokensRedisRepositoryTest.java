package com.team5.catdogeats.auth.repository;

import com.team5.catdogeats.auth.redis.RefreshTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataRedisTest
@ActiveProfiles("dev")
class RefreshTokensRedisRepositoryTest {
    @Autowired
    private RefreshTokensRedisRepository refreshTokensRedisRepository;

    private UUID userId;
    private RefreshTokens token;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        token = RefreshTokens.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .provider("google")
                .providerId("12345")
                .used(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void testSaveAndFind() {
        refreshTokensRedisRepository.save(token);

        List<RefreshTokens> result = refreshTokensRedisRepository.findByUserIdAndUsedIsFalse(userId);
        assertFalse(result.isEmpty());
        assertEquals(userId, result.get(0).getUserId());
    }
}