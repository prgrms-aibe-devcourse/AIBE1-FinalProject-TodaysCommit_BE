package com.team5.catdogeats.admins.domain.dto;

import lombok.Builder;

import java.time.ZonedDateTime;

/**
 * 관리자 비밀번호 초기화 응답 DTO
 */
@Builder
public record AdminPasswordResetResponseDTO(
        String email,
        String name,
        ZonedDateTime verificationCodeExpiry,
        String message
) {
}
