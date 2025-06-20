package com.team5.catdogeats.auth.service;

import com.team5.catdogeats.auth.dto.RotateTokenDTO;

import java.util.UUID;

public interface RotateRefreshTokenService {
    RotateTokenDTO RotateRefreshToken(UUID refreshTokenId);
}
