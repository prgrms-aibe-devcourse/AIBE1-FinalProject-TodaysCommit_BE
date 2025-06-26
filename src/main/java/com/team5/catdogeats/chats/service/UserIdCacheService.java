package com.team5.catdogeats.chats.service;

public interface UserIdCacheService {
    void cacheUserId(String provider, String providerId);
    String getCachedUserId(String provider, String providerId);
}
