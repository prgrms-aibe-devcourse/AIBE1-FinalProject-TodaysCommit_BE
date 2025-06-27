package com.team5.catdogeats.admins.controller;

import com.team5.catdogeats.admins.domain.dto.AdminInvitationRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminInvitationResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminListResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminSessionInfo;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.service.AdminAuthenticationService;
import com.team5.catdogeats.admins.service.AdminInvitationService;
import com.team5.catdogeats.admins.service.AdminManagementService;
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
 * 슈퍼관리자가 일반 관리자 계정들을 관리하는 기능
 */
@Slf4j
@Controller
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Account Management", description = "관리자 계정 관리 API")
public class AdminAccountManagementController {

    private final AdminAuthenticationService authenticationService;
    private final AdminInvitationService invitationService;
    private final AdminManagementService managementService;

    /**
     * 관리자 계정 관리 페이지 (ADMIN 부서만 접근 가능)
     */
    @GetMapping("/account-management")
    public String showAccountManagementPage(HttpSession session, Model model) {
        // 권한 검증: ADMIN 부서만 접근 가능
        if (!isAdminDepartmentUser(session)) {
            log.warn("비ADMIN 부서 사용자의 계정 관리 페이지 접근 시도: {}",
                    getSessionUserInfo(session));
            return "redirect:/v1/admin/dashboard?error=access_denied";
        }

        AdminSessionInfo sessionInfo = authenticationService.getSessionInfo(session);
        model.addAttribute("admin", sessionInfo);

        return "thymeleaf/admin/account-management";
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

        try {
            // 권한 검증
            if (!isAdminDepartmentUser(session)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(ResponseCode.ACCESS_DENIED, "ADMIN 부서만 접근할 수 있습니다."));
            }

            AdminListResponseDTO response = managementService.getAdminList(page, size, status, search, department);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (Exception e) {
            log.error("관리자 목록 조회 중 오류 발생: ", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 관리자 추가 (기존 초대 기능 활용)
     */
    @PostMapping("/accounts")
    @ResponseBody
    @Operation(summary = "관리자 추가", description = "새로운 관리자를 초대합니다.")
    public ResponseEntity<ApiResponse<AdminInvitationResponseDTO>> createAdmin(
            @Valid @RequestBody AdminInvitationRequestDTO request,
            HttpSession session) {

        try {
            // 권한 검증
            if (!isAdminDepartmentUser(session)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(ResponseCode.ACCESS_DENIED, "ADMIN 부서만 관리자를 추가할 수 있습니다."));
            }

            AdminInvitationResponseDTO response = invitationService.inviteAdmin(request);

            log.info("관리자 추가 성공: 추가자={}, 피추가자={}",
                    getSessionUserInfo(session), request.email());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.CREATED, response));

        } catch (Exception e) {
            log.error("관리자 추가 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        }
    }

    /**
     * 관리자 상태 토글 (활성화/비활성화) - 이메일 기반
     */
    @PatchMapping("/accounts/{adminEmail}/toggle-status")
    @ResponseBody
    @Operation(summary = "관리자 상태 토글", description = "관리자 계정의 활성화 상태를 변경합니다.")
    public ResponseEntity<ApiResponse<String>> toggleAdminStatus(
            @PathVariable String adminEmail,
            HttpSession session) {

        try {
            // 권한 검증
            if (!isAdminDepartmentUser(session)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(ResponseCode.ACCESS_DENIED));
            }

            AdminSessionInfo sessionInfo = authenticationService.getSessionInfo(session);
            String result = managementService.toggleAdminStatus(adminEmail, sessionInfo.getEmail());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, result));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.ACCESS_DENIED, e.getMessage()));
        } catch (Exception e) {
            log.error("관리자 상태 변경 중 오류 발생: ", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 관리자 삭제 - 이메일 기반
     */
    @DeleteMapping("/accounts/{adminEmail}")
    @ResponseBody
    @Operation(summary = "관리자 삭제", description = "관리자 계정을 삭제합니다.")
    public ResponseEntity<ApiResponse<String>> deleteAdmin(
            @PathVariable String adminEmail,
            HttpSession session) {

        try {
            // 권한 검증
            if (!isAdminDepartmentUser(session)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error(ResponseCode.ACCESS_DENIED));
            }

            AdminSessionInfo sessionInfo = authenticationService.getSessionInfo(session);
            String result = managementService.deleteAdmin(adminEmail, sessionInfo.getEmail());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, result));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.ACCESS_DENIED, e.getMessage()));
        } catch (Exception e) {
            log.error("관리자 삭제 중 오류 발생: ", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    // === Private Helper Methods ===

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