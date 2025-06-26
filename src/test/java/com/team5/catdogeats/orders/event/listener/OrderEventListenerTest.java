package com.team5.catdogeats.orders.event.listener;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.payments.domain.Payments;
import com.team5.catdogeats.payments.domain.enums.PaymentMethod;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.service.StockReservationService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventListener 테스트 (EDA + Record DTO)")
class OrderEventListenerTest {

    @InjectMocks
    private OrderEventListener orderEventListener;

    @Mock
    private StockReservationService stockReservationService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BuyerRepository buyerRepository;
    @Mock
    private ProductRepository productRepository;

    // 테스트 데이터
    private Orders testOrder;
    private Users testUser;
    private Buyers testBuyer;
    private BuyerDTO testBuyerDTO;
    private Products testProduct1;
    private Products testProduct2;
    private OrderCreatedEvent testEvent;
    private List<StockReservation> testReservations;
    private Payments testPayment;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = Users.builder()
                .id("user123")
                .provider("google")
                .providerId("google123")
                .name("김철수")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .build();

        // 테스트 구매자 DTO
        testBuyerDTO = new BuyerDTO("user123", true, false, null);

        // 테스트 구매자 엔티티
        testBuyer = Buyers.builder()
                .userId("user123")
                .user(testUser)
                .nameMaskingStatus(true)
                .isDeleted(false)
                .deledAt(null)
                .build();

        // 테스트 상품들 생성
        testProduct1 = Products.builder()
                .id("product1")
                .title("강아지 사료")
                .price(25000L)
                .stock(100)
                .build();

        testProduct2 = Products.builder()
                .id("product2")
                .title("고양이 간식")
                .price(15000L)
                .stock(50)
                .build();

        // 테스트 주문 생성
        testOrder = Orders.builder()
                .id("order123")
                .orderNumber(1001L)
                .user(testUser)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(65000L)
                .build();

        // 테스트 이벤트 생성 (Record DTO 정적 팩토리 메서드 사용)
        List<OrderItemInfo> orderItems = Arrays.asList(
                OrderItemInfo.of("product1", "강아지 사료", 2, 25000L),
                OrderItemInfo.of("product2", "고양이 간식", 1, 15000L)
        );

        testEvent = OrderCreatedEvent.of(
                "order123",
                1001L,
                "user123",
                "google",
                "google123",
                65000L,   // originalTotalPrice
                null,     // couponDiscountRate (없으면 null)
                65000L,   // finalTotalPrice
                orderItems
        );

        // 테스트 재고 예약 목록
        testReservations = Arrays.asList(
                StockReservation.createReservation(testOrder, testProduct1, 2, 30),
                StockReservation.createReservation(testOrder, testProduct2, 1, 30)
        );

        // 테스트 결제 정보
        testPayment = Payments.builder()
                .orders(testOrder)
                .buyers(testBuyer)
                .method(PaymentMethod.TOSS)
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("재고 예약 처리 테스트")
    class StockReservationTests {

        @Test
        @DisplayName("✅ 재고 예약 성공")
        void handleStockReservation_Success() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(testOrder));
            given(productRepository.findById("product1"))
                    .willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2"))
                    .willReturn(Optional.of(testProduct2));
            given(stockReservationService.createBulkReservations(eq(testOrder), anyList()))
                    .willReturn(testReservations);

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(stockReservationService).createBulkReservations(eq(testOrder), anyList());
        }

        @Test
        @DisplayName("❌ 주문 없음")
        void handleStockReservation_OrderNotFound() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.empty());

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(stockReservationService, never()).createBulkReservations(any(), any());
        }

        @Test
        @DisplayName("❌ 재고 부족 → 보상 트랜잭션")
        void handleStockReservation_InsufficientStock_Compensation() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(testOrder))
                    .willReturn(Optional.of(testOrder)); // 보상 트랜잭션용
            given(productRepository.findById("product1"))
                    .willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2"))
                    .willReturn(Optional.of(testProduct2));
            given(stockReservationService.createBulkReservations(eq(testOrder), anyList()))
                    .willThrow(new IllegalArgumentException("재고가 부족합니다"));

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(orderRepository, times(2)).findById("order123");
            verify(stockReservationService).createBulkReservations(eq(testOrder), anyList());
            verify(orderRepository).save(any(Orders.class));
        }

        @Test
        @DisplayName("❌ 동시성 충돌 → 보상 트랜잭션")
        void handleStockReservation_OptimisticLockingFailure_Compensation() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(testOrder))
                    .willReturn(Optional.of(testOrder));
            given(productRepository.findById("product1"))
                    .willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2"))
                    .willReturn(Optional.of(testProduct2));
            given(stockReservationService.createBulkReservations(eq(testOrder), anyList()))
                    .willThrow(new OptimisticLockingFailureException("동시성 충돌"));

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(orderRepository, times(2)).findById("order123");
            verify(stockReservationService).createBulkReservations(eq(testOrder), anyList());
            verify(orderRepository).save(any(Orders.class));
        }
    }

    @Nested
    @DisplayName("결제 정보 생성 테스트")
    class PaymentInfoCreationTests {

        @Test
        @DisplayName("✅ 결제 정보 생성 성공")
        void handlePaymentInfoCreation_Success() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(testOrder));
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testBuyerDTO));
            given(paymentRepository.findByOrdersId("order123"))
                    .willReturn(Optional.empty());
            given(paymentRepository.save(any(Payments.class)))
                    .willReturn(testPayment);

            // When
            orderEventListener.handlePaymentInfoCreation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(paymentRepository).findByOrdersId("order123");
            verify(paymentRepository).save(any(Payments.class));
        }

        @Test
        @DisplayName("✅ 취소된 주문 - 결제 정보 생성 건너뜀")
        void handlePaymentInfoCreation_CancelledOrder_Skip() {
            // Given
            Orders cancelledOrder = Orders.builder()
                    .id("order123")
                    .orderNumber(1001L)
                    .user(testUser)
                    .orderStatus(OrderStatus.CANCELLED)
                    .totalPrice(65000L)
                    .build();

            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(cancelledOrder));

            // When
            orderEventListener.handlePaymentInfoCreation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(buyerRepository, never()).findOnlyBuyerByProviderAndProviderId(any(), any());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ 구매자 정보 없음")
        void handlePaymentInfoCreation_BuyerNotFound() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(testOrder));
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.empty());

            // When
            orderEventListener.handlePaymentInfoCreation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ 이미 결제 정보 존재 - 건너뜀")
        void handlePaymentInfoCreation_PaymentAlreadyExists_Skip() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(testOrder));
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(testBuyerDTO));
            given(paymentRepository.findByOrdersId("order123"))
                    .willReturn(Optional.of(testPayment));

            // When
            orderEventListener.handlePaymentInfoCreation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(paymentRepository).findByOrdersId("order123");
            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("사용자 알림 처리 테스트")
    class UserNotificationTests {

        @Test
        @DisplayName("✅ 사용자 알림 성공")
        void handleUserNotification_Success() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(testOrder));

            // When
            orderEventListener.handleUserNotification(testEvent);

            // Then
            verify(orderRepository).findById("order123");
        }

        @Test
        @DisplayName("✅ 취소된 주문 - 알림 건너뜀")
        void handleUserNotification_CancelledOrder_Skip() {
            // Given
            Orders cancelledOrder = Orders.builder()
                    .id("order123")
                    .orderNumber(1001L)
                    .user(testUser)
                    .orderStatus(OrderStatus.CANCELLED)
                    .totalPrice(65000L)
                    .build();

            given(orderRepository.findById("order123"))
                    .willReturn(Optional.of(cancelledOrder));

            // When
            orderEventListener.handleUserNotification(testEvent);

            // Then
            verify(orderRepository).findById("order123");
        }

        @Test
        @DisplayName("❌ 주문 없음 - 예외 처리")
        void handleUserNotification_OrderNotFound_HandleException() {
            // Given
            given(orderRepository.findById("order123"))
                    .willReturn(Optional.empty());

            // When
            orderEventListener.handleUserNotification(testEvent);

            // Then
            verify(orderRepository).findById("order123");
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

            // Then - 단순히 예외 없이 완료되면 성공
        }

        @Test
        @DisplayName("✅ Record DTO 편의 메서드 활용 검증")
        void handleOrderProcessingComplete_RecordMethodsVerification() {
            // Given
            OrderCreatedEvent emptyEvent = OrderCreatedEvent.of(
                    "order456",
                    123456L,
                    "user456",
                    "google",
                    "google456",
                    0L,      // originalTotalPrice
                    null,    // couponDiscountRate
                    0L,      // finalTotalPrice
                    List.of()
            );

            // When
            orderEventListener.handleOrderProcessingComplete(emptyEvent);

            // Then - 빈 이벤트도 정상 처리
        }
    }

    @Nested
    @DisplayName("Record DTO 통합 테스트")
    class RecordDTOIntegrationTests {

        @Test
        @DisplayName("✅ OrderItemInfo Record 불변성 검증")
        void orderItemInfo_Immutability_Verified() {
            // Given
            OrderItemInfo item1 = OrderItemInfo.of("product1", "상품명", 2, 1000L);
            OrderItemInfo item2 = OrderItemInfo.of("product1", "상품명", 2, 1000L);

            // Then - Record의 자동 equals/hashCode 구현 검증
            assertThat(item1).isEqualTo(item2);
            assertThat(item1.hashCode()).isEqualTo(item2.hashCode());

            // 불변성 검증
            assertThat(item1.productId()).isEqualTo("product1");
            assertThat(item1.quantity()).isEqualTo(2);
            assertThat(item1.unitPrice()).isEqualTo(1000L);
            assertThat(item1.totalPrice()).isEqualTo(2000L);
        }

        @Test
        @DisplayName("✅ OrderCreatedEvent 편의 메서드 검증")
        void orderCreatedEvent_ConvenienceMethods_Verified() {
            // Then
            assertThat(testEvent.getOrderItemCount()).isEqualTo(2);
            assertThat(testEvent.getTotalQuantity()).isEqualTo(3);
            assertThat(testEvent.getFirstProductName()).isEqualTo("강아지 사료");
            assertThat(testEvent.getEventOccurredAt()).isNotNull();
        }
    }
}