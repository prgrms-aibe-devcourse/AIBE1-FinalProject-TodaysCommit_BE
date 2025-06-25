package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.users.domain.dto.SellerBrandImageResponseDTO;
import com.team5.catdogeats.users.service.SellerBrandImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "판매자 브랜드 이미지 관리", description = "판매자 브랜드 이미지 업로드 및 수정 API")
public class SellerBrandImageController {

    private final SellerBrandImageService sellerBrandImageService;

    @Operation(
            summary = "판매자 브랜드 이미지 업로드/수정",
            description = """
                    판매자의 브랜드 이미지를 업로드하거나 수정합니다.
                    판매자 권한(ROLE_SELLER)이 필요합니다.
                    파일 크기: 최대 10MB, 지원 형식: JPEG, PNG, JPG, WebP
                    """
    )
    @PatchMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SellerBrandImageResponseDTO>> uploadBrandImage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,

            @Parameter(description = "업로드할 브랜드 이미지 파일")
            @RequestPart("image") MultipartFile imageFile) {

        log.info("판매자 브랜드 이미지 업로드 요청 - provider: {}, providerId: {}, fileName: {}",
                userPrincipal.provider(), userPrincipal.providerId(),
                imageFile != null ? imageFile.getOriginalFilename() : "null");

        SellerBrandImageResponseDTO response = sellerBrandImageService.uploadBrandImage(userPrincipal, imageFile);

        return ResponseEntity.ok(
                ApiResponse.success(ResponseCode.SELLER_INFO_SAVE_SUCCESS, response)
        );
    }
}