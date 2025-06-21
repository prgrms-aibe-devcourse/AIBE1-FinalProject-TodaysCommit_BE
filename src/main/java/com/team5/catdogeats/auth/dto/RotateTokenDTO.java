package com.team5.catdogeats.auth.dto;

public record RotateTokenDTO(String newAccessToken,
                             String newRefreshToken,
                             String tokenType,
                             long expiration) {
}
