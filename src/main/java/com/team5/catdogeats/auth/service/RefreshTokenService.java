package com.team5.catdogeats.auth.service;

import org.springframework.security.core.Authentication;

public interface RefreshTokenService {
    String createRefreshToken(Authentication authentication);

}
