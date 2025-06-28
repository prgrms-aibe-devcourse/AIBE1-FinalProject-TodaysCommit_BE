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
import com.team5.catdogeats.admins.util.AdminUtils;
import com.team5.catdogeats.global.config.JpaTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    private final PasswordEncoder passwordEncoder;
    private final AdminUtils adminUtils;

    // 허용된 부서 목록 (ADMIN 제외)
    private static final List<Department> ALLOWED_DEPARTMENTS = List.of(
            Department.DEVELOPMENT,
            Department.CUSTOMER_SERVICE,
            Department.OPERATIONS
    );

    @Override
    @JpaTransactional
    public AdminInvitationResponseDTO inviteAdmin(AdminInvitationRequestDTO request) {
        // 1. 부서 검증
        validateDepartment(request.department());

        // 2. 이메일 중복 검증
        validateEmailNotExists(request.email());

        // 3. 인증코드 및 만료시간 생성
        String verificationCode = adminUtils.generateVerificationCode();
        ZonedDateTime expiry = adminUtils.calculateExpiryTime();

        // 4. 관리자 계정 생성
        Admins admin = createPendingAdmin(request, verificationCode, expiry);

        // 5. 인증 이메일 발송
        adminUtils.sendInvitationEmail(
                request.email(),
                request.name(),
                verificationCode,
                request.department()
        );

        log.info("관리자 초대 완료: email={}, name={}, department={}",
                request.email(), request.name(), request.department());

        return AdminInvitationResponseDTO.builder()
                .email(request.email())
                .name(request.name())
                .department(request.department())
                .verificationCodeExpiry(expiry)
                .message("인증 이메일이 발송되었습니다.")
                .build();
    }

    /**
     * 부서 유효성 검증
     */
    private void validateDepartment(Department department) {
        if (department == Department.ADMIN) {
            throw new InvalidDepartmentException("ADMIN 부서는 직접 등록할 수 없습니다. 시스템에서 자동으로 생성됩니다.");
        }

        if (!ALLOWED_DEPARTMENTS.contains(department)) {
            throw new InvalidDepartmentException("허용되지 않은 부서입니다. 사용 가능한 부서: " + ALLOWED_DEPARTMENTS);
        }
    }

    /**
     * 이메일 중복 검증
     */
    private void validateEmailNotExists(String email) {
        if (adminRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("이미 등록된 이메일입니다: " + email);
        }
    }

    /**
     * 대기중인 관리자 계정 생성
     */
    private Admins createPendingAdmin(AdminInvitationRequestDTO request, String verificationCode, ZonedDateTime expiry) {
        // 임시 비밀번호 생성
        String tempPassword = adminUtils.generateInitialPassword();

        Admins admin = Admins.builder()
                .email(request.email())
                .name(request.name())
                .department(request.department())
                .adminRole(AdminRole.ROLE_ADMIN)
                .password(passwordEncoder.encode(tempPassword)) // 임시 비밀번호 (인증 시 재생성됨)
                .isActive(false)
                .isFirstLogin(true)
                .build();

        admin.setVerificationCode(verificationCode, expiry);
        return adminRepository.save(admin);
    }
}