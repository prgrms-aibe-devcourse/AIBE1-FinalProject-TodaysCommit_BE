package com.team5.catdogeats.admins.domain.dto;

import lombok.Builder;

/**
 * 계정 인증 응답 DTO
 */
@Builder
public record AdminVerificationResponseDTO(
        String email,
        String name,
        boolean isVerified,
        String message,
        String redirectUrl
) {
}
