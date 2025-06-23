package com.team5.catdogeats.orders.event.listener;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.payments.domain.Payments;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.service.StockReservationService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import static org.mockito.Mockito.times;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 주문 이벤트 리스너 테스트
 * OrderEventListener의 모든 이벤트 처리 메서드를 검증합니다.
 * EDA 아키텍처의 핵심인 이벤트 기반 처리 로직의 정확성을 보장합니다.
 * 테스트 범위:
 * 1. 재고 예약 처리 (handleStockReservation)
 * 2. 결제 정보 생성 (handlePaymentInfoCreation)
 * 3. 사용자 알림 처리 (handleUserNotification)
 * 4. 감사 로깅 (handleOrderProcessingComplete)
 * 5. 보상 트랜잭션 (재고 예약 실패 시)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("주문 이벤트 리스너 테스트")
class OrderEventListenerTest {

    @InjectMocks
    private OrderEventListener orderEventListener;

    @Mock
    private StockReservationService stockReservationService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BuyerRepository buyerRepository;

    @Mock
    private UserRepository userRepository;

    // 테스트 데이터
    private OrderCreatedEvent testEvent;
    private Orders testOrder;
    private Products testProduct1;
    private Products testProduct2;
    private Users testUser;
    private Buyers testBuyer;
    private List<StockReservation> testReservations;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = Users.builder()
                .id("user123")
                .name("김철수")
                .role(Role.ROLE_BUYER)
                .provider("google")
                .providerId("google123")
                .build();

        // 테스트 구매자 생성
        testBuyer = Buyers.builder()
                .userId(testUser.getId()) // 수정: id() -> userId()
                .user(testUser)
                .build();

        // 테스트 상품들 생성
        testProduct1 = Products.builder()
                .id("product1")
                .title("강아지 사료") // 수정: productName() -> title()
                .price(25000L)
                .stock(100)
                .build();

        testProduct2 = Products.builder()
                .id("product2")
                .title("고양이 간식") // 수정: productName() -> title()
                .price(15000L)
                .stock(50)
                .build();

        // 테스트 주문 생성
        testOrder = Orders.builder()
                .id("order123")
                .orderNumber(1001L)
                .user(testUser)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(65000L) // 금액 일치시킴
                .build();

        // 테스트 이벤트 생성
        List<OrderCreatedEvent.OrderItemInfo> orderItems = Arrays.asList(
                OrderCreatedEvent.OrderItemInfo.builder()
                        .productId("product1")
                        .productName("강아지 사료")
                        .quantity(2)
                        .unitPrice(25000L)
                        .totalPrice(50000L)
                        .build(),
                OrderCreatedEvent.OrderItemInfo.builder()
                        .productId("product2")
                        .productName("고양이 간식")
                        .quantity(1)
                        .unitPrice(15000L)
                        .totalPrice(15000L)
                        .build()
        );

        testEvent = OrderCreatedEvent.of(
                "order123",
                1001L,
                "user123",
                "google",
                "google123",
                65000L,
                orderItems
        );

        // 테스트 재고 예약 생성
        testReservations = Arrays.asList(
                StockReservation.createReservation(testOrder, testProduct1, 2, 30),
                StockReservation.createReservation(testOrder, testProduct2, 1, 30)
        );
    }

    @Nested
    @DisplayName("재고 예약 처리 테스트")
    class StockReservationHandlerTests {

        @Test
        @DisplayName("✅ 재고 예약 성공")
        void handleStockReservation_Success() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
            given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));
            given(stockReservationService.createBulkReservations(eq(testOrder), any()))
                    .willReturn(testReservations);

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(stockReservationService).createBulkReservations(eq(testOrder), any());
            verify(orderRepository, never()).save(any()); // 성공 시에는 주문 상태 변경 없음
        }

        @Test
        @DisplayName("❌ 주문 정보 조회 실패 시 보상 트랜잭션 실행")
        void handleStockReservation_OrderNotFound_CompensationTriggered() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.empty());

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(orderRepository, times(2)).findById("order123");
            // 주문이 없으므로 보상 트랜잭션은 실행되지만, 내부에서 다시 findById를 호출하고 예외를 던지므로 save는 호출되지 않음.
            verify(orderRepository, never()).save(any());
            verify(stockReservationService, never()).createBulkReservations(any(), any());
        }

        @Test
        @DisplayName("❌ 재고 부족으로 예약 실패 - 보상 트랜잭션 실행")
        void handleStockReservation_InsufficientStock_CompensationTriggered() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
            given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));
            willThrow(new IllegalArgumentException("재고가 부족합니다"))
                    .given(stockReservationService).createBulkReservations(any(), any());

            // 보상 트랜잭션 Mocking
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(stockReservationService).createBulkReservations(any(), any());
            verify(orderRepository, times(2)).findById("order123"); // 최초 1번, 보상 트랜잭션에서 1번
            verify(orderRepository).save(testOrder); // 주문 상태를 CANCELLED로 변경하고 저장
            assert testOrder.getOrderStatus() == OrderStatus.CANCELLED;
        }

        @Test
        @DisplayName("❌ 동시성 충돌로 예약 실패 - 보상 트랜잭션 실행")
        void handleStockReservation_OptimisticLockFailure_CompensationTriggered() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
            given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));
            willThrow(new OptimisticLockingFailureException("동시성 충돌"))
                    .given(stockReservationService).createBulkReservations(any(), any());

            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(stockReservationService).createBulkReservations(any(), any());
            verify(orderRepository, times(2)).findById("order123");
            verify(orderRepository).save(testOrder);
            assert testOrder.getOrderStatus() == OrderStatus.CANCELLED;
        }
    }

    @Nested
    @DisplayName("결제 정보 생성 테스트")
    class PaymentInfoCreationTests {

        @Test
        @DisplayName("✅ 결제 정보 생성 성공")
        void handlePaymentInfoCreation_Success() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
            given(buyerRepository.findById("user123")).willReturn(Optional.of(testBuyer));

            // When
            orderEventListener.handlePaymentInfoCreation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(buyerRepository).findById("user123");
            verify(paymentRepository).save(any(Payments.class));
        }

        @Test
        @DisplayName("❌ 취소된 주문 - 결제 정보 생성 건너뜀")
        void handlePaymentInfoCreation_CancelledOrder_Skip() {
            // Given
            testOrder.setOrderStatus(OrderStatus.CANCELLED);
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));

            // When
            orderEventListener.handlePaymentInfoCreation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(buyerRepository, never()).findById(any());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ 구매자 정보 조회 실패")
        void handlePaymentInfoCreation_BuyerNotFound() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
            given(buyerRepository.findById("user123")).willReturn(Optional.empty());

            // When
            orderEventListener.handlePaymentInfoCreation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(buyerRepository).findById("user123");
            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("사용자 알림 처리 테스트")
    class UserNotificationTests {

        @Test
        @DisplayName("✅ 사용자 알림 발송 성공")
        void handleUserNotification_Success() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
            given(userRepository.findById("user123")).willReturn(Optional.of(testUser));

            // When
            orderEventListener.handleUserNotification(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(userRepository).findById("user123");
        }

        @Test
        @DisplayName("❌ 취소된 주문 - 알림 발송 건너뜀")
        void handleUserNotification_CancelledOrder_Skip() {
            // Given
            testOrder.setOrderStatus(OrderStatus.CANCELLED);
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));

            // When
            orderEventListener.handleUserNotification(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(userRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("감사 로깅 테스트")
    class AuditLoggingTests {

        @Test
        @DisplayName("✅ 주문 처리 감사 로그 기록")
        void handleOrderProcessingComplete_Success() {
            // When
            orderEventListener.handleOrderProcessingComplete(testEvent);

            // Then: 로깅은 실제 검증하기 어려우므로 메서드 호출 여부만으로 테스트를 대신합니다.
            // 실제 환경에서는 로그 어펜더를 Mocking하여 검증할 수 있습니다.
        }
    }
}