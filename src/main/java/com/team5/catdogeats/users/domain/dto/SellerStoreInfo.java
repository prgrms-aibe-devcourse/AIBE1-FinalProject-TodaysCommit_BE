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
        Long totalProducts,

        @Schema(description = "전체 상품 판매량", example = "1250")
        Long totalSalesQuantity,

        @Schema(description = "판매량 정보 텍스트", example = "총 판매량: 1.3K개")
        String salesInfoText,

        @Schema(description = "배송 정보 텍스트", example = "배송 정보: 평균 3~5일 소요")
        String deliveryInfoText
) {

    public static SellerStoreInfo from(Sellers seller, Long totalProducts, SellerStoreStats stats) {
        if (seller == null) {
            return null;
        }

        String operatingHours = formatOperatingHours(
                seller.getOperatingStartTime(),
                seller.getOperatingEndTime()
        );

        String operationStartYear = formatOperationStartYear(seller.getCreatedAt());

        // 통계 정보가 없는 경우 기본값 처리
        Long totalSalesQuantity = stats != null ? stats.totalSalesQuantity() : 0L;
        String salesInfoText = stats != null ? stats.getSalesInfoText() : "총 판매량: 판매 실적 없음";
        String deliveryInfoText = stats != null ? stats.getDeliveryInfoText() : "배송 정보: 데이터 부족";

        return new SellerStoreInfo(
                seller.getUserId() != null ? seller.getUserId().toString() : null,
                seller.getVendorName(),
                seller.getVendorProfileImage(),
                seller.getTags(),
                operatingHours,
                operationStartYear,
                totalProducts,
                totalSalesQuantity,
                salesInfoText,
                deliveryInfoText
        );
    }

    /**
     * 기존 호환성을 위한 메서드 (통계 없이)
     */
    public static SellerStoreInfo from(Sellers seller, Long totalProducts) {
        return from(seller, totalProducts, null);
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