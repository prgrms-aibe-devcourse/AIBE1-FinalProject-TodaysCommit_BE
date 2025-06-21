package com.team5.catdogeats.users.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 판매자 스토어 통계 정보 DTO
 */
@Schema(description = "판매자 스토어 통계 정보")
public record SellerStoreStats(
        @Schema(description = "전체 상품 판매량", example = "1250")
        Long totalSalesQuantity,

        @Schema(description = "총 배송 완료 건수", example = "450")
        Long totalDeliveries,

        @Schema(description = "평균 배송 소요일", example = "3.5")
        Double avgDeliveryDays,

        @Schema(description = "최소 배송 소요일", example = "1.0")
        Double minDeliveryDays,

        @Schema(description = "최대 배송 소요일", example = "7.0")
        Double maxDeliveryDays
) {

    /**
     * 배송 정보 텍스트 생성
     */
    public String getDeliveryInfoText() {
        if (totalDeliveries == 0 || avgDeliveryDays == 0.0) {
            return "배송 정보: 데이터 부족";
        }

        // 평균 배송일을 범위로 표현
        int minDays = (int) Math.max(1, Math.floor(avgDeliveryDays - 1));
        int maxDays = (int) Math.ceil(avgDeliveryDays + 1);

        return String.format("배송 정보: 평균 %d~%d일 소요", minDays, maxDays);
    }

    /**
     * 판매량 정보 텍스트 생성
     */
    public String getSalesInfoText() {
        if (totalSalesQuantity == 0) {
            return "총 판매량: 판매 실적 없음";
        }

        if (totalSalesQuantity >= 10000) {
            return String.format("총 판매량: %.1fK개", totalSalesQuantity / 1000.0);
        } else if (totalSalesQuantity >= 1000) {
            return String.format("총 판매량: %.1fK개", totalSalesQuantity / 1000.0);
        } else {
            return String.format("총 판매량: %d개", totalSalesQuantity);
        }
    }
}