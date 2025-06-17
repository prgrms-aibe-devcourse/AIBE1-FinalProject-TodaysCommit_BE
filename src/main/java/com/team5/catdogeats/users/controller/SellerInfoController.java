package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.users.dto.ApiResponse;
import com.team5.catdogeats.users.dto.SellerInfoRequest;
import com.team5.catdogeats.users.dto.SellerInfoResponse;
import com.team5.catdogeats.users.service.SellerInfoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/sellers")
@RequiredArgsConstructor
public class SellerInfoController {

    private final SellerInfoService sellerInfoService;

    /**
     * 판매자 정보 조회 (본인만)
     * GET /v1/sellers/info
     */
    @GetMapping("/info")
    @PreAuthorize("hasRole('ROLE_SELLERS')")
    public ResponseEntity<ApiResponse<SellerInfoResponse>> getSellerInfo(
            Authentication authentication) {

        try {
            UUID userId = UUID.fromString(authentication.getName());
            log.info("판매자 정보 조회 요청 - userId: {}", userId);

            SellerInfoResponse sellerInfo = sellerInfoService.getSellerInfo(userId);

            if (sellerInfo == null) {
                return ResponseEntity.ok(
                        ApiResponse.success("등록된 판매자 정보가 없습니다.", null)
                );
            }

            return ResponseEntity.ok(
                    ApiResponse.success("판매자 정보 조회 성공", sellerInfo)
            );

        } catch (Exception e) {
            log.error("판매자 정보 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다.", "/v1/sellers/info", null));
        }
    }

    /**
     * 판매자 정보 등록/수정 (본인만)
     * PATCH /v1/sellers/info
     */
    @PatchMapping("/info")
    @PreAuthorize("hasRole('ROLE_SELLERS')")
    public ResponseEntity<ApiResponse<SellerInfoResponse>> upsertSellerInfo(
            @Valid @RequestBody SellerInfoRequest request,
            BindingResult bindingResult,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        try {
            UUID userId = UUID.fromString(authentication.getName());
            log.info("판매자 정보 등록/수정 요청 - userId: {}, vendorName: {}", userId, request.getVendorName());

            // 유효성 검증 오류 처리
            if (bindingResult.hasErrors()) {
                List<ApiResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                        .map(error -> ApiResponse.FieldError.builder()
                                .field(error.getField())
                                .message(error.getDefaultMessage())
                                .build())
                        .collect(Collectors.toList());

                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("입력 정보가 올바르지 않습니다.",
                                httpRequest.getRequestURI(), fieldErrors));
            }

            // 기존 판매자 정보 확인 (신규/수정 구분용)
            SellerInfoResponse existingInfo = sellerInfoService.getSellerInfo(userId);
            boolean isNewRegistration = (existingInfo == null);

            // 판매자 정보 저장
            SellerInfoResponse savedInfo = sellerInfoService.upsertSellerInfo(userId, request);

            // 응답 메시지 및 상태 코드 결정
            String message = isNewRegistration ?
                    "판매자 정보가 성공적으로 등록되었습니다." :
                    "판매자 정보가 성공적으로 수정되었습니다.";

            HttpStatus status = isNewRegistration ? HttpStatus.CREATED : HttpStatus.OK;

            return ResponseEntity.status(status)
                    .body(ApiResponse.success(message, savedInfo));

        } catch (IllegalArgumentException e) {
            log.warn("판매자 정보 등록/수정 중 유효성 검증 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage(), httpRequest.getRequestURI(), null));

        } catch (Exception e) {
            log.error("판매자 정보 등록/수정 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("서버 오류가 발생했습니다.", httpRequest.getRequestURI(), null));
        }
    }
}