package com.team5.catdogeats.products.service.impl;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.products.component.StockValidator;
import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.repository.StockReservationRepository;
import com.team5.catdogeats.products.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 재고 예약 서비스 구현체 (리팩토링)
 * - 재고 예약의 생성, 확정, 취소 등 상태 변경 로직에만 집중합니다.
 * - 재고 검증은 StockValidator에 위임합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationServiceImpl implements StockReservationService {

    private final StockReservationRepository stockReservationRepository;
    private final StockValidator stockValidator;

    @Value("${stock.reservation.expiration-minutes:30}")
    private int reservationExpirationMinutes;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(retryFor = {OptimisticLockingFailureException.class}, backoff = @Backoff(delay = 100))
    public List<StockReservation> createBulkReservations(Orders order, List<ReservationRequest> reservationRequests) {
        log.info("일괄 재고 예약 생성 시작: orderId={}, 상품 개수={}", order.getId(), reservationRequests.size());

        List<StockValidator.ProductValidation> validations = reservationRequests.stream()
                .map(request -> new StockValidator.ProductValidation(
                        request.product().getId(),
                        request.quantity()))
                .toList();
        stockValidator.validateMultipleStockAvailability(validations);

        List<StockReservation> reservations = new ArrayList<>();
        for (ReservationRequest request : reservationRequests) {
            StockReservation reservation = StockReservation.createReservation(
                    order, request.product(), request.quantity(), reservationExpirationMinutes);
            reservations.add(reservation);
        }

        List<StockReservation> savedReservations = stockReservationRepository.saveAll(reservations);
        log.info("일괄 재고 예약 생성 완료: orderId={}, 생성된 예약 개수={}",
                order.getId(), savedReservations.size());
        return savedReservations;
    }

    @Override
    @Transactional
    public List<StockReservation> confirmReservations(String orderId) {
        log.info("재고 예약 확정 시작: orderId={}", orderId);

        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);
        if (reservations.isEmpty()) {
            throw new NoSuchElementException("예약을 찾을 수 없습니다: orderId=" + orderId);
        }

        reservations.forEach(StockReservation::confirm);
        List<StockReservation> confirmedReservations = stockReservationRepository.saveAll(reservations);

        log.info("재고 예약 확정 완료: orderId={}, 확정된 예약 개수={}",
                orderId, confirmedReservations.size());
        return confirmedReservations;
    }

    @Override
    @Transactional
    public List<StockReservation> cancelReservations(String orderId) {
        log.info("재고 예약 취소 시작: orderId={}", orderId);

        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);
        if (reservations.isEmpty()) {
            log.warn("취소할 예약을 찾을 수 없습니다: orderId={}", orderId);
            return List.of();
        }

        reservations.stream()
                .filter(StockReservation::isActive)
                .forEach(StockReservation::cancel);

        List<StockReservation> cancelledReservations = stockReservationRepository.saveAll(reservations);
        log.info("재고 예약 취소 완료: orderId={}, 취소된 예약 개수={}",
                orderId, cancelledReservations.size());
        return cancelledReservations;
    }
}