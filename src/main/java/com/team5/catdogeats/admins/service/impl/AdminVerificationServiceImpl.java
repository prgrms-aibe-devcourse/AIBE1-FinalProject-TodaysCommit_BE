package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminVerificationRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminVerificationResponseDTO;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.AdminVerificationService;
import com.team5.catdogeats.admins.exception.InvalidVerificationCodeException;
import com.team5.catdogeats.admins.exception.VerificationCodeExpiredException;
import com.team5.catdogeats.global.config.JpaTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.admin.base-url}")
    private String baseUrl;

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
                    .redirectUrl(baseUrl + "/v1/admin/login")
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

        // 5. 계정 활성화
        admin.activate();
        adminRepository.save(admin);

        log.info("관리자 계정 활성화 완료: email={}, name={}", admin.getEmail(), admin.getName());

        return AdminVerificationResponseDTO.builder()
                .email(admin.getEmail())
                .name(admin.getName())
                .isVerified(true)
                .message("계정이 성공적으로 활성화되었습니다. 이제 로그인할 수 있습니다.")
                .redirectUrl(baseUrl + "/v1/admin/login")
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

        // 새로운 인증코드 생성 및 설정
        String newCode = generateVerificationCode();
        ZonedDateTime expiry = ZonedDateTime.now().plusHours(1);
        admin.setVerificationCode(newCode, expiry);
        adminRepository.save(admin);

        // 인증코드 재발송 이메일 발송
        sendResendVerificationEmail(admin.getEmail(), admin.getName(), newCode);

        log.info("인증코드 재발송: email={}", email);
        return newCode;
    }

    /**
     * 6자리 인증코드 생성
     */
    private String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    /**
     * 인증코드 재발송 이메일 발송
     */
    private void sendResendVerificationEmail(String email, String name, String verificationCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[고양이강아지잇츠] 인증코드 재발송");
            message.setText(buildResendEmailContent(name, verificationCode));

            mailSender.send(message);
            log.info("인증코드 재발송 이메일 발송 완료: {}", email);

        } catch (Exception e) {
            log.error("인증코드 재발송 이메일 발송 실패: email={}, error={}", email, e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }

    /**
     * 재발송 이메일 내용 구성
     */
    private String buildResendEmailContent(String name, String verificationCode) {
        return String.format("""
            안녕하세요 %s님,
            
            요청하신 인증코드를 재발송합니다.
            
            ===== 새로운 인증코드 =====
            %s
            
            ===== 계정 활성화 방법 =====
            1. 아래 링크로 접속해주세요.
            %s/v1/admin/verify
            
            2. 이메일과 새로운 인증코드를 입력해주세요.
            
            ※ 인증코드는 1시간 동안만 유효합니다.
            ※ 문의사항이 있으시면 개발팀에 연락해주세요.
            
            고양이강아지잇츠 관리팀
            """, name, verificationCode, baseUrl);
    }
}