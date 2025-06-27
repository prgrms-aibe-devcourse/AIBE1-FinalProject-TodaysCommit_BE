package com.team5.catdogeats.admins.domain.dto;

import com.team5.catdogeats.admins.domain.enums.Department;
import lombok.Builder;

import java.time.ZonedDateTime;

/**
 * 관리자 정보 조회 응답 DTO
 */
@Builder
public record AdminInfoResponseDTO(
        String id,
        String email,
        String name,
        Department department,
        boolean isActive,
        boolean isFirstLogin,
        String verificationCode,
        ZonedDateTime verificationCodeExpiry,
        ZonedDateTime lastLoginAt,
        ZonedDateTime createdAt
) {
    /**
     * 관리자 상태를 문자열로 반환
     */
    public String getStatusString() {
        if (isActive && !isFirstLogin) {
            return "ACTIVE";
        } else if (!isActive && verificationCode != null) {
            return "PENDING";
        } else {
            return "INACTIVE";
        }
    }

    /**
     * 인증코드가 만료되었는지 확인
     */
    public boolean isVerificationCodeExpired() {
        if (verificationCodeExpiry == null) {
            return true;
        }
        return verificationCodeExpiry.isBefore(ZonedDateTime.now());
    }
}
