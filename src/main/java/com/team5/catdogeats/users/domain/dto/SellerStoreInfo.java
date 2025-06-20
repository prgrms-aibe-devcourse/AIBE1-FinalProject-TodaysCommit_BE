package com.team5.catdogeats.users.domain.dto;

import com.team5.catdogeats.users.domain.mapping.Sellers;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 판매자 스토어 페이지 정보 DTO
 */
@Schema(description = "판매자 스토어 정보")
public record SellerStoreInfo(
        @Schema(description = "판매자 ID", example = "2ceb807f-586f-4450-b470-d1ece7173749")
        String sellerId,

        @Schema(description = "상점명", example = "멍멍이네 수제간식")
        String vendorName,

        @Schema(description = "상점 프로필 이미지", example = "https://example.com/profile.jpg")
        String vendorProfileImage,

        @Schema(description = "태그", example = "수제간식,강아지")
        String tags,

        @Schema(description = "운영시간", example = "09:00 - 18:00")
        String operatingHours,

        @Schema(description = "운영 시작년도", example = "2020년부터")
        String operationStartYear,

        @Schema(description = "총 상품 수", example = "50")
        Long totalProducts
) {

    public static SellerStoreInfo from(Sellers seller, Long totalProducts) {
        if (seller == null) {
            return null;
        }

        String operatingHours = formatOperatingHours(
                seller.getOperatingStartTime(),
                seller.getOperatingEndTime()
        );

        String operationStartYear = formatOperationStartYear(seller.getCreatedAt());

        return new SellerStoreInfo(
                seller.getUserId() != null ? seller.getUserId().toString() : null,
                seller.getVendorName(),
                seller.getVendorProfileImage(),
                seller.getTags(),
                operatingHours,
                operationStartYear,
                totalProducts
        );
    }

    private static String formatOperatingHours(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return "운영시간 미설정";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return String.format("%s - %s",
                startTime.format(formatter),
                endTime.format(formatter));
    }

    private static String formatOperationStartYear(java.time.ZonedDateTime createdAt) {
        if (createdAt == null) {
            return "정보 없음";
        }

        return createdAt.getYear() + "년부터";
    }
}