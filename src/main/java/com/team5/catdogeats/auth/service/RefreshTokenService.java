package com.team5.catdogeats.auth.service;

import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface RefreshTokenService {
    UUID createRefreshToken(Authentication authentication);

}
