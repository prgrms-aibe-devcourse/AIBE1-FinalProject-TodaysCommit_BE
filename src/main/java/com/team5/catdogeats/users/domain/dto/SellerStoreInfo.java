package com.team5.catdogeats.users.domain.dto;

import com.team5.catdogeats.orders.domain.dto.SellerStoreStats;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 판매자 스토어 페이지 정보 DTO (수정됨)
 * Orders 도메인의 SellerStoreStats 사용
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

        @Schema(description = "평균 배송 소요일", example = "3.2")
        Double avgDeliveryDays,

        @Schema(description = "총 리뷰 수", example = "85")
        Long totalReviews

) {

    /**
     * Orders 도메인의 SellerStoreStats를 사용하여 생성
     */
    public static SellerStoreInfo from(Sellers seller, Long totalProducts, SellerStoreStats stats) {
        if (seller == null) {
            return null;
        }

        String operatingHours = formatOperatingHours(
                seller.getOperatingStartTime(),
                seller.getOperatingEndTime()
        );

        String operationStartYear = formatOperationStartYear(seller.getCreatedAt());

        // Orders 도메인의 SellerStoreStats 사용
        Long totalSalesCount = stats != null ? stats.totalSalesCount() : 0L;
        Double avgDeliveryDays = stats != null ? stats.avgDeliveryDays() : 0.0;
        Long totalReviews = stats != null ? stats.totalReviews() : 0L;

        return new SellerStoreInfo(
                seller.getUserId() != null ? seller.getUserId().toString() : null,
                seller.getVendorName(),
                seller.getVendorProfileImage(),
                seller.getTags(),
                operatingHours,
                operationStartYear,
                totalProducts,
                totalSalesCount,
                avgDeliveryDays,
                totalReviews
        );
    }

    /**
     * 기존 호환성을 위한 메서드 (통계 없이) - Deprecated
     */
    @Deprecated
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