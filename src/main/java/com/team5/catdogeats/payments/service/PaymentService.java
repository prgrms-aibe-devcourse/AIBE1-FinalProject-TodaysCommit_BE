package com.team5.catdogeats.payments.service;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.payments.client.TossPaymentsClient;
import com.team5.catdogeats.payments.domain.Payments;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.payments.dto.request.TossPaymentConfirmRequest;
import com.team5.catdogeats.payments.dto.response.PaymentConfirmResponse;
import com.team5.catdogeats.payments.dto.response.TossPaymentConfirmResponse;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.products.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * 결제 처리 서비스
 *
 * Toss Payments API와 연동하여 결제 승인, 검증 및 상태 관리를 담당합니다.
 * 주요 기능:
 * - 결제 승인 및 검증
 * - 재고 확정 및 차감
 * - 주문 상태 업데이트
 * - 결제 실패 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final StockReservationService stockReservationService;
    private final TossPaymentsClient tossPaymentsClient;

    /**
     * 결제 승인 처리
     *
     * Toss Payments 콜백으로부터 받은 결제 정보를 검증하고 최종 승인합니다.
     * 처리 순서:
     * 1. Toss Payments API 결제 승인 요청
     * 2. 결제 금액 검증
     * 3. 주문 상태 업데이트
     * 4. 결제 정보 업데이트
     * 5. 재고 확정 및 차감
     *
     * @param paymentKey Toss Payments 결제 키
     * @param orderId 주문 ID
     * @param amount 결제 금액
     * @return 결제 승인 결과
     */
    @Transactional
    public PaymentConfirmResponse confirmPayment(String paymentKey, String orderId, Long amount) {
        log.info("결제 승인 처리 시작: paymentKey={}, orderId={}, amount={}",
                paymentKey, orderId, amount);

        // 1. 주문 정보 조회 및 검증
        UUID orderUuid = UUID.fromString(orderId);
        Orders order = orderRepository.findById(orderUuid)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

        // 2. 결제 정보 조회
        Payments payment = paymentRepository.findByOrdersId(orderUuid)
                .orElseThrow(() -> new NoSuchElementException("결제 정보를 찾을 수 없습니다: " + orderId));

        // 3. 결제 상태 검증
        validatePaymentStatus(payment, order);

        // 4. 결제 금액 검증 (보안 중요)
        validatePaymentAmount(order, amount);

        // 5. Toss Payments API 결제 승인 요청
        TossPaymentConfirmResponse tossResponse = callTossPaymentConfirm(paymentKey, orderId, amount);

        // 6. Toss 응답 검증
        validateTossResponse(tossResponse, order, amount);

        // 7. 주문 상태 업데이트
        updateOrderStatus(order, OrderStatus.PAYMENT_COMPLETED);

        // 8. 결제 정보 업데이트
        updatePaymentStatus(payment, tossResponse);

        // 9. 재고 확정 및 차감
        confirmAndDecrementStock(orderUuid);

        log.info("결제 승인 완료: orderId={}, paymentId={}, tossPaymentKey={}",
                orderId, payment.getId(), tossResponse.getPaymentKey());

        return PaymentConfirmResponse.builder()
                .paymentId(payment.getId().toString())
                .orderId(orderId)
                .orderNumber(order.getOrderNumber())
                .amount(amount)
                .status(PaymentStatus.SUCCESS)
                .paidAt(ZonedDateTime.now())
                .tossPaymentKey(tossResponse.getPaymentKey())
                .build();
    }

    /**
     * 결제 실패 처리
     *
     * 결제 실패 시 관련 상태를 업데이트하고 재고 예약을 취소합니다.
     *
     * @param orderId 주문 ID
     * @param code 실패 코드
     * @param message 실패 메시지
     */
    @Transactional
    public void handlePaymentFailure(String orderId, String code, String message) {
        log.info("결제 실패 처리 시작: orderId={}, code={}, message={}", orderId, code, message);

        try {
            UUID orderUuid = UUID.fromString(orderId);

            // 주문 정보 조회
            Orders order = orderRepository.findById(orderUuid)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            // 주문 상태를 취소로 변경
            updateOrderStatus(order, OrderStatus.CANCELLED);

            // 결제 정보가 있다면 실패 상태로 업데이트
            paymentRepository.findByOrdersId(orderUuid)
                    .ifPresent(payment -> {
                        payment.setStatus(PaymentStatus.FAILED);
                        paymentRepository.save(payment);
                        log.info("결제 정보 실패 상태 업데이트: paymentId={}", payment.getId());
                    });

            // 재고 예약 취소
            stockReservationService.cancelReservations(orderUuid);

            log.info("결제 실패 처리 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류: orderId={}, error={}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    // === 내부 메서드 ===

    /**
     * 결제 상태 검증
     */
    private void validatePaymentStatus(Payments payment, Orders order) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException(
                    String.format("결제 승인 불가: 현재 결제 상태=%s, 주문ID=%s",
                            payment.getStatus(), order.getId()));
        }

        if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalArgumentException(
                    String.format("결제 승인 불가: 현재 주문 상태=%s, 주문ID=%s",
                            order.getOrderStatus(), order.getId()));
        }
    }

    /**
     * 결제 금액 검증 (보안 필수)
     */
    private void validatePaymentAmount(Orders order, Long requestedAmount) {
        if (!order.getTotalPrice().equals(requestedAmount)) {
            throw new IllegalArgumentException(
                    String.format("결제 금액 불일치: 주문금액=%d, 요청금액=%d, 주문ID=%s",
                            order.getTotalPrice(), requestedAmount, order.getId()));
        }
    }

    /**
     * Toss Payments API 결제 승인 호출
     */
    private TossPaymentConfirmResponse callTossPaymentConfirm(String paymentKey, String orderId, Long amount) {
        try {
            TossPaymentConfirmRequest request = TossPaymentConfirmRequest.builder()
                    .paymentKey(paymentKey)
                    .orderId(orderId)
                    .amount(amount)
                    .build();

            return tossPaymentsClient.confirmPayment(request);

        } catch (Exception e) {
            log.error("Toss Payments API 호출 실패: paymentKey={}, orderId={}, error={}",
                    paymentKey, orderId, e.getMessage(), e);
            throw new RuntimeException("결제 승인 API 호출에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * Toss 응답 검증
     */
    private void validateTossResponse(TossPaymentConfirmResponse tossResponse, Orders order, Long amount) {
        if (tossResponse == null) {
            throw new RuntimeException("Toss Payments 응답이 null입니다");
        }

        if (!order.getId().toString().equals(tossResponse.getOrderId())) {
            throw new IllegalArgumentException("주문 ID 불일치: " + tossResponse.getOrderId());
        }

        if (!amount.equals(tossResponse.getTotalAmount())) {
            throw new IllegalArgumentException(
                    String.format("결제 금액 불일치: 요청=%d, 응답=%d", amount, tossResponse.getTotalAmount()));
        }

        if (!"DONE".equals(tossResponse.getStatus())) {
            throw new RuntimeException("결제가 완료되지 않았습니다: " + tossResponse.getStatus());
        }
    }

    /**
     * 주문 상태 업데이트
     */
    private void updateOrderStatus(Orders order, OrderStatus newStatus) {
        order.setOrderStatus(newStatus);
        orderRepository.save(order);
        log.info("주문 상태 업데이트: orderId={}, status={}", order.getId(), newStatus);
    }

    /**
     * 결제 정보 업데이트
     */
    private void updatePaymentStatus(Payments payment, TossPaymentConfirmResponse tossResponse) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTossPaymentKey(tossResponse.getPaymentKey());
        payment.setPaidAt(ZonedDateTime.now());
        paymentRepository.save(payment);
        log.info("결제 정보 업데이트: paymentId={}, tossPaymentKey={}",
                payment.getId(), tossResponse.getPaymentKey());
    }

    /**
     * 재고 확정 및 차감
     */
    private void confirmAndDecrementStock(UUID orderId) {
        try {
            // 1. 재고 예약 확정
            stockReservationService.confirmReservations(orderId);

            // 2. 실제 재고 차감
            stockReservationService.decrementConfirmedStock(orderId);

            log.info("재고 확정 및 차감 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("재고 확정 및 차감 실패: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new RuntimeException("재고 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}