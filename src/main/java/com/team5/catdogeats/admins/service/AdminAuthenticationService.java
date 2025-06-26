package com.team5.catdogeats.admins.service;

import com.team5.catdogeats.admins.domain.dto.*;
import jakarta.servlet.http.HttpSession;

/**
 * 관리자 인증 서비스 인터페이스
 */
public interface AdminAuthenticationService {
    /**
     * 관리자 로그인
     */
    AdminLoginResponseDTO login(AdminLoginRequestDTO request, HttpSession session);

    /**
     * 관리자 로그아웃
     */
    void logout(HttpSession session);

    /**
     * 비밀번호 변경
     */
    void changePassword(AdminPasswordChangeRequestDTO request, HttpSession session);

    /**
     * 세션에서 관리자 정보 조회
     */
    AdminSessionInfo getSessionInfo(HttpSession session);

    /**
     * 로그인 상태 확인
     */
    boolean isLoggedIn(HttpSession session);
}