package com.team5.catdogeats.admins.service;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.enums.AdminRole;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 애플리케이션 시작 시 슈퍼관리자 계정을 자동으로 생성하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminInitializer {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.super.email}")
    private String superAdminEmail;

    @Value("${admin.super.name}")
    private String superAdminName;

    @Value("${admin.super.password}")
    private String superAdminPassword;

    /**
     * 애플리케이션 시작 시 슈퍼관리자 계정 생성
     */
    @Bean
    public ApplicationRunner initializeSuperAdmin() {
        return args -> {
            try {
                // 이미 슈퍼관리자가 존재하는지 확인
                if (adminRepository.existsByEmail(superAdminEmail)) {
                    log.info("슈퍼관리자 계정이 이미 존재합니다: {}", superAdminEmail);
                    return;
                }

                // 슈퍼관리자 계정 생성
                Admins superAdmin = Admins.builder()
                        .email(superAdminEmail)
                        .password(passwordEncoder.encode(superAdminPassword))
                        .name(superAdminName)
                        .department(Department.ADMIN)
                        .adminRole(AdminRole.ROLE_ADMIN)
                        .isActive(true)  // 슈퍼관리자는 바로 활성화
                        .isFirstLogin(false)  // 슈퍼관리자는 첫 로그인이 아님
                        .build();

                adminRepository.save(superAdmin);
                log.info("슈퍼관리자 계정이 생성되었습니다: email={}, name={}", superAdminEmail, superAdminName);

            } catch (Exception e) {
                log.error("슈퍼관리자 계정 생성 중 오류 발생: ", e);
            }
        };
    }
}