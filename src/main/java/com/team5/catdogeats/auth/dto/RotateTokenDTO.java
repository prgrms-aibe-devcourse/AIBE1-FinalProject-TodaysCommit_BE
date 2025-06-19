package com.team5.catdogeats.auth.dto;

import java.util.UUID;

public record RotateTokenDTO(String newAccessToken,
                             UUID newRefreshToken,
                             String tokenType,
                             long expiration) {
}
