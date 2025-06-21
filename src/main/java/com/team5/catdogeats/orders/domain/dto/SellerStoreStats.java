package com.team5.catdogeats.orders.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 구매자에게 보여줄 판매자 상점 집계 정보
 * (복잡한 통계가 아닌 신뢰도 표시용 단순 집계)
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
     * 구매자에게 보여줄 판매량 텍스트
     */
    public String getSalesDisplayText() {
        if (totalSalesCount == null || totalSalesCount == 0) {
            return "판매 실적 없음";
        }

        if (totalSalesCount >= 1000) {
            return String.format("%.1fK개 판매", totalSalesCount / 1000.0);
        }
        return String.format("%d개 판매", totalSalesCount);
    }

    /**
     * 구매자에게 보여줄 배송 정보 텍스트
     */
    public String getDeliveryDisplayText() {
        if (avgDeliveryDays == null || avgDeliveryDays <= 0) {
            return "배송 정보 없음";
        }

        int days = (int) Math.round(avgDeliveryDays);
        return String.format("평균 %d일 배송", days);
    }

    /**
     * 기본값으로 SellerStoreStats 생성
     */
    public static SellerStoreStats empty() {
        return new SellerStoreStats(0L, 0.0, 0L);
    }
}