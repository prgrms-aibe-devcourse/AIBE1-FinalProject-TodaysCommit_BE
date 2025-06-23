package com.team5.catdogeats.products.service;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.domain.enums.ReservationStatus;
import com.team5.catdogeats.products.dto.StockAvailabilityDto;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * 재고 예약 서비스 (타입 수정됨)
 * Products와 Orders 엔티티의 ID 타입이 String으로 변경됨에 따라 모든 관련 메서드를 수정하였습니다.
 * 안전한 재고 관리를 위한 예약 시스템의 핵심 비즈니스 로직을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final StockReservationRepository stockReservationRepository;
    private final ProductRepository productRepository;

    @Value("${stock.reservation.expiration-minutes:30}")
    private int reservationExpirationMinutes;

    // === 재고 예약 생성 (타입 수정) ===

    /**
     * 새로운 재고 예약 생성 (타입 수정)
     * @param order 주문 정보
     * @param product 상품 정보
     * @param quantity 예약할 수량
     * @return 생성된 재고 예약
     * @throws IllegalArgumentException 재고 부족 시
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(value = {OptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100))
    public StockReservation createReservation(Orders order, Products product, Integer quantity) {
        log.info("재고 예약 생성 시작: orderId={}, productId={}, quantity={}",
                order.getId(), product.getId(), quantity);

        // 1. 재고 가용성 검증 (String 타입)
        validateStockAvailability(product.getId(), quantity);

        // 2. 재고 예약 생성
        StockReservation reservation = StockReservation.createReservation(
                order, product, quantity, reservationExpirationMinutes);

        // 3. 예약 저장
        StockReservation savedReservation = stockReservationRepository.save(reservation);

        log.info("재고 예약 생성 완료: reservationId={}, orderId={}, productId={}, quantity={}, expiredAt={}",
                savedReservation.getId(), order.getId(), product.getId(),
                quantity, savedReservation.getExpiredAt());

        return savedReservation;
    }

    /**
     * 여러 상품에 대한 일괄 예약 생성 (타입 수정)
     * @param order 주문 정보
     * @param reservationRequests 예약 요청 목록
     * @return 생성된 예약 목록
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(value = {OptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100))
    public List<StockReservation> createBulkReservations(Orders order,
                                                         List<ReservationRequest> reservationRequests) {
        log.info("일괄 재고 예약 생성 시작: orderId={}, 상품 개수={}",
                order.getId(), reservationRequests.size());

        // 1. 모든 상품의 재고 가용성 사전 검증 (String 타입)
        for (ReservationRequest request : reservationRequests) {
            validateStockAvailability(request.getProduct().getId(), request.getQuantity());
        }

        // 2. 모든 검증이 통과한 경우에만 예약 생성
        List<StockReservation> reservations = reservationRequests.stream()
                .map(request -> StockReservation.createReservation(
                        order, request.getProduct(), request.getQuantity(), reservationExpirationMinutes))
                .toList();

        List<StockReservation> savedReservations = stockReservationRepository.saveAll(reservations);

        log.info("일괄 재고 예약 생성 완료: orderId={}, 생성된 예약 개수={}",
                order.getId(), savedReservations.size());

        return savedReservations;
    }

    // === 재고 가용성 검증 (타입 수정) ===

    /**
     * 재고 가용성 검증 (타입 수정: UUID → String)
     * @param productId 상품 ID (String 타입)
     * @param requestedQuantity 요청 수량
     * @throws IllegalArgumentException 재고 부족 시
     */
    public void validateStockAvailability(String productId, Integer requestedQuantity) {
        StockAvailabilityDto availability = getStockAvailability(productId);

        if (availability.getAvailableStock() < requestedQuantity) {
            throw new IllegalArgumentException(
                    String.format("재고가 부족합니다: 상품ID=%s, 요청수량=%d, 가용재고=%d, 실제재고=%d, 예약수량=%d",
                            productId, requestedQuantity, availability.getAvailableStock(),
                            availability.getActualStock(), availability.getReservedStock()));
        }

        log.debug("재고 가용성 검증 통과: productId={}, 요청수량={}, 가용재고={}",
                productId, requestedQuantity, availability.getAvailableStock());
    }

    /**
     * 상품의 재고 가용성 정보 조회 (타입 수정: UUID → String)
     * @param productId 상품 ID (String 타입)
     * @return 재고 가용성 정보
     */
    @Transactional(readOnly = true)
    public StockAvailabilityDto getStockAvailability(String productId) {
        // 실제 재고 조회
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다: " + productId));

        // 예약 수량 계산
        Integer reservedQuantity = stockReservationRepository.getTotalReservedQuantityByProductId(productId);
        Integer availableStock = product.getStock() - (reservedQuantity != null ? reservedQuantity : 0);

        return StockAvailabilityDto.builder()
                .productId(productId)
                .actualStock(product.getStock())
                .reservedStock(reservedQuantity != null ? reservedQuantity : 0)
                .availableStock(Math.max(0, availableStock)) // 음수 방지
                .build();
    }

    // === 예약 상태 관리 (타입 수정) ===

    /**
     * 재고 예약 확정 처리 (타입 수정: UUID → String)
     * @param orderId 주문 ID (String 타입)
     * @return 확정된 예약 목록
     */
    @Transactional
    public List<StockReservation> confirmReservations(String orderId) {
        log.info("재고 예약 확정 시작: orderId={}", orderId);

        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);

        if (reservations.isEmpty()) {
            throw new NoSuchElementException("예약을 찾을 수 없습니다: orderId=" + orderId);
        }

        // 모든 예약을 확정 상태로 변경
        reservations.forEach(StockReservation::confirm);
        List<StockReservation> confirmedReservations = stockReservationRepository.saveAll(reservations);

        log.info("재고 예약 확정 완료: orderId={}, 확정된 예약 개수={}",
                orderId, confirmedReservations.size());

        return confirmedReservations;
    }

    /**
     * 확정된 예약에 대한 실제 재고 차감 (타입 수정: UUID → String)
     * @param orderId 주문 ID (String 타입)
     * @return 차감 처리된 예약 목록
     */
    @Transactional
    public List<StockReservation> decrementConfirmedStock(String orderId) {
        log.info("확정된 재고 차감 시작: orderId={}", orderId);

        // 확정된 예약 목록 조회
        List<StockReservation> confirmedReservations = stockReservationRepository.findByOrderId(orderId)
                .stream()
                .filter(reservation -> reservation.getReservationStatus() == ReservationStatus.CONFIRMED)
                .toList();

        if (confirmedReservations.isEmpty()) {
            throw new IllegalStateException("확정된 예약을 찾을 수 없습니다: orderId=" + orderId);
        }

        // 각 예약에 대해 실제 재고 차감
        for (StockReservation reservation : confirmedReservations) {
            Products product = reservation.getProduct();
            Integer decrementQuantity = reservation.getReservedQuantity();

            // 재고 부족 검증 (확정된 예약이므로 정상적으로는 발생하지 않아야 함)
            if (product.getStock() < decrementQuantity) {
                log.error("재고 부족으로 차감 실패: productId={}, 현재재고={}, 차감요청={}",
                        product.getId(), product.getStock(), decrementQuantity);
                throw new IllegalStateException(
                        String.format("재고 부족: 상품ID=%s, 현재재고=%d, 차감요청=%d",
                                product.getId(), product.getStock(), decrementQuantity));
            }

            // 실제 재고 차감
            product.decreaseStock(decrementQuantity);
            productRepository.save(product);

            log.info("재고 차감 완료: productId={}, 차감수량={}, 남은재고={}",
                    product.getId(), decrementQuantity, product.getStock());
        }

        log.info("확정된 재고 차감 완료: orderId={}, 처리된 예약 개수={}",
                orderId, confirmedReservations.size());

        return confirmedReservations;
    }

    /**
     * 재고 예약 취소 처리 (타입 수정: UUID → String)
     * @param orderId 주문 ID (String 타입)
     * @return 취소된 예약 목록
     */
    @Transactional
    public List<StockReservation> cancelReservations(String orderId) {
        log.info("재고 예약 취소 시작: orderId={}", orderId);

        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);

        if (reservations.isEmpty()) {
            log.warn("취소할 예약을 찾을 수 없습니다: orderId={}", orderId);
            return List.of();
        }

        // 활성 상태인 예약만 취소 처리
        List<StockReservation> activeReservations = reservations.stream()
                .filter(StockReservation::isActive)
                .peek(StockReservation::cancel)
                .toList();

        List<StockReservation> cancelledReservations = stockReservationRepository.saveAll(activeReservations);

        log.info("재고 예약 취소 완료: orderId={}, 취소된 예약 개수={}",
                orderId, cancelledReservations.size());

        return cancelledReservations;
    }

    /**
     * 재고 예약 만료 처리
     * @param reservationId 예약 ID
     */
    @Transactional
    public void expireReservation(UUID reservationId) {
        log.info("재고 예약 만료 처리 시작: reservationId={}", reservationId);

        StockReservation reservation = stockReservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("예약을 찾을 수 없습니다: " + reservationId));

        if (reservation.isActive()) {
            reservation.expire();
            stockReservationRepository.save(reservation);

            log.info("재고 예약 만료 처리 완료: reservationId={}, orderId={}",
                    reservationId, reservation.getOrder().getId());
        } else {
            log.info("이미 처리된 예약입니다: reservationId={}, status={}",
                    reservationId, reservation.getReservationStatus());
        }
    }

    // === 배치 처리 메서드 ===

    /**
     * 만료된 예약들을 일괄 처리
     * @return 처리된 예약 개수
     */
    @Transactional
    public int processExpiredReservations() {
        ZonedDateTime currentTime = ZonedDateTime.now();
        int expiredCount = stockReservationRepository.bulkExpireReservations(currentTime);

        if (expiredCount > 0) {
            log.info("만료된 예약 일괄 처리 완료: 처리된 개수={}", expiredCount);
        }

        return expiredCount;
    }

    // === 조회 메서드 (타입 수정) ===

    /**
     * 주문의 활성 예약 목록 조회 (타입 수정: UUID → String)
     * @param orderId 주문 ID (String 타입)
     * @return 활성 예약 목록
     */
    @Transactional(readOnly = true)
    public List<StockReservation> getActiveReservationsByOrder(String orderId) {
        return stockReservationRepository.findByOrderId(orderId).stream()
                .filter(StockReservation::isActive)
                .toList();
    }

    /**
     * 상품의 활성 예약 목록 조회 (타입 수정: UUID → String)
     * @param productId 상품 ID (String 타입)
     * @return 활성 예약 목록
     */
    @Transactional(readOnly = true)
    public List<StockReservation> getActiveReservationsByProduct(String productId) {
        return stockReservationRepository.findActiveReservationsByProductId(productId);
    }

    // === 내부 DTO 클래스 ===

    /**
     * 예약 요청 정보를 담는 내부 클래스
     */
    public static class ReservationRequest {
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