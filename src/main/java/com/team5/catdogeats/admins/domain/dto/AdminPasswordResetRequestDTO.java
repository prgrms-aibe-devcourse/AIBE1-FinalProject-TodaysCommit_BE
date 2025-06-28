package com.team5.catdogeats.admins.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 비밀번호 초기화 요청 DTO
 */
public record AdminPasswordResetRequestDTO(
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @NotBlank(message = "요청자 이메일은 필수입니다.")
        String requestedBy
) {
}