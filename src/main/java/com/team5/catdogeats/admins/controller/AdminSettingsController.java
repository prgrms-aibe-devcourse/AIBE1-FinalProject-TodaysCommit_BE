package com.team5.catdogeats.admins.controller;

import com.team5.catdogeats.admins.domain.dto.AdminSessionInfo;
import com.team5.catdogeats.admins.util.AdminControllerUtils;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 설정 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Settings", description = "관리자 설정 API")
public class AdminSettingsController {

    private final AdminControllerUtils controllerUtils;

    /**
     * 설정 페이지 표시
     */
    @GetMapping("/settings")
    public String showSettingsPage(HttpSession session) {
        String redirectResult = controllerUtils.checkFirstLoginRedirect(session);
        if (redirectResult != null) {
            return redirectResult;
        }
        return "thymeleaf/admin/settings";
    }

    /**
     * 프로필 업데이트 (이름만 변경) - 예외 처리 간소화
     */
    @PostMapping("/profile/update")
    @ResponseBody
    @Operation(summary = "관리자 프로필 업데이트")
    public ResponseEntity<ApiResponse<String>> updateAdminProfile(
            @RequestBody UpdateProfileRequest request,
            HttpSession session) {

        AdminSessionInfo sessionInfo = controllerUtils.requireSessionInfo(session);

        // 입력값 검증
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }

        // 세션 정보 업데이트
        sessionInfo.setName(request.name().trim());
        session.setAttribute("ADMIN_USER", sessionInfo);

        log.info("관리자 프로필 업데이트: email={}, newName={}",
                sessionInfo.getEmail(), request.name());

        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, "프로필이 업데이트되었습니다."));
    }

    public record UpdateProfileRequest(String name) {}
}