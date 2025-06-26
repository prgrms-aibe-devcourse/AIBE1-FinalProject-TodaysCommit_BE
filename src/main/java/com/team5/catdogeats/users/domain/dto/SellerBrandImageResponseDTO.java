package com.team5.catdogeats.users.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.ZonedDateTime;

@Schema(description = "판매자 브랜드 이미지 업로드 응답 DTO")
public record SellerBrandImageResponseDTO(
        @Schema(description = "사용자 ID", example = "2ceb807f-586f-4450-b470-d1ece7173749")
        String userId,

        @Schema(description = "업체명", example = "멍멍이네 수제간식")
        String vendorName,

        @Schema(description = "업데이트된 브랜드 이미지 URL", example = "https://cdn.example.com/images/brand_image_123.jpg")
        String vendorProfileImage,

        @Schema(description = "이미지 업로드 시간", example = "2024-01-20T14:20:00")
        ZonedDateTime updatedAt
) {
    public static SellerBrandImageResponseDTO from(com.team5.catdogeats.users.domain.mapping.Sellers seller) {
        return new SellerBrandImageResponseDTO(
                seller.getUserId(),
                seller.getVendorName(),
                seller.getVendorProfileImage(),
                seller.getUpdatedAt()
        );
    }
}
