package com.team5.catdogeats.products.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 재고 가용성 정보 DTO (String ID 타입 적용)
 *
 * 상품의 실제 재고, 예약된 재고, 가용 재고 정보를 담는 데이터 전송 객체입니다.
 * 재고 예약 시스템에서 재고 상태를 확인하고 검증할 때 사용됩니다.
 *
 * 프로젝트의 모든 Entity ID가 String 타입이므로 이에 맞춰 수정되었습니다.
 */
@Getter
@Builder
public class StockAvailabilityDto {

    /**
     * 상품 ID (String 타입)
     */
    private final String productId;

    /**
     * 실제 재고 수량
     * Products 테이블의 stock 필드 값
     */
    private final Integer actualStock;

    /**
     * 예약된 재고 수량
     * 현재 RESERVED 상태인 모든 예약의 총 수량
     */
    private final Integer reservedStock;

    /**
     * 가용 재고 수량
     * 실제 재고에서 예약된 재고를 뺀 값 (actualStock - reservedStock)
     * 새로운 주문 시 실제로 예약 가능한 수량
     */
    private final Integer availableStock;

    /**
     * 재고 가용성 검증
     * 요청 수량이 가용 재고보다 작거나 같은지 확인합니다.
     *
     * @param requestedQuantity 요청 수량
     * @return 주문 가능 여부
     */
    public boolean canReserve(Integer requestedQuantity) {
        return availableStock >= requestedQuantity;
    }

    /**
     * 재고 부족 수량 계산
     * 요청 수량이 가용 재고를 초과하는 경우 부족한 수량을 반환합니다.
     *
     * @param requestedQuantity 요청 수량
     * @return 부족 수량 (음수면 충분함)
     */
    public Integer getShortageQuantity(Integer requestedQuantity) {
        return requestedQuantity - availableStock;
    }

    /**
     * 예약률 계산
     * 실제 재고 대비 예약된 재고의 비율을 백분율로 반환합니다.
     *
     * @return 예약률 (0.0 ~ 100.0)
     */
    public Double getReservationRate() {
        if (actualStock == null || actualStock == 0) {
            return 0.0;
        }
        return (reservedStock.doubleValue() / actualStock) * 100.0;
    }

    /**
     * 가용률 계산
     * 실제 재고 대비 가용 재고의 비율을 백분율로 반환합니다.
     *
     * @return 가용률 (0.0 ~ 100.0)
     */
    public Double getAvailabilityRate() {
        if (actualStock == null || actualStock == 0) {
            return 0.0;
        }
        return (availableStock.doubleValue() / actualStock) * 100.0;
    }

    /**
     * 재고 상태 문자열 반환
     * 관리자 화면이나 로그에서 사용할 수 있는 상태 정보를 반환합니다.
     *
     * @return 재고 상태 문자열
     */
    public String getStockStatusDescription() {
        if (availableStock <= 0) {
            return "재고 부족";
        } else if (availableStock <= 5) {
            return "재고 부족 임박";
        } else if (getReservationRate() >= 80.0) {
            return "높은 예약률";
        } else {
            return "재고 충분";
        }
    }

    @Override
    public String toString() {
        return String.format("StockAvailability{productId=%s, actual=%d, reserved=%d, available=%d, rate=%.1f%%}",
                productId, actualStock, reservedStock, availableStock, getReservationRate());
    }
}