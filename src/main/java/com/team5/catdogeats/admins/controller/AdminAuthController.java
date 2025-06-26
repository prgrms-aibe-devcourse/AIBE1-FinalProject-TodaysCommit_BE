package com.team5.catdogeats.admins.controller;

import com.team5.catdogeats.admins.domain.dto.*;
import com.team5.catdogeats.admins.service.AdminAuthenticationService;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * 로그인 페이지
     */
    @GetMapping("/login")
    public String showLoginPage(HttpSession session, Model model) {
        // 세션에 관리자 정보가 있으면 이미 로그인된 상태
        AdminSessionInfo sessionInfo = authService.getSessionInfo(session);

        if (sessionInfo != null && sessionInfo.isValid()) {
            log.debug("이미 로그인된 관리자: {}", sessionInfo.getEmail());
            // 첫 로그인이면 비밀번호 변경 페이지로
            if (sessionInfo.isFirstLogin()) {
                return "redirect:/v1/admin/change-password";
            }
            return "redirect:/v1/admin/dashboard";
        }

        return "thymeleaf/admin/login";
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

        try {
            AdminLoginResponseDTO response = authService.login(request, session);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (BadCredentialsException e) {
            log.warn("로그인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        } catch (IllegalStateException e) {
            log.warn("로그인 실패 (계정 미활성화): {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.ACCESS_DENIED, e.getMessage()));

        } catch (Exception e) {
            log.error("로그인 중 오류 발생: ", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
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
        AdminSessionInfo sessionInfo = authService.getSessionInfo(session);
        if (sessionInfo == null) {
            return "redirect:/v1/admin/login";
        }

        model.addAttribute("adminName", sessionInfo.getName());
        model.addAttribute("isFirstLogin", sessionInfo.isFirstLogin());
        return "thymeleaf/admin/change-password";
    }

    /**
     * 비밀번호 변경 처리
     */
    @PostMapping("/change-password")
    @ResponseBody
    @Operation(summary = "관리자 비밀번호 변경", description = "관리자의 비밀번호를 변경합니다.")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody AdminPasswordChangeRequestDTO request,
            HttpSession session) {

        try {
            authService.changePassword(request, session);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, "비밀번호가 성공적으로 변경되었습니다."));

        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.UNAUTHORIZED, "현재 비밀번호가 올바르지 않습니다."));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.UNAUTHORIZED, e.getMessage()));

        } catch (Exception e) {
            log.error("비밀번호 변경 중 오류 발생: ", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 대시보드 페이지
     */
    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        AdminSessionInfo sessionInfo = authService.getSessionInfo(session);
        if (sessionInfo == null) {
            return "redirect:/v1/admin/login";
        }

        // 첫 로그인이면 비밀번호 변경 페이지로 리다이렉트
        if (sessionInfo.isFirstLogin()) {
            return "redirect:/v1/admin/change-password";
        }

        model.addAttribute("admin", sessionInfo);
        return "thymeleaf/admin/dashboard";
    }

    /**
     * 관리자 정보 조회
     */
    @GetMapping("/profile")
    @ResponseBody
    @Operation(summary = "관리자 정보 조회", description = "현재 로그인한 관리자의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<AdminSessionInfo>> getAdminProfile(HttpSession session) {
        AdminSessionInfo sessionInfo = authService.getSessionInfo(session);
        if (sessionInfo == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }

        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, sessionInfo));
    }
}