package com.team5.catdogeats.admins.controller;

import com.team5.catdogeats.admins.domain.dto.*;
import com.team5.catdogeats.admins.service.AdminAuthenticationService;
import com.team5.catdogeats.admins.util.AdminControllerUtils;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 인증 컨트롤러
 * 세션 기반 로그인/로그아웃 처리
 */
@Slf4j
@Controller
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Authentication", description = "관리자 인증 API")
public class AdminAuthController {

    private final AdminAuthenticationService authService;
    private final AdminControllerUtils controllerUtils;

    /**
     * 로그인 페이지
     */
    @GetMapping("/login")
    public String showLoginPage(HttpSession session) {
        // 이미 로그인된 상태라면 적절한 페이지로 리다이렉트
        AdminSessionInfo sessionInfo = authService.getSessionInfo(session);
        if (sessionInfo != null && sessionInfo.isValid()) {
            log.debug("이미 로그인된 관리자: {}", sessionInfo.getEmail());
            return sessionInfo.isFirstLogin()
                    ? "redirect:/v1/admin/change-password"
                    : "redirect:/v1/admin/dashboard";
        }
        return "thymeleaf/administratorPage_login";
    }

    /**
     * 로그인 처리
     */
    @PostMapping("/login")
    @ResponseBody
    @Operation(summary = "관리자 로그인", description = "관리자 계정으로 로그인합니다.")
    public ResponseEntity<ApiResponse<AdminLoginResponseDTO>> login(
            @Valid @RequestBody AdminLoginRequestDTO request,
            HttpSession session) {

        AdminLoginResponseDTO response = authService.login(request, session);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    /**
     * 로그아웃 처리
     */
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        authService.logout(session);
        return "redirect:/v1/admin/login?logout=true";
    }

    /**
     * 비밀번호 변경 페이지
     */
    @GetMapping("/change-password")
    public String showChangePasswordPage(HttpSession session, Model model) {
        AdminSessionInfo sessionInfo = controllerUtils.requireSessionInfo(session);
        model.addAttribute("adminName", sessionInfo.getName());
        model.addAttribute("isFirstLogin", sessionInfo.isFirstLogin());
        return "thymeleaf/administratorPage_change_password";
    }

    /**
     * 비밀번호 변경 처리 (예외 처리 간소화)
     */
    @PostMapping("/change-password")
    @ResponseBody
    @Operation(summary = "관리자 비밀번호 변경", description = "관리자의 비밀번호를 변경합니다.")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody AdminPasswordChangeRequestDTO request,
            HttpSession session) {

        authService.changePassword(request, session);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, "비밀번호가 성공적으로 변경되었습니다."));
    }

    /**
     * 대시보드 페이지
     */
    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        String redirectCheck = controllerUtils.checkFirstLoginRedirect(session);
        if (redirectCheck != null) {
            return redirectCheck;
        }

        AdminSessionInfo sessionInfo = controllerUtils.requireSessionInfo(session);
        model.addAttribute("admin", sessionInfo);
        return "thymeleaf/administratorPage_dashboard";
    }

    /**
     * 관리자 정보 조회
     */
    @GetMapping("/profile")
    @ResponseBody
    @Operation(summary = "관리자 정보 조회", description = "현재 로그인한 관리자의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<AdminSessionInfo>> getAdminProfile(HttpSession session) {
        AdminSessionInfo sessionInfo = controllerUtils.requireSessionInfo(session);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, sessionInfo));
    }
}
