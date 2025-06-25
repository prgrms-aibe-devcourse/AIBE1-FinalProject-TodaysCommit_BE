package com.team5.catdogeats.users.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 판매자 스토어 페이지 최종 응답 DTO
 */
@Schema(description = "판매자 스토어 페이지 응답")
public record SellerStorePageResponse(
        @Schema(description = "판매자 정보")
        SellerStoreInfoDTO sellerInfo,

        @Schema(description = "상품 목록 (페이징)")
        ProductCardPageResponseDTO products
) {

    public static SellerStorePageResponse of(SellerStoreInfoDTO sellerInfo, ProductCardPageResponseDTO products) {
        return new SellerStorePageResponse(sellerInfo, products);
    }
}