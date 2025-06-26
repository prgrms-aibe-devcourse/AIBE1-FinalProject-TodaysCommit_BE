package com.team5.catdogeats.products.service;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.StockReservation;
import lombok.Builder;

import java.util.List;

/**
 * 재고 예약 서비스 인터페이스 (리팩토링)
 * - 재고 예약의 생성 및 상태 변경(확정, 취소)에 대한 책임만 가집니다.
 * - 실제 재고 차감 및 만료 처리는 다른 서비스로 책임이 분리되었습니다.
 */
public interface StockReservationService {

    /**
     * 일괄 재고 예약 생성
     */
    List<StockReservation> createBulkReservations(Orders order, List<ReservationRequest> reservationRequests);

    /**
     * 주문에 대한 재고 예약 확정 처리
     */
    List<StockReservation> confirmReservations(String orderId);

    /**
     * 주문에 대한 재고 예약 취소 처리
     */
    List<StockReservation> cancelReservations(String orderId);

    /**
     * 예약 요청 정보를 담는 내부 DTO
     */
    @Builder
    record ReservationRequest(Products product, Integer quantity) {
    }
}