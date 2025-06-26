package com.team5.catdogeats.admins.service;

import com.team5.catdogeats.admins.domain.dto.AdminVerificationRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminVerificationResponseDTO;

/**
 * 관리자 계정 인증 서비스 인터페이스
 */
public interface AdminVerificationService {
    /**
     * 관리자 계정 인증
     */
    AdminVerificationResponseDTO verifyAdmin(AdminVerificationRequestDTO request);

    /**
     * 인증코드 재발송
     */
    String resendVerificationCode(String email);
}