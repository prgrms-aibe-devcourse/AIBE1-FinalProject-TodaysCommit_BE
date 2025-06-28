package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.*;
import com.team5.catdogeats.admins.exception.InvalidVerificationCodeException;
import com.team5.catdogeats.admins.exception.VerificationCodeExpiredException;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.AdminPasswordResetService;
import com.team5.catdogeats.admins.util.AdminUtils;
import com.team5.catdogeats.global.config.JpaTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

/**
 * 관리자 비밀번호 초기화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPasswordResetServiceImpl implements AdminPasswordResetService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminUtils adminUtils;

    private static final String SUPER_ADMIN_EMAIL = "super@catdogeats.com";

    @Override
    @JpaTransactional
    public AdminPasswordResetResponseDTO requestPasswordReset(AdminPasswordResetRequestDTO request) {
        // 1. 대상 관리자 조회 및 검증
        Admins targetAdmin = findAndValidateTargetAdmin(request);

        // 2. 인증코드 생성 및 계정 비활성화
        String verificationCode = adminUtils.generateVerificationCode();
        ZonedDateTime expiry = adminUtils.calculateExpiryTime();

        resetAdminAccount(targetAdmin, verificationCode, expiry);

        // 3. 비밀번호 초기화 이메일 발송
        adminUtils.sendPasswordResetEmail(
                targetAdmin.getEmail(),
                targetAdmin.getName(),
                verificationCode,
                request.requestedBy()
        );

        log.info("비밀번호 초기화 요청 완료: target={}, requestedBy={}",
                targetAdmin.getEmail(), request.requestedBy());

        return AdminPasswordResetResponseDTO.builder()
                .email(targetAdmin.getEmail())
                .name(targetAdmin.getName())
                .verificationCodeExpiry(expiry)
                .message("비밀번호 초기화 이메일이 발송되었습니다.")
                .build();
    }

    @Override
    @JpaTransactional
    public AdminVerificationResponseDTO verifyAndResetPassword(AdminPasswordResetVerificationDTO request) {
        // 1. 관리자 조회 및 인증코드 검증
        Admins admin = findAndValidateAdmin(request.email(), request.verificationCode());

        // 2. 새 비밀번호 검증
        validatePasswordMatch(request.newPassword(), request.confirmPassword());

        // 3. 비밀번호 변경 및 계정 활성화
        resetPassword(admin, request.newPassword());

        log.info("비밀번호 초기화 완료: email={}", admin.getEmail());

        return AdminVerificationResponseDTO.builder()
                .email(admin.getEmail())
                .name(admin.getName())
                .isVerified(true)
                .message("비밀번호가 성공적으로 재설정되었습니다. 새 비밀번호로 로그인해주세요.")
                .redirectUrl(adminUtils.getLoginRedirectUrl())
                .initialPassword(null) // 보안상 새 비밀번호는 반환하지 않음
                .build();
    }

    /**
     * 대상 관리자 조회 및 유효성 검증
     */
    private Admins findAndValidateTargetAdmin(AdminPasswordResetRequestDTO request) {
        Admins targetAdmin = adminRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        // 슈퍼관리자 계정은 초기화 불가
        if (SUPER_ADMIN_EMAIL.equals(targetAdmin.getEmail())) {
            throw new IllegalArgumentException("ADMIN 관리자 계정은 비밀번호를 초기화할 수 없습니다.");
        }

        // 자기 자신 초기화 방지
        if (request.email().equals(request.requestedBy())) {
            throw new IllegalArgumentException("자신의 비밀번호는 초기화할 수 없습니다. 비밀번호 변경 기능을 이용하세요.");
        }

        return targetAdmin;
    }

    /**
     * 관리자 계정 초기화 (비활성화 및 인증코드 설정)
     */
    private void resetAdminAccount(Admins admin, String verificationCode, ZonedDateTime expiry) {
        admin.setIsActive(false);
        admin.setIsFirstLogin(true);
        admin.setVerificationCode(verificationCode, expiry);
        adminRepository.save(admin);
    }

    /**
     * 관리자 조회 및 인증코드 검증
     */
    private Admins findAndValidateAdmin(String email, String verificationCode) {
        Admins admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidVerificationCodeException("존재하지 않는 이메일입니다."));

        // 인증코드 검증
        if (admin.getVerificationCode() == null ||
                !admin.getVerificationCode().equals(verificationCode)) {
            throw new InvalidVerificationCodeException("잘못된 인증코드입니다.");
        }

        // 인증코드 만료 확인
        if (admin.getVerificationCodeExpiry() == null ||
                admin.getVerificationCodeExpiry().isBefore(ZonedDateTime.now())) {
            throw new VerificationCodeExpiredException("인증코드가 만료되었습니다. 관리자에게 새로운 초기화를 요청하세요.");
        }

        return admin;
    }

    /**
     * 비밀번호 일치 검증
     */
    private void validatePasswordMatch(String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }
    }

    /**
     * 비밀번호 재설정 및 계정 활성화
     */
    private void resetPassword(Admins admin, String newPassword) {
        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setIsActive(true);
        admin.setIsFirstLogin(false); // 비밀번호 재설정 완료로 간주
        admin.setVerificationCode(null, null); // 인증코드 제거
        adminRepository.save(admin);
    }
}