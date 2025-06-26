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
    public void cacheUserIdAndRole(String provider, String providerId) {
        Users user = getUser(provider, providerId);          // ↓ userId·role 둘 다 갖고 옴
        String baseKey = MakeKeyString.makeKeyProviderAndProviderId(provider, providerId);

        // 1) userId
        redisTemplate.opsForValue()
                .set(baseKey + ":userId", user.getId(), EXPIRATION);

        // 2) role
        redisTemplate.opsForValue()
                .set(user.getId() , user.getRole().name(), EXPIRATION);

        log.debug("Cached userId={} & role={} under baseKey={}",
                user.getId(), user.getRole(), baseKey);
    }

    @Override
    public String getCachedRoleByUserId(String userId) {

        return redisTemplate.opsForValue().get(userId);
    }



    @Override
    public String getCachedUserId(String provider, String providerId) {
        String key = MakeKeyString.makeKeyProviderAndProviderId(provider, providerId) + ":userId";
        return redisTemplate.opsForValue().get(key);
    }

    private Users getUser(String provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new NoSuchElementException("유저 정보가 없습니다."));
    }


}
