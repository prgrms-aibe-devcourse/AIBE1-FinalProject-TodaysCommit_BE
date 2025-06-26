package com.team5.catdogeats.orders.dto.common;

/**
 * 기본 주문 아이템 정보 (Record 타입)
 * 이벤트 전송 및 기본적인 주문 아이템 정보를 담는 불변 객체입니다.
 * Lombok 어노테이션 대신 Java Record를 사용하여 보일러플레이트 코드를 제거했습니다.
 *
 * @param productId 상품 ID
 * @param productName 상품명
 * @param quantity 주문 수량
 * @param unitPrice 상품 단가 (할인 적용 가능)
 * @param totalPrice 총 가격 (단가 * 수량)
 */
public record OrderItemInfo(
        String productId,
        String productName,
        Integer quantity,
        Long unitPrice,
        Long totalPrice
) {

    // 기본 생성자 검증
    public OrderItemInfo {
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
     * 정적 팩토리 메서드 - 기본 정보로 생성
     */
    public static OrderItemInfo of(String productId, String productName, Integer quantity, Long unitPrice) {
        return new OrderItemInfo(productId, productName, quantity, unitPrice, unitPrice * quantity);
    }

    /**
     * 정적 팩토리 메서드 - 모든 정보 지정
     */
    public static OrderItemInfo of(String productId, String productName, Integer quantity, Long unitPrice, Long totalPrice) {
        return new OrderItemInfo(productId, productName, quantity, unitPrice, totalPrice);
    }
}