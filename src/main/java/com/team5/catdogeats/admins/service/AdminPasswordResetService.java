package com.team5.catdogeats.admins.service;

import com.team5.catdogeats.admins.domain.dto.AdminPasswordResetRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminPasswordResetResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminPasswordResetVerificationDTO;
import com.team5.catdogeats.admins.domain.dto.AdminVerificationResponseDTO;

/**
 * 관리자 비밀번호 초기화 서비스 인터페이스
 */
public interface AdminPasswordResetService {

    /**
     * 관리자 비밀번호 초기화 요청
     *
     * @param request 초기화 요청 정보
     * @return 초기화 응답 정보
     */
    AdminPasswordResetResponseDTO requestPasswordReset(AdminPasswordResetRequestDTO request);

    /**
     * 비밀번호 초기화 인증 및 새 비밀번호 설정
     *
     * @param request 인증 및 새 비밀번호 정보
     * @return 인증 응답 정보
     */
    AdminVerificationResponseDTO verifyAndResetPassword(AdminPasswordResetVerificationDTO request);
}
