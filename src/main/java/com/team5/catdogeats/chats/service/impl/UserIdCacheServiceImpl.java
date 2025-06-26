package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.chats.util.MakeKeyString;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserIdCacheServiceImpl implements UserIdCacheService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration EXPIRATION = Duration.ofMinutes(30);
    private final UserRepository userRepository;

    @Override
    public void cacheUserId(String provider, String providerId) {
        String key = MakeKeyString.makeKeyProviderAndProviderId(provider, providerId);
        String userId = getUserId(provider, providerId);
        redisTemplate.opsForValue().setIfAbsent(key, userId,  EXPIRATION);
        log.debug("Cached userId={} under key={}", userId, key);

    }

    @Override
    public String getCachedUserId(String provider, String providerId) {
        String key = MakeKeyString.makeKeyProviderAndProviderId(provider, providerId);
        return redisTemplate.opsForValue().get(key);
    }


    private String getUserId(String provider, String providerId) {
        Users users = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new NoSuchElementException("유저 정보가 없습니다."));
        return users.getId();
    }

}
