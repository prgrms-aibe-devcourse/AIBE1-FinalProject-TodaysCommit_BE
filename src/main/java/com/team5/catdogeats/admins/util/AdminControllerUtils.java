package com.team5.catdogeats.admins.util;

import com.team5.catdogeats.admins.domain.dto.AdminSessionInfo;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.service.AdminAuthenticationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

/**
 * 관리자 컨트롤러 공통 유틸리티
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminControllerUtils {

    private final AdminAuthenticationService authenticationService;

    /**
     * 세션에서 관리자 정보 조회 (필수)
     * 세션이 없거나 유효하지 않으면 예외 발생
     */
    public AdminSessionInfo requireSessionInfo(HttpSession session) {
        AdminSessionInfo sessionInfo = authenticationService.getSessionInfo(session);
        if (sessionInfo == null || !sessionInfo.isValid()) {
            throw new BadCredentialsException("로그인이 필요합니다.");
        }
        return sessionInfo;
    }

    /**
     * ADMIN 부서 권한 확인 (필수)
     * ADMIN 부서가 아니면 예외 발생
     */
    public AdminSessionInfo requireAdminDepartment(HttpSession session) {
        AdminSessionInfo sessionInfo = requireSessionInfo(session);
        if (!Department.ADMIN.equals(sessionInfo.getDepartment())) {
            throw new AccessDeniedException("ADMIN 부서만 접근할 수 있습니다.");
        }
        return sessionInfo;
    }

    /**
     * ADMIN 부서 사용자인지 확인 (boolean 반환)
     */
    public boolean isAdminDepartmentUser(HttpSession session) {
        try {
            AdminSessionInfo sessionInfo = authenticationService.getSessionInfo(session);
            return sessionInfo != null && Department.ADMIN.equals(sessionInfo.getDepartment());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 세션 사용자 정보 문자열 (로깅용)
     */
    public String getSessionUserInfo(HttpSession session) {
        try {
            AdminSessionInfo sessionInfo = authenticationService.getSessionInfo(session);
            if (sessionInfo == null) {
                return "익명사용자";
            }
            return String.format("email=%s, department=%s",
                    sessionInfo.getEmail(), sessionInfo.getDepartment());
        } catch (Exception e) {
            return "정보조회실패";
        }
    }

    /**
     * 첫 로그인인지 확인하여 비밀번호 변경 페이지로 리다이렉트 필요한지 판단
     */
    public String checkFirstLoginRedirect(HttpSession session) {
        AdminSessionInfo sessionInfo = authenticationService.getSessionInfo(session);
        if (sessionInfo == null) {
            return "redirect:/v1/admin/login";
        }
        if (sessionInfo.isFirstLogin()) {
            return "redirect:/v1/admin/change-password";
        }
        return null; // 리다이렉트 불필요
    }

    /**
     * 페이지 접근 권한 검증 (뷰 페이지용)
     * 로그인 되어있지 않으면 로그인 페이지로, ADMIN 부서가 아니면 대시보드로
     */
    public String validatePageAccess(HttpSession session, boolean requireAdminDepartment) {
        AdminSessionInfo sessionInfo = authenticationService.getSessionInfo(session);

        if (sessionInfo == null) {
            return "redirect:/v1/admin/login";
        }

        if (sessionInfo.isFirstLogin()) {
            return "redirect:/v1/admin/change-password";
        }

        if (requireAdminDepartment && !Department.ADMIN.equals(sessionInfo.getDepartment())) {
            log.warn("ADMIN 부서가 아닌 사용자의 접근 시도: {}", getSessionUserInfo(session));
            return "redirect:/v1/admin/dashboard?error=access_denied";
        }

        return null; // 접근 허용
    }
}