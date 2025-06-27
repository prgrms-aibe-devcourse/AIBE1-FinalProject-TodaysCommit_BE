package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.*;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.AdminAuthenticationService;
import com.team5.catdogeats.global.config.JpaTransactional;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * 관리자 인증 서비스 구현체
 * Spring Security 세션 기반 로그인 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthenticationServiceImpl implements AdminAuthenticationService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public static final String ADMIN_SESSION_KEY = "ADMIN_USER";

    @Override
    @JpaTransactional
    public AdminLoginResponseDTO login(AdminLoginRequestDTO request, HttpSession session) {
        // 1. 이메일로 관리자 조회
        Admins admin = adminRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 2. 계정 활성화 상태 확인
        if (!admin.getIsActive()) {
            throw new IllegalStateException("계정이 활성화되지 않았습니다. 이메일을 확인해주세요.");
        }

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 4. 로그인 시간 업데이트
        admin.updateLastLogin();
        adminRepository.save(admin);

        // 5. Spring Security Authentication 객체 생성 및 설정 (부서별 권한 포함)
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        // 부서별 권한 추가
        authorities.add(new SimpleGrantedAuthority(admin.getDepartment().name()));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                admin.getEmail(),
                null, // 비밀번호는 null로 설정 (보안상)
                authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 6. 세션에 관리자 정보 저장 (추가 정보용)
        AdminSessionInfo sessionInfo = AdminSessionInfo.builder()
                .adminId(admin.getId())
                .email(admin.getEmail())
                .name(admin.getName())
                .department(admin.getDepartment())
                .loginTime(ZonedDateTime.now())
                .isFirstLogin(admin.getIsFirstLogin())
                .build();

        session.setAttribute(ADMIN_SESSION_KEY, sessionInfo);
        session.setMaxInactiveInterval(30 * 60); // 30분 세션 만료

        log.info("관리자 로그인 성공: email={}, name={}, department={}",
                admin.getEmail(), admin.getName(), admin.getDepartment());

        return AdminLoginResponseDTO.builder()
                .email(admin.getEmail())
                .name(admin.getName())
                .department(admin.getDepartment())
                .isFirstLogin(admin.getIsFirstLogin())
                .lastLoginAt(admin.getLastLoginAt())
                .redirectUrl(admin.getIsFirstLogin() ? "/v1/admin/change-password" : "/v1/admin/dashboard")
                .message("로그인 성공")
                .build();
    }

    @Override
    public void logout(HttpSession session) {
        AdminSessionInfo sessionInfo = getSessionInfo(session);
        if (sessionInfo != null) {
            log.info("관리자 로그아웃: email={}", sessionInfo.getEmail());
        }

        // Spring Security 컨텍스트 클리어
        SecurityContextHolder.clearContext();
        session.invalidate();
    }

    @Override
    @JpaTransactional
    public void changePassword(AdminPasswordChangeRequestDTO request, HttpSession session) {
        AdminSessionInfo sessionInfo = getSessionInfo(session);
        if (sessionInfo == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        Admins admin = adminRepository.findById(sessionInfo.getAdminId())
                .orElseThrow(() -> new IllegalStateException("관리자를 찾을 수 없습니다."));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.currentPassword(), admin.getPassword())) {
            throw new BadCredentialsException("현재 비밀번호가 올바르지 않습니다.");
        }

        // 새 비밀번호와 확인 비밀번호 일치 검증
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        // 비밀번호 변경
        admin.changePassword(passwordEncoder.encode(request.newPassword()));
        adminRepository.save(admin);

        // 세션 정보 업데이트
        sessionInfo.setFirstLogin(false);
        session.setAttribute(ADMIN_SESSION_KEY, sessionInfo);

        log.info("관리자 비밀번호 변경 완료: email={}", admin.getEmail());
    }

    @Override
    public AdminSessionInfo getSessionInfo(HttpSession session) {
        return (AdminSessionInfo) session.getAttribute(ADMIN_SESSION_KEY);
    }

    @Override
    public boolean isLoggedIn(HttpSession session) {
        // Spring Security 인증 상태와 세션 정보 둘 다 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getPrincipal()) &&
                getSessionInfo(session) != null;
    }
}