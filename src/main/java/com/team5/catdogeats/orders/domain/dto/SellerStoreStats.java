package com.team5.catdogeats.orders.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 구매자에게 보여줄 판매자 상점 정보 (총 판매량, 평균 배송소요일 , 총 리뷰 수 )
 */
@Schema(description = "판매자 상점 집계 정보")
public record SellerStoreStats(
        @Schema(description = "총 판매량", example = "1250")
        Long totalSalesCount,

        @Schema(description = "평균 배송 소요일", example = "3.5")
        Double avgDeliveryDays,

        @Schema(description = "총 리뷰 수", example = "85")
        Long totalReviews
) {

    /**
     * 기본값으로 SellerStoreStats 생성
     */
    public static SellerStoreStats empty() {
        return new SellerStoreStats(0L, 0.0, 0L);
    }
}