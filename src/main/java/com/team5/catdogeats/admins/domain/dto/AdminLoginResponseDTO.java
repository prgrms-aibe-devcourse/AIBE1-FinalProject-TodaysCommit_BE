package com.team5.catdogeats.admins.domain.dto;

import com.team5.catdogeats.admins.domain.enums.Department;
import lombok.Builder;

import java.time.ZonedDateTime;

/**
 * 관리자 로그인 응답 DTO
 */
@Builder
public record AdminLoginResponseDTO(
        String email,
        String name,
        Department department,
        boolean isFirstLogin,
        ZonedDateTime lastLoginAt,
        String redirectUrl,
        String message
) {
}

