package com.team5.catdogeats.auth.repository;

import com.team5.catdogeats.auth.redis.RefreshTokens;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokensRedisRepository extends CrudRepository<RefreshTokens, UUID> {
    List<RefreshTokens> findByUserIdAndUsedIsFalse(String userId);
    void deleteByUserId(String userId);

    void deleteById(String id);
    Optional<RefreshTokens> findById(String id);
}
