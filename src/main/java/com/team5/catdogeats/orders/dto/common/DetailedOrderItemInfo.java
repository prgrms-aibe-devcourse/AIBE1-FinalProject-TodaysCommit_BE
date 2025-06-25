package com.team5.catdogeats.orders.dto.common;

/**
 * 상세 주문 아이템 정보 (Record 타입) - 쿠폰 할인 방식 적용
 * 주문 처리 중 상세 정보가 필요한 경우 사용하는 불변 객체입니다.
 * 상품별 할인을 제거하고 원가 기준으로 단순화했습니다.
 *
 * @param productId 상품 ID
 * @param productName 상품명
 * @param quantity 주문 수량
 * @param unitPrice 상품 단가 (원가)
 * @param totalPrice 상품 총 가격 (단가 * 수량)
 */
public record DetailedOrderItemInfo(
        String productId,
        String productName,
        Integer quantity,
        Long unitPrice,
        Long totalPrice
) {

    // 기본 생성자 검증
    public DetailedOrderItemInfo {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("상품 ID는 필수입니다");
        }
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수입니다");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다");
        }
        if (unitPrice == null || unitPrice < 0) {
            throw new IllegalArgumentException("단가는 0 이상이어야 합니다");
        }
        if (totalPrice == null || totalPrice < 0) {
            throw new IllegalArgumentException("총 가격은 0 이상이어야 합니다");
        }
    }

    /**
     * 정적 팩토리 메서드 - 기본 생성
     * 상품 정보를 기반으로 DetailedOrderItemInfo를 생성합니다.
     *
     * @param productId 상품 ID
     * @param productName 상품명
     * @param quantity 주문 수량
     * @param unitPrice 상품 단가
     * @return DetailedOrderItemInfo 인스턴스
     */
    public static DetailedOrderItemInfo of(String productId, String productName, Integer quantity, Long unitPrice) {
        Long totalPrice = unitPrice * quantity;
        return new DetailedOrderItemInfo(productId, productName, quantity, unitPrice, totalPrice);
    }

    /**
     * 기본 OrderItemInfo로 변환
     * 이벤트 발행 시 사용되는 메서드입니다.
     */
    public OrderItemInfo toOrderItemInfo() {
        return OrderItemInfo.of(productId, productName, quantity, unitPrice, totalPrice);
    }
}