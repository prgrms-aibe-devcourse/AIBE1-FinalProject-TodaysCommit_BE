package com.team5.catdogeats.admins.domain.dto;

import com.team5.catdogeats.admins.domain.enums.Department;
import lombok.Builder;

import java.time.ZonedDateTime;

/**
 * 관리자 정보 조회 응답 DTO
 */
@Builder
public record AdminInfoResponseDTO(
        String email,
        String name,
        Department department,
        boolean isFirstLogin,
        ZonedDateTime lastLoginAt,
        ZonedDateTime createdAt
) {
}
