package com.team5.catdogeats.admins.controller;

import com.team5.catdogeats.admins.domain.dto.AdminInvitationRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminInvitationResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminVerificationRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminVerificationResponseDTO;
import com.team5.catdogeats.admins.service.AdminInvitationService;
import com.team5.catdogeats.admins.service.AdminVerificationService;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    /**
     * 관리자 초대 페이지
     */
    @GetMapping("/invite")
    public String showInvitePage() {
        return "thymeleaf/admin/invite";
    }

    /**
     * 관리자 초대 처리
     */
    @PostMapping("/invite")
    @ResponseBody
    @Operation(summary = "관리자 초대", description = "슈퍼관리자가 새로운 관리자를 초대합니다.")
    public ResponseEntity<ApiResponse<AdminInvitationResponseDTO>> inviteAdmin(
            @Valid @RequestBody AdminInvitationRequestDTO request) {

        try {
            AdminInvitationResponseDTO response = invitationService.inviteAdmin(request);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (Exception e) {
            log.error("관리자 초대 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        }
    }

    /**
     * 계정 인증 페이지
     */
    @GetMapping("/verify")
    public String showVerifyPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email != null ? email : "");
        return "thymeleaf/admin/verify";
    }

    /**
     * 계정 인증 처리
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
            log.error("계정 인증 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        }
    }

    /**
     * 인증코드 재발송
     */
    @PostMapping("/resend-code")
    @ResponseBody
    @Operation(summary = "인증코드 재발송", description = "만료된 인증코드를 재발송합니다.")
    public ResponseEntity<ApiResponse<String>> resendVerificationCode(@RequestParam String email) {
        try {
            verificationService.resendVerificationCode(email);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, "인증코드가 재발송되었습니다."));

        } catch (Exception e) {
            log.error("인증코드 재발송 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        }
    }
}