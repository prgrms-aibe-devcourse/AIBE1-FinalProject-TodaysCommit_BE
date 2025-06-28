package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminPasswordResetVerificationDTO;
import com.team5.catdogeats.admins.domain.dto.AdminVerificationRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminVerificationResponseDTO;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.AdminVerificationService;
import com.team5.catdogeats.admins.exception.InvalidVerificationCodeException;
import com.team5.catdogeats.admins.exception.VerificationCodeExpiredException;
import com.team5.catdogeats.admins.util.AdminUtils;
import com.team5.catdogeats.global.config.JpaTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.ZonedDateTime;

/**
 * 관리자 계정 인증 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVerificationServiceImpl implements AdminVerificationService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminUtils adminUtils;

    @Override
    @JpaTransactional
    public AdminVerificationResponseDTO verifyAdmin(AdminVerificationRequestDTO request) {
        // 1. 이메일로 관리자 조회
        Admins admin = findAdminByEmail(request.email());

        // 2. 이미 활성화된 계정인지 확인
        if (admin.getIsActive()) {
            return buildAlreadyActiveResponse(admin);
        }

        // 3. 인증코드 검증
        validateVerificationCode(admin, request.verificationCode());

        // 4. 계정 활성화 및 초기 비밀번호 설정
        String initialPassword = activateAdminAccount(admin);

        log.info("관리자 계정 활성화 완료: email={}, name={}", admin.getEmail(), admin.getName());

        return AdminVerificationResponseDTO.builder()
                .email(admin.getEmail())
                .name(admin.getName())
                .isVerified(true)
                .message("계정이 성공적으로 활성화되었습니다. 이제 로그인할 수 있습니다.")
                .redirectUrl(adminUtils.getLoginRedirectUrl())
                .initialPassword(initialPassword)
                .build();
    }

    @Override
    @JpaTransactional
    public String resendVerificationCode(String email) {
        Admins admin = findAdminByEmail(email);

        if (admin.getIsActive()) {
            throw new IllegalStateException("이미 활성화된 계정입니다.");
        }

        // 새로운 인증코드 생성 및 설정
        String newCode = adminUtils.generateVerificationCode();
        ZonedDateTime expiry = adminUtils.calculateExpiryTime();
        admin.setVerificationCode(newCode, expiry);
        adminRepository.save(admin);

        // 재발송 이메일 발송
        adminUtils.sendResendVerificationEmail(admin.getEmail(), admin.getName(), newCode);

        log.info("인증코드 재발송: email={}", email);
        return newCode;
    }

    @Override
    @JpaTransactional
    public AdminVerificationResponseDTO verifyAndResetPassword(AdminPasswordResetVerificationDTO request) {
        // 1. 관리자 조회 및 인증코드 검증
        Admins admin = findAdminByEmail(request.email());
        validateVerificationCode(admin, request.verificationCode());

        // 2. 새 비밀번호 검증
        validatePasswordMatch(request.newPassword(), request.confirmPassword());

        // 3. 비밀번호 변경 및 계정 활성화
        resetAdminPassword(admin, request.newPassword());

        log.info("비밀번호 재설정 완료: email={}", admin.getEmail());

        return AdminVerificationResponseDTO.builder()
                .email(admin.getEmail())
                .name(admin.getName())
                .isVerified(true)
                .message("비밀번호가 성공적으로 재설정되었습니다. 새 비밀번호로 로그인해주세요.")
                .redirectUrl(adminUtils.getLoginRedirectUrl())
                .initialPassword(null)
                .build();
    }

    // ===== 헬퍼 메서드들 =====

    /**
     * 이메일로 관리자 조회
     */
    private Admins findAdminByEmail(String email) {
        return adminRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidVerificationCodeException("존재하지 않는 이메일입니다."));
    }

    /**
     * 인증코드 검증
     */
    private void validateVerificationCode(Admins admin, String verificationCode) {
        if (admin.getVerificationCode() == null ||
                !admin.getVerificationCode().equals(verificationCode)) {
            throw new InvalidVerificationCodeException("잘못된 인증코드입니다.");
        }

        if (admin.getVerificationCodeExpiry() == null ||
                admin.getVerificationCodeExpiry().isBefore(ZonedDateTime.now())) {
            throw new VerificationCodeExpiredException("인증코드가 만료되었습니다. 새로운 인증코드를 요청해주세요.");
        }
    }

    /**
     * 이미 활성화된 계정에 대한 응답 생성
     */
    private AdminVerificationResponseDTO buildAlreadyActiveResponse(Admins admin) {
        return AdminVerificationResponseDTO.builder()
                .email(admin.getEmail())
                .name(admin.getName())
                .isVerified(true)
                .message("이미 활성화된 계정입니다.")
                .redirectUrl(adminUtils.getLoginRedirectUrl())
                .initialPassword(null)
                .build();
    }

    /**
     * 관리자 계정 활성화 및 초기 비밀번호 설정
     */
    private String activateAdminAccount(Admins admin) {
        // 초기 비밀번호 생성
        String initialPassword = adminUtils.generateInitialPassword();

        // 계정 활성화 및 초기 비밀번호 설정
        admin.activate();
        admin.setPassword(passwordEncoder.encode(initialPassword));
        admin.setIsFirstLogin(true);
        adminRepository.save(admin);

        return initialPassword;
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
     * 관리자 비밀번호 재설정
     */
    private void resetAdminPassword(Admins admin, String newPassword) {
        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setIsActive(true);
        admin.setIsFirstLogin(false);
        admin.setVerificationCode(null, null);
        adminRepository.save(admin);
    }
}