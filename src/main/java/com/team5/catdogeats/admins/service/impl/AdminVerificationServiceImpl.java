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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
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
        Admins admin = adminRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidVerificationCodeException("존재하지 않는 이메일입니다."));

        // 2. 이미 활성화된 계정인지 확인
        if (admin.getIsActive()) {
            return AdminVerificationResponseDTO.builder()
                    .email(admin.getEmail())
                    .name(admin.getName())
                    .isVerified(true)
                    .message("이미 활성화된 계정입니다.")
                    .redirectUrl(adminUtils.getBaseUrl() + "/v1/admin/login")
                    .initialPassword(null)
                    .build();
        }

        // 3. 인증코드 검증
        if (admin.getVerificationCode() == null ||
                !admin.getVerificationCode().equals(request.verificationCode())) {
            throw new InvalidVerificationCodeException("잘못된 인증코드입니다.");
        }

        // 4. 인증코드 만료 확인
        if (admin.getVerificationCodeExpiry() == null ||
                admin.getVerificationCodeExpiry().isBefore(ZonedDateTime.now())) {
            throw new VerificationCodeExpiredException("인증코드가 만료되었습니다. 새로운 인증코드를 요청해주세요.");
        }

        // 5. 초기 비밀번호 생성 (✨ 유틸리티 사용)
        String initialPassword = adminUtils.generateInitialPassword();

        // 6. 계정 활성화 및 초기 비밀번호 설정
        admin.activate();
        admin.setPassword(passwordEncoder.encode(initialPassword));
        admin.setIsFirstLogin(true);
        adminRepository.save(admin);

        log.info("관리자 계정 활성화 완료: email={}, name={}", admin.getEmail(), admin.getName());

        return AdminVerificationResponseDTO.builder()
                .email(admin.getEmail())
                .name(admin.getName())
                .isVerified(true)
                .message("계정이 성공적으로 활성화되었습니다. 이제 로그인할 수 있습니다.")
                .redirectUrl(adminUtils.getBaseUrl() + "/v1/admin/login")
                .initialPassword(initialPassword)
                .build();
    }

    @Override
    @JpaTransactional
    public String resendVerificationCode(String email) {
        Admins admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidVerificationCodeException("존재하지 않는 이메일입니다."));

        if (admin.getIsActive()) {
            throw new IllegalStateException("이미 활성화된 계정입니다.");
        }

        // 새로운 인증코드 생성
        String newCode = adminUtils.generateVerificationCode();
        ZonedDateTime expiry = ZonedDateTime.now().plusHours(1);
        admin.setVerificationCode(newCode, expiry);
        adminRepository.save(admin);

        //  이메일 발송
        String subject = "[CatDogEats] 인증코드 재발송";
        String content = adminUtils.buildResendEmailContent(admin.getName(), newCode);
        adminUtils.sendEmail(admin.getEmail(), subject, content);

        log.info("인증코드 재발송: email={}", email);
        return newCode;
    }

    @Override
    @JpaTransactional
    public AdminVerificationResponseDTO verifyAndResetPassword(AdminPasswordResetVerificationDTO request) {
        // 1. 이메일로 관리자 조회
        Admins admin = adminRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidVerificationCodeException("존재하지 않는 이메일입니다."));

        // 2. 인증코드 검증
        if (admin.getVerificationCode() == null ||
                !admin.getVerificationCode().equals(request.verificationCode())) {
            throw new InvalidVerificationCodeException("잘못된 인증코드입니다.");
        }

        // 3. 인증코드 만료 확인
        if (admin.getVerificationCodeExpiry() == null ||
                admin.getVerificationCodeExpiry().isBefore(ZonedDateTime.now())) {
            throw new VerificationCodeExpiredException("인증코드가 만료되었습니다. 관리자에게 새로운 초기화를 요청하세요.");
        }

        // 4. 새 비밀번호와 확인 비밀번호 일치 검증
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        // 5. 비밀번호 변경 및 계정 활성화
        admin.setPassword(passwordEncoder.encode(request.newPassword()));
        admin.setIsActive(true);
        admin.setIsFirstLogin(false);
        admin.setVerificationCode(null, null);
        adminRepository.save(admin);

        log.info("비밀번호 재설정 완료: email={}", admin.getEmail());

        return AdminVerificationResponseDTO.builder()
                .email(admin.getEmail())
                .name(admin.getName())
                .isVerified(true)
                .message("비밀번호가 성공적으로 재설정되었습니다. 새 비밀번호로 로그인해주세요.")
                .redirectUrl(adminUtils.getBaseUrl() + "/v1/admin/login")
                .initialPassword(null)
                .build();
    }
}




