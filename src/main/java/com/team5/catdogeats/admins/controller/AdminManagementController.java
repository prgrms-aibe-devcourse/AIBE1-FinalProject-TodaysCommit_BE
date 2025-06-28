package com.team5.catdogeats.admins.controller;

import com.team5.catdogeats.admins.domain.dto.*;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.service.AdminInvitationService;
import com.team5.catdogeats.admins.service.AdminPasswordResetService;
import com.team5.catdogeats.admins.service.AdminVerificationService;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 관리 컨트롤러
 * 슈퍼관리자의 관리자 초대 및 계정 인증 기능
 */
@Slf4j
@Controller
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "관리자 관리 API")
public class AdminManagementController {

    private final AdminInvitationService invitationService;
    private final AdminVerificationService verificationService;
    private final AdminAuthenticationService authenticationService;
    private final AdminPasswordResetService passwordResetService;
    /**
     * 관리자 초대 페이지 (ADMIN 부서만 접근 가능)
     */
    @GetMapping("/invite")
    public String showInvitePage(HttpSession session) {
        // 권한 검증: ADMIN 부서만 접근 가능
        if (!isAdminDepartmentUser(session)) {
            log.warn("비ADMIN 부서 사용자의 초대 페이지 접근 시도: {}",
                    getSessionUserInfo(session));
            return "redirect:/v1/admin/dashboard?error=access_denied";
        }

        return "thymeleaf/admin/invite";
    }

    /**
     * 관리자 초대 처리 (ADMIN 부서만 실행 가능)
     */
    @PostMapping("/invite")
    @ResponseBody
    @Operation(summary = "관리자 초대", description = "슈퍼관리자가 새로운 관리자를 초대합니다.")
    public ResponseEntity<ApiResponse<AdminInvitationResponseDTO>> inviteAdmin(
            @Valid @RequestBody AdminInvitationRequestDTO request,
            HttpSession session) {

        try {
            // 권한 검증: ADMIN 부서만 실행 가능
            if (!isAdminDepartmentUser(session)) {
                log.warn("비ADMIN 부서 사용자의 관리자 초대 시도: {}, 요청: {}",
                        getSessionUserInfo(session), request.email());
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(ResponseCode.ACCESS_DENIED, "ADMIN 부서만 관리자를 초대할 수 있습니다."));
            }

            AdminInvitationResponseDTO response = invitationService.inviteAdmin(request);

            log.info("관리자 초대 성공: 초대자={}, 피초대자={}",
                    getSessionUserInfo(session), request.email());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (Exception e) {
            log.error("관리자 초대 중 오류 발생: 초대자={}, 요청={}, 오류={}",
                    getSessionUserInfo(session), request.email(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        }
    }

    /**
     * 계정 인증 페이지 (모든 사용자 접근 가능)
     */
    @GetMapping("/verify")
    public String showVerifyPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email != null ? email : "");
        return "thymeleaf/admin/verify";
    }

    /**
     * 계정 인증 처리 (모든 사용자 실행 가능)
     */
    @PostMapping("/verify")
    @ResponseBody
    @Operation(summary = "계정 인증", description = "인증코드를 통해 관리자 계정을 활성화합니다.")
    public ResponseEntity<ApiResponse<AdminVerificationResponseDTO>> verifyAdmin(
            @Valid @RequestBody AdminVerificationRequestDTO request) {

        try {
            AdminVerificationResponseDTO response = verificationService.verifyAdmin(request);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (Exception e) {
            log.error("계정 인증 중 오류 발생: email={}, 오류={}", request.email(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        }
    }


    /**
     * 인증코드 재발송 (모든 사용자 실행 가능)
     */
    @PostMapping("/resend-code")
    @ResponseBody
    @Operation(summary = "인증코드 재발송", description = "만료된 인증코드를 재발송합니다.")
    public ResponseEntity<ApiResponse<String>> resendVerificationCode(@RequestParam String email) {
        try {
            verificationService.resendVerificationCode(email);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, "인증코드가 재발송되었습니다."));

        } catch (Exception e) {
            log.error("인증코드 재발송 중 오류 발생: email={}, 오류={}", email, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        }
    }



    /**
     *  관리자 비밀번호 초기화 요청
     */
    @PostMapping("/reset-password")
    @ResponseBody
    @Operation(summary = "관리자 비밀번호 초기화", description = "관리자의 비밀번호를 초기화하고 인증 메일을 발송합니다.")
    public ResponseEntity<ApiResponse<AdminPasswordResetResponseDTO>> resetAdminPassword(
            @Valid @RequestBody AdminPasswordResetRequestDTO request,
            HttpSession session) {

        try {
            // 권한 검증
            if (!isAdminDepartmentUser(session)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(ResponseCode.ACCESS_DENIED, "ADMIN 부서만 비밀번호를 초기화할 수 있습니다."));
            }

            AdminPasswordResetResponseDTO response = passwordResetService.requestPasswordReset(request);

            log.info("비밀번호 초기화 요청 성공: target={}, requestedBy={}",
                    request.email(), request.requestedBy());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        } catch (Exception e) {
            log.error("비밀번호 초기화 중 오류 발생: ", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }


    /**
     *  비밀번호 재설정 처리
     */
    @PostMapping("/verify-reset-password")
    @ResponseBody
    @Operation(summary = "비밀번호 재설정", description = "인증코드 확인 후 새 비밀번호를 설정합니다.")
    public ResponseEntity<ApiResponse<AdminVerificationResponseDTO>> verifyAndResetPassword(
            @Valid @RequestBody AdminPasswordResetVerificationDTO request) {

        try {
            AdminVerificationResponseDTO response = passwordResetService.verifyAndResetPassword(request);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (Exception e) {
            log.error("비밀번호 재설정 중 오류 발생: email={}, 오류={}", request.email(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        }
    }


    /**
     * ADMIN 부서 사용자인지 확인
     */
    private boolean isAdminDepartmentUser(HttpSession session) {
        AdminSessionInfo sessionInfo = authenticationService.getSessionInfo(session);
        if (sessionInfo == null) {
            return false;
        }
        return Department.ADMIN.equals(sessionInfo.getDepartment());
    }

    /**
     * 세션 사용자 정보 로깅용
     */
    private String getSessionUserInfo(HttpSession session) {
        AdminSessionInfo sessionInfo = authenticationService.getSessionInfo(session);
        if (sessionInfo == null) {
            return "익명사용자";
        }
        return String.format("email=%s, department=%s",
                sessionInfo.getEmail(), sessionInfo.getDepartment());
    }
}