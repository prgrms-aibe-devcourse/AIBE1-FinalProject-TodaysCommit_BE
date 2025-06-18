package com.team5.catdogeats.auth.service;

import org.springframework.security.core.Authentication;

public interface JwtService {
    String generateAccessToken(Authentication authentication);
    Authentication getAuthentication(String subject);
}
