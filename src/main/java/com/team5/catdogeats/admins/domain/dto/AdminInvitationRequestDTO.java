package com.team5.catdogeats.admins.domain.dto;

import com.team5.catdogeats.admins.domain.enums.Department;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 관리자 초대 요청 DTO
 */
public record AdminInvitationRequestDTO(
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotNull(message = "부서는 필수입니다.")
        Department department
) {
}
