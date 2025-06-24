package com.team5.catdogeats.products.service;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.dto.StockAvailabilityDto;

import java.util.List;

// 재고 예약 서비스 인터페이스
// 안전한 재고 관리를 위한 예약 시스템의 핵심 비즈니스 로직을 담당합니다.
// 프로젝트 컨벤션에 따라 인터페이스 + 구현체 패턴을 적용했습니다.
public interface StockReservationService {

    // === 재고 예약 생성 ===

    // 새로운 재고 예약 생성
    StockReservation createReservation(Orders order, Products product, Integer quantity);

    // 일괄 재고 예약 생성
    List<StockReservation> createBulkReservations(Orders order, List<ReservationRequest> reservationRequests);

    // === 재고 가용성 검증 ===

    // 재고 가용성 검증
    void validateStockAvailability(String productId, Integer requestedQuantity);

    // 재고 가용성 조회
    StockAvailabilityDto getStockAvailability(String productId);

    // === 예약 상태 관리 ===

    // 재고 예약 확정 처리
    List<StockReservation> confirmReservations(String orderId);

    // 확정된 예약에 대한 실제 재고 차감
    List<StockReservation> decrementConfirmedStock(String orderId);

    // 재고 예약 취소 처리
    List<StockReservation> cancelReservations(String orderId);

    // === 만료 처리 ===

    // 만료된 예약 처리
    int processExpiredReservations();

    // === 조회 메서드 ===

    // 주문의 활성 예약 목록 조회
    List<StockReservation> getActiveReservationsByOrder(String orderId);

    // 상품의 활성 예약 목록 조회
    List<StockReservation> getActiveReservationsByProduct(String productId);

    // === 내부 DTO 클래스 ===

    // 예약 요청 정보를 담는 내부 클래스
    class ReservationRequest {
        private final Products product;
        private final Integer quantity;

        public ReservationRequest(Products product, Integer quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        public Products getProduct() { return product; }
        public Integer getQuantity() { return quantity; }
    }
}