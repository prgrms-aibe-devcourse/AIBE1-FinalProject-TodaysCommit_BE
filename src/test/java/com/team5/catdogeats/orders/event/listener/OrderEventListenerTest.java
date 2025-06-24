package com.team5.catdogeats.orders.event.listener;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * OrderEventListener 테스트 (Record DTO 적용)
 * Record DTO 사용으로 인한 테스트 코드 변경사항:
 * 1. OrderItemInfo Record 정적 팩토리 메서드 사용
 * 2. Record의 불변성 활용
 * 3. 메서드 호출 방식 변경 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventListener 테스트 (Record DTO)")
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

    private Orders testOrder;
    private Users testUser;
    private Buyers testBuyer;
    private Products testProduct1;
    private Products testProduct2;
    private OrderCreatedEvent testEvent;
    private List<StockReservation> testReservations;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = Users.builder()
                .id("user123")
                .provider("google")
                .providerId("google123")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .build();

        // 테스트 구매자 생성
        testBuyer = Buyers.builder()
                .userId("user123")
                .user(testUser)
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
    class StockReservationTests {

        @Test
        @DisplayName("✅ 재고 예약 성공 (Record DTO 활용)")
        void handleStockReservation_Success_WithRecordDTO() {
            // Given - 모든 Mock을 한 번에 설정
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));
            given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));
            given(stockReservationService.createBulkReservations(any(Orders.class), anyList()))
                    .willReturn(testReservations);

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(orderRepository).findById("order123");
            verify(stockReservationService).createBulkReservations(eq(testOrder), anyList());
            verify(productRepository).findById("product1");
            verify(productRepository).findById("product2");
        }

        @Test
        @DisplayName("❌ 재고 부족으로 예약 실패 → 보상 트랜잭션 실행")
        void handleStockReservation_InsufficientStock_CompensationTriggered() {
            // Given - 초기 주문 조회 성공
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));

            // Product Mock 설정 (예외 발생 전에 모든 product가 조회되어야 함)
            given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));

            // 재고 예약 실패 시뮬레이션
            given(stockReservationService.createBulkReservations(any(Orders.class), anyList()))
                    .willThrow(new IllegalArgumentException("재고가 부족합니다"));

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(orderRepository, times(2)).findById("order123"); // 1번: 예약 시도, 1번: 보상 트랜잭션
            verify(stockReservationService).createBulkReservations(any(Orders.class), anyList());
            verify(orderRepository).save(testOrder); // 보상 트랜잭션에서 주문 상태 변경
        }

        @Test
        @DisplayName("❌ 동시성 충돌로 예약 실패 → 보상 트랜잭션 실행")
        void handleStockReservation_OptimisticLockingFailure_CompensationTriggered() {
            // Given - 초기 주문 조회 성공
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));

            // Product Mock 설정 (예외 발생 전에 모든 product가 조회되어야 함)
            given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
            given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));

            // 동시성 충돌 예외 시뮬레이션
            given(stockReservationService.createBulkReservations(any(Orders.class), anyList()))
                    .willThrow(new OptimisticLockingFailureException("동시성 충돌"));

            // When
            orderEventListener.handleStockReservation(testEvent);

            // Then
            verify(orderRepository, times(2)).findById("order123");
            verify(stockReservationService).createBulkReservations(any(Orders.class), anyList());
            verify(orderRepository).save(testOrder); // 보상 트랜잭션에서 주문 상태 변경
        }

        @Test
        @DisplayName("❌ 주문을 찾을 수 없음")
        void handleStockReservation_OrderNotFound() {
            // Given - 주문을 찾을 수 없는 상황
            given(orderRepository.findById("order123")).willReturn(Optional.empty());

            // When & Then - 예외가 발생하므로 보상 트랜잭션도 실행되지 않음
            orderEventListener.handleStockReservation(testEvent);

            verify(orderRepository).findById("order123");
            verify(stockReservationService, never()).createBulkReservations(any(), any());
            verify(orderRepository, never()).save(any()); // 주문이 없으므로 저장도 없음
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
        @DisplayName("❌ 구매자를 찾을 수 없음")
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
        @DisplayName("✅ 사용자 알림 발송 성공 (Record 편의 메서드 활용)")
        void handleUserNotification_Success_WithRecordMethods() {
            // Given
            given(orderRepository.findById("order123")).willReturn(Optional.of(testOrder));

            // When
            orderEventListener.handleUserNotification(testEvent);

            // Then
            verify(orderRepository).findById("order123");

            // Record의 편의 메서드들이 올바르게 동작하는지 확인
            // (실제 알림 발송 로직이 구현되면 더 상세한 검증 추가)
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
        }
    }

    @Nested
    @DisplayName("감사 로깅 테스트")
    class AuditLoggingTests {

        @Test
        @DisplayName("✅ 주문 처리 감사 로그 기록 (Record 편의 메서드 활용)")
        void handleOrderProcessingComplete_Success_WithRecordMethods() {
            // When
            orderEventListener.handleOrderProcessingComplete(testEvent);

            // Then
            // Record의 편의 메서드들이 올바르게 동작하는지 검증
            // 실제 환경에서는 로그 어펜더를 Mocking하여 로그 내용을 검증할 수 있습니다.

            // 이벤트의 편의 메서드들이 예상한 값을 반환하는지 확인
            assert testEvent.getOrderItemCount() == 2;
            assert testEvent.getTotalQuantity() == 3; // 2 + 1
            assert testEvent.getFirstProductName().equals("강아지 사료");
        }
    }

    @Nested
    @DisplayName("Record DTO 메서드 동작 테스트")
    class RecordDTOMethodTests {

        @Test
        @DisplayName("✅ OrderItemInfo Record 메서드 동작 확인")
        void orderItemInfo_RecordMethods_WorkCorrectly() {
            // Given
            OrderItemInfo item = OrderItemInfo.of("test-product", "테스트 상품", 5, 10000L);

            // When & Then
            assert item.productId().equals("test-product");
            assert item.productName().equals("테스트 상품");
            assert item.quantity() == 5;
            assert item.unitPrice() == 10000L;
            assert item.totalPrice() == 50000L; // 10000 * 5
        }

        @Test
        @DisplayName("✅ OrderCreatedEvent 편의 메서드 동작 확인")
        void orderCreatedEvent_ConvenienceMethods_WorkCorrectly() {
            // When & Then
            assert testEvent.getOrderItemCount() == 2;
            assert testEvent.getTotalQuantity() == 3; // 2 + 1
            assert testEvent.getFirstProductName().equals("강아지 사료");
            assert testEvent.getTotalPrice() == 65000L;
        }
    }
}