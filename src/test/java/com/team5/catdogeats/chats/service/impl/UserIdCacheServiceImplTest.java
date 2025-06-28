package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

// 4. UserIdCacheServiceImpl 테스트 예시
@ExtendWith(MockitoExtension.class)
class UserIdCacheServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserIdCacheServiceImpl userIdCacheService;


    @Test
    @DisplayName("사용자 ID와 Role 캐싱 성공")
    void cacheUserIdAndRole_Success() {
        // Given
        String provider = "google";
        String providerId = "12345";
        String userId = "user123";
        Role role = Role.ROLE_BUYER;

        Users user = Users.builder()
                .id(userId)
                .provider(provider)
                .providerId(providerId)
                .role(role)
                .build();

        when(userRepository.findByProviderAndProviderId(provider, providerId))
                .thenReturn(Optional.of(user));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations); // ✅ 여기에만!

        // When
        userIdCacheService.cacheUserIdAndRole(provider, providerId);

        // Then
        verify(valueOperations).set(
                eq("google:12345:userId"),
                eq(userId),
                any(Duration.class));
        verify(valueOperations).set(
                eq(userId),
                eq(role.name()),
                any(Duration.class));
    }

    @Test
    @DisplayName("캐시된 사용자 ID 조회 성공")
    void getCachedUserId_Success() {
        // Given
        String provider = "google";
        String providerId = "12345";
        String expectedUserId = "user123";

        when(valueOperations.get("google:12345:userId"))
                .thenReturn(expectedUserId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations); // ✅ 여기에만!

        // When
        String result = userIdCacheService.getCachedUserId(provider, providerId);

        // Then
        assertThat(result).isEqualTo(expectedUserId);
    }

    @Test
    @DisplayName("캐시된 Role 조회 성공")
    void getCachedRoleByUserId_Success() {
        // Given
        String userId = "user123";
        String expectedRole = "ROLE_BUYER";

        when(valueOperations.get(userId)).thenReturn(expectedRole);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations); // ✅ 여기에만!

        // When
        String result = userIdCacheService.getCachedRoleByUserId(userId);

        // Then
        assertThat(result).isEqualTo(expectedRole);
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 캐싱 시도 시 예외 발생")
    void cacheUserIdAndRole_ThrowsException_WhenUserNotFound() {
        // Given
        String provider = "google";
        String providerId = "nonexistent";

        when(userRepository.findByProviderAndProviderId(provider, providerId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userIdCacheService.cacheUserIdAndRole(provider, providerId))
                .isInstanceOf(NoSuchElementException.class);
    }
}
