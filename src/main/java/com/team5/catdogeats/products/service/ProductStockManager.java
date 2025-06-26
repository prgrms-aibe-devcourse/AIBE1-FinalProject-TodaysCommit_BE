package com.team5.catdogeats.products.service;

/**
 * 상품 재고 관리자 인터페이스
 * - 실제 Products 엔티티의 재고(stock)를 직접 변경(차감, 복원)하는 책임을 가집니다.
 */
public interface ProductStockManager {

    /**
     * 확정된 예약에 대한 실제 재고 차감
     * @param orderId 주문 ID
     */
    void decrementStockForConfirmedReservations(String orderId);
}