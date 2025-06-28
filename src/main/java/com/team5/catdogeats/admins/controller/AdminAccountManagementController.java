package com.team5.catdogeats.admins.controller;

import com.team5.catdogeats.admins.domain.dto.*;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.service.AdminInvitationService;
import com.team5.catdogeats.admins.service.AdminManagementService;
import com.team5.catdogeats.admins.service.AdminPasswordResetService;
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
 * 관리자 계정 관리 컨트롤러
 * ADMIN 부서 사용자만 접근 가능
 */
@Slf4j
@Controller
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Account Management", description = "관리자 계정 관리 API")
public class AdminAccountManagementController {

    private final AdminInvitationService invitationService;
    private final AdminManagementService managementService;
    private final AdminPasswordResetService passwordResetService;
    private final AdminControllerUtils controllerUtils;

    /**
     * 관리자 계정 관리 페이지 (ADMIN 부서만 접근 가능)
     */
    @GetMapping("/account-management")
    public String showAccountManagementPage(HttpSession session, Model model) {
        String redirectResult = controllerUtils.validatePageAccess(session, true);
        if (redirectResult != null) {
            return redirectResult;
        }

        AdminSessionInfo sessionInfo = controllerUtils.requireAdminDepartment(session);
        model.addAttribute("admin", sessionInfo);
        return "thymeleaf/administratorPage_account_management";
    }

    /**
     * 관리자 목록 조회 API
     */
    @GetMapping("/accounts")
    @ResponseBody
    @Operation(summary = "관리자 목록 조회", description = "페이지네이션과 필터링이 적용된 관리자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<AdminListResponseDTO>> getAdminList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Department department,
            HttpSession session) {

        controllerUtils.requireAdminDepartment(session);
        AdminListResponseDTO response = managementService.getAdminList(page, size, status, search, department);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    /**
     * 관리자 추가
     */
    @PostMapping("/accounts")
    @ResponseBody
    @Operation(summary = "관리자 추가", description = "새로운 관리자를 초대합니다.")
    public ResponseEntity<ApiResponse<AdminInvitationResponseDTO>> createAdmin(
            @Valid @RequestBody AdminInvitationRequestDTO request,
            HttpSession session) {

        controllerUtils.requireAdminDepartment(session);
        AdminInvitationResponseDTO response = invitationService.inviteAdmin(request);

        log.info("관리자 추가 성공: 추가자={}, 피추가자={}",
                controllerUtils.getSessionUserInfo(session), request.email());

        return ResponseEntity.ok(ApiResponse.success(ResponseCode.CREATED, response));
    }

    /**
     * 관리자 비밀번호 초기화 요청
     */
    @PostMapping("/accounts/{adminEmail}/reset-password")
    @ResponseBody
    @Operation(summary = "관리자 비밀번호 초기화", description = "관리자의 비밀번호를 초기화하고 인증 메일을 발송합니다.")
    public ResponseEntity<ApiResponse<AdminPasswordResetResponseDTO>> resetAdminPassword(
            @PathVariable String adminEmail,
            HttpSession session) {

        AdminSessionInfo sessionInfo = controllerUtils.requireAdminDepartment(session);
        AdminPasswordResetRequestDTO request = new AdminPasswordResetRequestDTO(
                adminEmail,
                sessionInfo.getEmail()
        );

        AdminPasswordResetResponseDTO response = passwordResetService.requestPasswordReset(request);

        log.info("비밀번호 초기화 요청 성공: target={}, requestedBy={}",
                adminEmail, sessionInfo.getEmail());

        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }
}