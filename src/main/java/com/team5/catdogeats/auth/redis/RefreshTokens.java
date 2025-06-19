package com.team5.catdogeats.auth.redis;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "refreshToken", timeToLive = 60 * 60 * 24) // 1일 TTL

//엔티티가 아니기 때문에 @Entity 어노테이션 생략
public class RefreshTokens implements Serializable {

    @Id
    private UUID id;

    private String provider;
    private String providerId;

    @Indexed
    private UUID userId;

    @Indexed
    private boolean used;
    private Instant expiresAt;
    private Instant createdAt; // 토큰 생성 시간 (정렬용)
}