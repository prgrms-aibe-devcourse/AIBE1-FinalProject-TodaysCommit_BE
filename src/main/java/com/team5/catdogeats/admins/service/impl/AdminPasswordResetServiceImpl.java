package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.*;
import com.team5.catdogeats.admins.exception.InvalidVerificationCodeException;
import com.team5.catdogeats.admins.exception.VerificationCodeExpiredException;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.AdminPasswordResetService;
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
 * 관리자 비밀번호 초기화 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPasswordResetServiceImpl implements AdminPasswordResetService {

    private final AdminRepository adminRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${admin.invitation.expiration-hours:1}")
    private int expirationHours;

    @Value("${app.admin.base-url}")
    private String baseUrl;

    @Override
    @JpaTransactional
    public AdminPasswordResetResponseDTO requestPasswordReset(AdminPasswordResetRequestDTO request) {
        // 1. 대상 관리자 조회
        Admins targetAdmin = adminRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        // 2. 슈퍼관리자 계정은 초기화 불가
        if ("super@catdogeats.com".equals(targetAdmin.getEmail())) {
            throw new IllegalArgumentException("ADMIN 관리자 계정은 비밀번호를 초기화할 수 없습니다.");
        }

        // 3. 요청자와 대상자가 동일한지 확인 (자기 자신 초기화 방지)
        if (request.email().equals(request.requestedBy())) {
            throw new IllegalArgumentException("자신의 비밀번호는 초기화할 수 없습니다. 비밀번호 변경 기능을 이용하세요.");
        }

        // 4. 6자리 인증코드 생성
        String verificationCode = generateVerificationCode();
        ZonedDateTime expiry = ZonedDateTime.now().plusHours(expirationHours);

        // 5. 계정 비활성화 및 인증코드 설정
        targetAdmin.setIsActive(false);
        targetAdmin.setIsFirstLogin(true);
        targetAdmin.setVerificationCode(verificationCode, expiry);
        adminRepository.save(targetAdmin);

        // 6. 비밀번호 초기화 이메일 발송
        sendPasswordResetEmail(targetAdmin.getEmail(), targetAdmin.getName(), verificationCode, request.requestedBy());

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
        admin.setIsFirstLogin(false); // 비밀번호 재설정 완료로 간주
        admin.setVerificationCode(null, null); // 인증코드 제거
        adminRepository.save(admin);

        log.info("비밀번호 초기화 완료: email={}", admin.getEmail());

        return AdminVerificationResponseDTO.builder()
                .email(admin.getEmail())
                .name(admin.getName())
                .isVerified(true)
                .message("비밀번호가 성공적으로 재설정되었습니다. 새 비밀번호로 로그인해주세요.")
                .redirectUrl(baseUrl + "/v1/admin/login")
                .initialPassword(null) // 보안상 새 비밀번호는 반환하지 않음
                .build();
    }

    /**
     * 6자리 인증코드 생성
     */
    private String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    /**
     * 비밀번호 초기화 이메일 발송
     */
    private void sendPasswordResetEmail(String email, String name, String verificationCode, String requestedBy) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[CatDogEats] 관리자 계정 비밀번호 초기화 안내");
            message.setText(buildPasswordResetEmailContent(name, verificationCode, requestedBy));

            mailSender.send(message);
            log.info("비밀번호 초기화 이메일 발송 완료: {}", email);

        } catch (Exception e) {
            log.error("비밀번호 초기화 이메일 발송 실패: email={}, error={}", email, e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }

    /**
     * 비밀번호 초기화 이메일 내용 구성
     */
    private String buildPasswordResetEmailContent(String name, String verificationCode, String requestedBy) {
        return String.format("""
            안녕하세요 %s님,
            
            관리자(%s)의 요청으로 귀하의 CatDogEats 관리자 계정 비밀번호가 초기화되었습니다.
            
            ===== 비밀번호 재설정 안내 =====
            인증코드: %s
            
            ===== 비밀번호 재설정 방법 =====
            1. 아래 링크로 접속해주세요.
            %s/v1/admin/verify
            
            2. 이메일과 인증코드를 입력해주세요.
            
            3. 새로운 비밀번호를 설정해주세요.
            
            ※ 인증코드는 1시간 동안만 유효합니다.
            ※ 계정이 임시로 비활성화되었으며, 재설정 완료 후 다시 사용할 수 있습니다.
            ※ 문의사항이 있으시면 시스템 관리자에게 연락해주세요.
            
            CatDogEats 관리팀
            """, name, requestedBy, verificationCode, baseUrl);
    }
}