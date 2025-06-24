package com.team5.catdogeats.orders.dto.common;

/**
 * 할인 정보 포함 상세 주문 아이템 정보 (Record 타입)
 * 주문 처리 중 할인 계산 및 상세 정보가 필요한 경우 사용하는 불변 객체입니다.
 * Record 타입으로 구현하여 불변성과 타입 안정성을 보장합니다.
 *
 * @param productId 상품 ID
 * @param productName 상품명
 * @param quantity 주문 수량
 * @param originalPrice 원가 (할인 전 가격)
 * @param unitPrice 할인 적용된 단가
 * @param totalPrice 할인 적용된 총 가격
 * @param isDiscounted 할인 적용 여부
 * @param discountRate 할인율 (%)
 */
public record DetailedOrderItemInfo(
        String productId,
        String productName,
        Integer quantity,
        Long originalPrice,
        Long unitPrice,
        Long totalPrice,
        Boolean isDiscounted,
        Double discountRate
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
        if (originalPrice == null || originalPrice < 0) {
            throw new IllegalArgumentException("원가는 0 이상이어야 합니다");
        }
        if (unitPrice == null || unitPrice < 0) {
            throw new IllegalArgumentException("단가는 0 이상이어야 합니다");
        }
        if (totalPrice == null || totalPrice < 0) {
            throw new IllegalArgumentException("총 가격은 0 이상이어야 합니다");
        }
        if (isDiscounted == null) {
            throw new IllegalArgumentException("할인 적용 여부는 필수입니다");
        }
        if (isDiscounted && (discountRate == null || discountRate < 0 || discountRate > 100)) {
            throw new IllegalArgumentException("할인 적용 시 할인율은 0~100 사이여야 합니다");
        }
    }

    /**
     * 정적 팩토리 메서드 - 할인 없는 상품용
     */
    public static DetailedOrderItemInfo withoutDiscount(String productId, String productName, Integer quantity, Long originalPrice) {
        Long totalPrice = originalPrice * quantity;
        return new DetailedOrderItemInfo(productId, productName, quantity, originalPrice, originalPrice, totalPrice, false, 0.0);
    }

    /**
     * 정적 팩토리 메서드 - 할인 적용 상품용
     */
    public static DetailedOrderItemInfo withDiscount(String productId, String productName, Integer quantity,
                                                     Long originalPrice, Long discountedUnitPrice, Double discountRate) {
        Long totalPrice = discountedUnitPrice * quantity;
        return new DetailedOrderItemInfo(productId, productName, quantity, originalPrice, discountedUnitPrice, totalPrice, true, discountRate);
    }

    /**
     * 기본 OrderItemInfo로 변환
     * 이벤트 발행 시 사용되는 메서드입니다.
     */
    public OrderItemInfo toOrderItemInfo() {
        return OrderItemInfo.of(productId, productName, quantity, unitPrice, totalPrice);
    }
}