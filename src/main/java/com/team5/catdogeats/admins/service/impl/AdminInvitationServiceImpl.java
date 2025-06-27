package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.enums.AdminRole;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.domain.dto.AdminInvitationRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminInvitationResponseDTO;
import com.team5.catdogeats.admins.exception.DuplicateEmailException;
import com.team5.catdogeats.admins.exception.InvalidDepartmentException;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.AdminInvitationService;
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
import java.util.List;

/**
 * 관리자 초대 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInvitationServiceImpl implements AdminInvitationService {

    private final AdminRepository adminRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    // 허용된 부서 목록 (ADMIN 제외)
    private static final List<Department> ALLOWED_DEPARTMENTS = List.of(
            Department.DEVELOPMENT,
            Department.CUSTOMER_SERVICE,
            Department.OPERATIONS
    );

    @Value("${admin.invitation.expiration-hours:1}")
    private int expirationHours;

    @Value("${app.admin.base-url}")
    private String baseUrl;

    @Override
    @JpaTransactional
    public AdminInvitationResponseDTO inviteAdmin(AdminInvitationRequestDTO request) {
        // 1. 부서 검증 - ADMIN 부서는 등록 불가
        if (request.department() == Department.ADMIN) {
            throw new InvalidDepartmentException("ADMIN 부서는 직접 등록할 수 없습니다. 시스템에서 자동으로 생성됩니다.");
        }

        // 2. 허용된 부서인지 검증
        if (!ALLOWED_DEPARTMENTS.contains(request.department())) {
            throw new InvalidDepartmentException("허용되지 않은 부서입니다. 사용 가능한 부서: " + ALLOWED_DEPARTMENTS);
        }

        // 3. 이메일 중복 검증
        if (adminRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("이미 등록된 이메일입니다: " + request.email());
        }

        // 4. 6자리 인증코드 생성
        String verificationCode = generateVerificationCode();
        ZonedDateTime expiry = ZonedDateTime.now().plusHours(expirationHours);

        // 5. 초기 비밀번호 생성 (임시)
        String initialPassword = generateInitialPassword();

        // 6. 관리자 계정 생성 (비활성화 상태)
        Admins admin = Admins.builder()
                .email(request.email())
                .name(request.name())
                .department(request.department())
                .adminRole(AdminRole.ROLE_ADMIN)
                .password(passwordEncoder.encode(initialPassword))
                .isActive(false)
                .isFirstLogin(true)
                .build();

        admin.setVerificationCode(verificationCode, expiry);
        adminRepository.save(admin);

        // 7. 인증 이메일 발송
        sendVerificationEmail(request.email(), request.name(), verificationCode, initialPassword, request.department());

        log.info("관리자 초대 완료: email={}, name={}, department={}", request.email(), request.name(), request.department());

        return AdminInvitationResponseDTO.builder()
                .email(request.email())
                .name(request.name())
                .department(request.department())
                .verificationCodeExpiry(expiry)
                .message("인증 이메일이 발송되었습니다.")
                .build();
    }

    /**
     * 6자리 인증코드 생성
     */
    private String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    /**
     * 초기 비밀번호 생성 (8자리 영문+숫자)
     */
    private String generateInitialPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return password.toString();
    }

    /**
     * 인증 이메일 발송
     */
    private void sendVerificationEmail(String email, String name, String verificationCode, String initialPassword, Department department) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[CatDogEats] 관리자 계정 인증 안내");
            message.setText(buildEmailContent(name, verificationCode, initialPassword, department));

            mailSender.send(message);
            log.info("인증 이메일 발송 완료: {}", email);

        } catch (Exception e) {
            log.error("인증 이메일 발송 실패: email={}, error={}", email, e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }

    /**
     * 이메일 내용 구성
     */
    private String buildEmailContent(String name, String verificationCode, String initialPassword, Department department) {
        String departmentName = getDepartmentDisplayName(department);

        return String.format("""
            안녕하세요 %s님,
            
            CatDogEats %s 관리자 계정이 생성되었습니다.
            
            ===== 계정 정보 =====
            부서: %s
            인증코드: %s
            초기 비밀번호: %s
            
            ===== 계정 활성화 방법 =====
            1. 아래 링크로 접속해주세요.
            %s/v1/admin/verify
            
            2. 이메일과 인증코드를 입력해주세요.
            
            3. 활성화 완료 후 로그인하여 비밀번호를 변경해주세요.
            
            ※ 인증코드는 1시간 동안만 유효합니다.
            ※ 문의사항이 있으시면 시스템 관리자에게 연락해주세요.
            
            고양이강아지잇츠 관리팀
            """, name, departmentName, departmentName, verificationCode, initialPassword, baseUrl);
    }

    /**
     * 부서 표시명 반환
     */
    private String getDepartmentDisplayName(Department department) {
        return switch (department) {
            case DEVELOPMENT -> "개발팀";
            case CUSTOMER_SERVICE -> "고객서비스팀";
            case OPERATIONS -> "운영팀";
            case ADMIN -> "관리팀";
        };
    }
}