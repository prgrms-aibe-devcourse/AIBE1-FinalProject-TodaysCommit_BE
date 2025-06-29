package com.team5.catdogeats.admins.domain.dto;

import com.team5.catdogeats.admins.domain.enums.Department;
import lombok.Builder;

import java.time.ZonedDateTime;

/**
 * 관리자 초대 응답 DTO
 */
@Builder
public record AdminInvitationResponseDTO(
        String email,
        String name,
        Department department,
        ZonedDateTime verificationCodeExpiry,
        String message
) {
}
