package com.team5.catdogeats.payments.service;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.payments.client.TossPaymentsClient;
import com.team5.catdogeats.payments.domain.Payments;
import com.team5.catdogeats.payments.domain.enums.PaymentMethod;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.payments.dto.request.TossPaymentConfirmRequest;
import com.team5.catdogeats.payments.dto.response.PaymentConfirmResponse;
import com.team5.catdogeats.payments.dto.response.TossPaymentConfirmResponse;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.products.service.StockReservationService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

/**
 * 결제 서비스 단위 테스트 – 컴파일 오류 수정 버전
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("결제 서비스 테스트")
class PaymentServiceTest {

    @InjectMocks private PaymentService paymentService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private StockReservationService stockReservationService;
    @Mock private TossPaymentsClient tossPaymentsClient;

    private Orders testOrder;
    private Payments testPayment;
    private Users testUser;
    private Buyers testBuyer;
    private TossPaymentConfirmResponse mockTossResponse;

    @BeforeEach
    void setUp() {
        /* 사용자 */
        testUser = Users.builder()
                .id("user123")
                .name("김철수")
                .role(Role.ROLE_BUYER)
                .provider("google")
                .providerId("google123")
                .build();

        /* 구매자 */
        testBuyer = Buyers.builder().user(testUser).build();

        /* 주문 */
        testOrder = Orders.builder()
                .id("order123")
                .orderNumber(1001L)
                .user(testUser)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(50_000L)
                .build();

        /* 결제 */
        testPayment = Payments.builder()
                .id("payment123")
                .orders(testOrder)
                .buyers(testBuyer)
                .method(PaymentMethod.TOSS)
                .status(PaymentStatus.PENDING)
                .tossPaymentKey("test_payment_key_123")
                .build();

        /* 토스 결제 승인 응답 – 실제 생성자 시그니처에 맞춤 */
        mockTossResponse = new TossPaymentConfirmResponse(
                "v1",                              // version
                "confirmed_payment_key_123",       // paymentKey
                "DONE",                            // status
                "order123",                        // orderId
                "강아지 사료 외 1건",               // orderName
                ZonedDateTime.now(),                // approvedAt
                50_000L,                            // totalAmount
                50_000L,                            // balanceAmount
                0L,                                 // suppliedAmount
                0L,                                 // vat
                "KRW",                             // currency
                null                                // cardInfo (mock null)
        );
    }

    // ─────────────────────────────────────────── 결제 성공 처리
    @Nested
    @DisplayName("결제 성공 콜백")
    class PaymentSuccess {
        @Test
        @DisplayName("✅ 결제 승인 – 전체 플로우")
        void confirmPayment_success() {
            String paymentKey = "test_payment_key_123";
            String orderId = "order123";
            Long amount = 50_000L;

            given(paymentRepository.findByTossPaymentKey(paymentKey)).willReturn(Optional.of(testPayment));
            given(orderRepository.findById(orderId)).willReturn(Optional.of(testOrder));
            given(tossPaymentsClient.confirmPayment(any(TossPaymentConfirmRequest.class))).willReturn(mockTossResponse);
            given(paymentRepository.save(testPayment)).willReturn(testPayment);
            given(orderRepository.save(testOrder)).willReturn(testOrder);

            PaymentConfirmResponse result = paymentService.confirmPayment(paymentKey, orderId, amount);

            assertThat(result).isNotNull();
            assertThat(result.getPaymentId()).isEqualTo("payment123");
            assertThat(result.getOrderId()).isEqualTo("order123");
            assertThat(result.getAmount()).isEqualTo(50_000L);
            assertThat(result.getStatus()).isEqualTo("SUCCESS");

            verify(stockReservationService).confirmReservations(orderId);
            verify(stockReservationService).decrementConfirmedStock(orderId);
            verify(tossPaymentsClient).confirmPayment(any(TossPaymentConfirmRequest.class));
        }

        @Test
        @DisplayName("❌ 토스 응답 검증 실패 – 주문 ID 불일치")
        void confirmPayment_orderIdMismatch() {
            String paymentKey = "test_payment_key_123";
            String orderId = "order123";
            Long amount = 50_000L;

            TossPaymentConfirmResponse invalid = new TossPaymentConfirmResponse(
                    "v1", "confirmed_payment_key_123", "DONE", "different_order_id", "강아지 사료 외 1건",
                    ZonedDateTime.now(), 50_000L, 50_000L, 0L, 0L, "KRW", null);

            given(paymentRepository.findByTossPaymentKey(paymentKey)).willReturn(Optional.of(testPayment));
            given(orderRepository.findById(orderId)).willReturn(Optional.of(testOrder));
            given(tossPaymentsClient.confirmPayment(any(TossPaymentConfirmRequest.class))).willReturn(invalid);

            assertThatThrownBy(() -> paymentService.confirmPayment(paymentKey, orderId, amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문 ID가 일치하지 않습니다");

            verify(stockReservationService, never()).confirmReservations(anyString());
        }
    }

    // ─────────────────────────────────────────── 결제 실패 처리
    @Nested
    @DisplayName("결제 실패 콜백")
    class PaymentFail {
        @Test
        @DisplayName("✅ 결제 실패 처리 – 정상 플로우")
        void handlePaymentFailure_success() {
            String orderId = "order123";
            String errorCode = "INSUFFICIENT_BALANCE";
            String errorMsg = "잔액이 부족합니다";

            given(orderRepository.findById(orderId)).willReturn(Optional.of(testOrder));
            given(paymentRepository.findByOrdersId(orderId)).willReturn(Optional.of(testPayment));

            paymentService.handlePaymentFailure(orderId, errorCode, errorMsg);

            verify(orderRepository).save(testOrder);
            verify(paymentRepository).save(testPayment);
            verify(stockReservationService).cancelReservations(orderId);
        }

        @Test
        @DisplayName("❌ 주문 없음 – 로직 스킵")
        void handlePaymentFailure_orderNotFound() {
            String orderId = "missing";
            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            paymentService.handlePaymentFailure(orderId, "ERR", "msg");

            verify(paymentRepository, never()).findByOrdersId(anyString());
            verify(stockReservationService, never()).cancelReservations(anyString());
        }

        @Test
        @DisplayName("✅ 결제 정보 없음 – 주문 취소만 진행")
        void handlePaymentFailure_paymentNotFound() {
            String orderId = "order123";
            given(orderRepository.findById(orderId)).willReturn(Optional.of(testOrder));
            given(paymentRepository.findByOrdersId(orderId)).willReturn(Optional.empty());

            paymentService.handlePaymentFailure(orderId, "USER_CANCEL", "msg");

            verify(orderRepository).save(testOrder);
            verify(stockReservationService).cancelReservations(orderId);
        }

        @Test
        @DisplayName("✅ 재고 취소 실패해도 처리 지속")
        void handlePaymentFailure_stockCancelFail() {
            String orderId = "order123";
            given(orderRepository.findById(orderId)).willReturn(Optional.of(testOrder));
            given(paymentRepository.findByOrdersId(orderId)).willReturn(Optional.of(testPayment));
            willThrow(new RuntimeException("재고 취소 실패")).given(stockReservationService).cancelReservations(orderId);

            paymentService.handlePaymentFailure(orderId, "CARD_EXPIRED", "msg");

            verify(orderRepository).save(testOrder);
            verify(paymentRepository).save(testPayment);
        }
    }

    // ─────────────────────────────────────────── 토스 API 파라미터 검증
    @Nested
    @DisplayName("토스 API 연동")
    class TossIntegration {
        @Test
        void tossApiCall_paramCheck() {
            String paymentKey = "test_payment_key_123";
            String orderId = "order123";
            Long amount = 50_000L;

            given(paymentRepository.findByTossPaymentKey(paymentKey)).willReturn(Optional.of(testPayment));
            given(orderRepository.findById(orderId)).willReturn(Optional.of(testOrder));
            given(tossPaymentsClient.confirmPayment(any(TossPaymentConfirmRequest.class))).willReturn(mockTossResponse);

            paymentService.confirmPayment(paymentKey, orderId, amount);

            verify(tossPaymentsClient).confirmPayment(argThat(req ->
                    req.getPaymentKey().equals(paymentKey) &&
                            req.getOrderId().equals(orderId) &&
                            req.getAmount().equals(amount)));
        }
    }
}
