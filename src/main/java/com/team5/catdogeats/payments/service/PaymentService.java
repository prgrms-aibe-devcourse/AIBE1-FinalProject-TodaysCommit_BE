package com.team5.catdogeats.payments.service;

import com.team5.catdogeats.global.config.JpaTransactional;
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

/**
 * 결제 처리 서비스 (타입 수정됨)
 * Orders 엔티티의 ID 타입이 String으로 변경됨에 따라 관련 메서드들을 수정하였습니다.
 * Toss Payments API와 연동하여 결제 승인, 검증 및 상태 관리를 담당합니다.
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
     * 결제 승인 처리 (타입 수정)
     * @param paymentKey Toss Payments 결제 키
     * @param orderId 주문 ID (String 타입)
     * @param amount 결제 금액
     * @return 결제 승인 결과
     */
    @Transactional(transactionManager = "jpaTransactionManager")
    public PaymentConfirmResponse confirmPayment(String paymentKey, String orderId, Long amount) {
        log.info("결제 승인 처리 시작: paymentKey={}, orderId={}, amount={}",
                paymentKey, orderId, amount);

        // 1. 주문 정보 조회 및 검증 (String 타입으로 직접 사용)
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

        // 2. 결제 정보 조회 (String 타입으로 직접 사용)
        Payments payment = paymentRepository.findByOrdersId(orderId)
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

        // 9. 재고 확정 및 차감 (String 타입으로 직접 사용)
        confirmAndDecrementStock(orderId);

        log.info("결제 승인 완료: orderId={}, paymentId={}, tossPaymentKey={}",
                orderId, payment.getId(), tossResponse.getPaymentKey());

        return PaymentConfirmResponse.builder()
                .paymentId(payment.getId())
                .orderId(orderId)
                .orderNumber(order.getOrderNumber())
                .amount(amount)
                .status(PaymentStatus.SUCCESS)
                .paidAt(ZonedDateTime.now())
                .tossPaymentKey(tossResponse.getPaymentKey())
                .build();
    }

    /**
     * 결제 실패 처리 (타입 수정)
     * @param orderId 주문 ID (String 타입)
     * @param code 실패 코드
     * @param message 실패 메시지
     */
    @Transactional(transactionManager = "jpaTransactionManager")
    public void handlePaymentFailure(String orderId, String code, String message) {
        log.info("결제 실패 처리 시작: orderId={}, code={}, message={}", orderId, code, message);

        try {
            // 주문 정보 조회 (String 타입으로 직접 사용)
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            // 주문 상태를 취소로 변경
            updateOrderStatus(order, OrderStatus.CANCELLED);

            // 결제 정보가 있다면 실패 상태로 업데이트 (String 타입으로 직접 사용)
            paymentRepository.findByOrdersId(orderId)
                    .ifPresent(payment -> {
                        payment.setStatus(PaymentStatus.FAILED);
                        paymentRepository.save(payment);
                        log.info("결제 정보 실패 상태 업데이트: paymentId={}", payment.getId());
                    });

            // 재고 예약 취소 (String 타입으로 직접 사용)
            stockReservationService.cancelReservations(orderId);

            log.info("결제 실패 처리 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    // === 내부 검증 메서드들 ===

    private void validatePaymentStatus(Payments payment, Orders order) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("결제가 이미 처리되었습니다: " + payment.getStatus());
        }

        if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("주문 상태가 결제 대기가 아닙니다: " + order.getOrderStatus());
        }
    }

    private void validatePaymentAmount(Orders order, Long amount) {
        if (!order.getTotalPrice().equals(amount)) {
            throw new IllegalArgumentException(
                    String.format("결제 금액이 일치하지 않습니다: 주문금액=%d, 결제금액=%d",
                            order.getTotalPrice(), amount));
        }
    }

    private TossPaymentConfirmResponse callTossPaymentConfirm(String paymentKey, String orderId, Long amount) {
        TossPaymentConfirmRequest request = TossPaymentConfirmRequest.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(amount)
                .build();

        return tossPaymentsClient.confirmPayment(request);
    }

    private void validateTossResponse(TossPaymentConfirmResponse tossResponse, Orders order, Long amount) {
        if (!tossResponse.getOrderId().equals(order.getId())) {
            throw new IllegalArgumentException("주문 ID가 일치하지 않습니다");
        }

        if (!tossResponse.getTotalAmount().equals(amount)) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다");
        }
    }

    private void updateOrderStatus(Orders order, OrderStatus status) {
        order.setOrderStatus(status);
        orderRepository.save(order);
        log.info("주문 상태 업데이트: orderId={}, status={}", order.getId(), status);
    }

    private void updatePaymentStatus(Payments payment, TossPaymentConfirmResponse tossResponse) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTossPaymentKey(tossResponse.getPaymentKey());
        paymentRepository.save(payment);
        log.info("결제 정보 업데이트: paymentId={}", payment.getId());
    }

    private void confirmAndDecrementStock(String orderId) {
        // 재고 예약 확정 (String 타입으로 직접 사용)
        stockReservationService.confirmReservations(orderId);

        // 확정된 재고 차감 (String 타입으로 직접 사용)
        stockReservationService.decrementConfirmedStock(orderId);

        log.info("재고 확정 및 차감 완료: orderId={}", orderId);
    }
}