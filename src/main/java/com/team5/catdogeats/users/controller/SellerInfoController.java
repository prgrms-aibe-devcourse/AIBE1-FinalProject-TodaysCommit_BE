package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.users.domain.dto.SellerInfoRequest;
import com.team5.catdogeats.users.domain.dto.SellerInfoResponse;
import com.team5.catdogeats.users.service.SellerInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
                    JWT 토큰에서 사용자 정보를 추출하여 해당 판매자의 정보를 조회합니다.
                    판매자 권한(ROLE_SELLER)이 필요합니다.
                    """
    )
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<SellerInfoResponse>> getSellerInfo(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("판매자 정보 조회 요청 - provider: {}, providerId: {}",
                userPrincipal.provider(), userPrincipal.providerId());

        SellerInfoResponse response = sellerInfoService.getSellerInfo(userPrincipal);

        if (response == null) {
            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SELLER_INFO_NOT_FOUND)
            );
        }

        return ResponseEntity.ok(
                ApiResponse.success(ResponseCode.SELLER_INFO_SUCCESS, response)
        );
    }

    /**
     * 판매자 정보 등록/수정
     */
    @Operation(
            summary = "판매자 정보 등록/수정",
            description = """
                    JWT 토큰에서 사용자 정보를 추출하여 해당 판매자의 정보를 등록하거나 수정합니다.
                    판매자 권한(ROLE_SELLER)이 필요합니다.
                    기존 정보가 있으면 수정, 없으면 신규 등록됩니다.
                    """
    )
    @PatchMapping("/info")
    public ResponseEntity<ApiResponse<SellerInfoResponse>> upsertSellerInfo(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SellerInfoRequest request) {

        log.info("판매자 정보 등록/수정 요청 - provider: {}, providerId: {}, vendorName: {}",
                userPrincipal.provider(), userPrincipal.providerId(), request.vendorName());

        SellerInfoResponse response = sellerInfoService.upsertSellerInfo(userPrincipal, request);

        return ResponseEntity.ok(
                ApiResponse.success(ResponseCode.SELLER_INFO_SAVE_SUCCESS, response)
        );
    }
}