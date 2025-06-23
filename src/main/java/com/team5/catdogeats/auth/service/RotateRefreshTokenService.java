package com.team5.catdogeats.auth.service;

import com.team5.catdogeats.auth.dto.RotateTokenDTO;

public interface RotateRefreshTokenService {
    RotateTokenDTO RotateRefreshToken(String refreshTokenId);
}
