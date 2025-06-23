package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.users.domain.dto.SellerInfoRequest;
import com.team5.catdogeats.users.domain.dto.SellerInfoResponse;
import com.team5.catdogeats.users.exception.BusinessNumberDuplicateException;
import com.team5.catdogeats.users.exception.InvalidOperatingHoursException;
import com.team5.catdogeats.users.exception.SellerAccessDeniedException;
import com.team5.catdogeats.users.exception.UserNotFoundException;
import com.team5.catdogeats.users.service.SellerInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "판매자 정보 관리", description = "판매자 정보 조회 및 등록/수정 API")
public class SellerInfoController {

    private final SellerInfoService sellerInfoService;

    /**
     * 판매자 정보 조회
     */
    @Operation(
            summary = "판매자 정보 조회",
            description = """
                    현재 개발 단계에서는 하드코딩된 사용자 ID를 사용합니다.
                    
                    **개발용 사용자 ID**: 2ceb807f-586f-4450-b470-d1ece7173749
                    
                    JWT 구현 완료 후에는 토큰에서 사용자 ID를 추출하여 사용합니다.
                    """
    )
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<SellerInfoResponse>> getSellerInfo() {
        // TODO: JWT 토큰에서 사용자 ID 추출하는 로직으로 교체 예정
        String tempUserId = ("2ceb807f-586f-4450-b470-d1ece7173749");
        log.info("판매자 정보 조회 요청 - 개발용 하드코딩 ID: {}", tempUserId);

        try {
            SellerInfoResponse response = sellerInfoService.getSellerInfo(tempUserId);

            if (response == null) {
                return ResponseEntity.ok(
                        ApiResponse.success(ResponseCode.SELLER_INFO_NOT_FOUND)
                );
            }

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SELLER_INFO_SUCCESS, response)
            );

        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음 - userId: {}", tempUserId, e);
            return ResponseEntity.status(ResponseCode.USER_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.USER_NOT_FOUND));

        } catch (SellerAccessDeniedException e) {
            log.error("판매자 권한 없음 - userId: {}", tempUserId, e);
            return ResponseEntity.status(ResponseCode.SELLER_ACCESS_DENIED.getStatus())
                    .body(ApiResponse.error(ResponseCode.SELLER_ACCESS_DENIED));

        } catch (Exception e) {
            log.error("판매자 정보 조회 중 오류 발생 - userId: {}", tempUserId, e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 판매자 정보 등록/수정
     */
    @Operation(
            summary = "판매자 정보 등록/수정",
            description = """
                    현재 개발 단계에서는 하드코딩된 사용자 ID를 사용합니다.
                    
                    **개발용 사용자 ID**: 2ceb807f-586f-4450-b470-d1ece7173749
                    
                    기존 정보가 있으면 수정, 없으면 신규 등록됩니다.
                    JWT 구현 완료 후에는 토큰에서 사용자 ID를 추출하여 사용합니다.
                    """
    )
    @PatchMapping("/info")
    public ResponseEntity<ApiResponse<SellerInfoResponse>> upsertSellerInfo(
            @Valid @RequestBody SellerInfoRequest request,
            BindingResult bindingResult) {

        // TODO: JWT 토큰에서 사용자 ID 추출하는 로직으로 교체 예정
        String tempUserId = ("2ceb807f-586f-4450-b470-d1ece7173749");
        log.info("판매자 정보 등록/수정 요청 - 개발용 하드코딩 ID: {}, vendorName: {}",
                tempUserId, request.vendorName());

        // 유효성 검증 오류 처리
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, errorMessage));
        }

        try {
            SellerInfoResponse response = sellerInfoService.upsertSellerInfo(tempUserId, request);

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SELLER_INFO_SAVE_SUCCESS, response)
            );

        } catch (UserNotFoundException e) {
            log.error("사용자를 찾을 수 없음 - userId: {}", tempUserId, e);
            return ResponseEntity.status(ResponseCode.USER_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.USER_NOT_FOUND));

        } catch (SellerAccessDeniedException e) {
            log.error("판매자 권한 없음 - userId: {}", tempUserId, e);
            return ResponseEntity.status(ResponseCode.SELLER_ACCESS_DENIED.getStatus())
                    .body(ApiResponse.error(ResponseCode.SELLER_ACCESS_DENIED));

        } catch (BusinessNumberDuplicateException e) {
            log.error("사업자 등록번호 중복 - userId: {}, businessNumber: {}",
                    tempUserId, request.businessNumber(), e);
            return ResponseEntity.status(ResponseCode.BUSINESS_NUMBER_DUPLICATE.getStatus())
                    .body(ApiResponse.error(ResponseCode.BUSINESS_NUMBER_DUPLICATE));

        } catch (InvalidOperatingHoursException e) {
            log.error("운영시간 유효성 검증 실패 - userId: {}, error: {}", tempUserId, e.getMessage(), e);
            return ResponseEntity.status(ResponseCode.INVALID_OPERATING_HOURS.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_OPERATING_HOURS, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.error("입력값 유효성 검증 실패 - userId: {}, error: {}", tempUserId, e.getMessage(), e);
            return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("판매자 정보 저장 중 오류 발생 - userId: {}", tempUserId, e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    // TODO: JWT 구현 후 실제 토큰 추출 로직 추가 예정
    /*
    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
    }
    */
}