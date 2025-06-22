package com.team5.catdogeats.orders.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.service.OrderService;
import com.team5.catdogeats.payments.domain.Payments;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.BuyersRepository;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * 이벤트 기반 아키텍처(EDA) 주문 통합 테스트
 *
 * OrderCreatedEvent 발행부터 모든 이벤트 리스너의 처리 완료까지
 * 전체 이벤트 흐름을 검증하는 통합 테스트입니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("EDA 주문 통합 테스트")
class OrderIntegrationTest {

    /**
     * 테스트용 이벤트 캡처 설정
     */
    @TestConfiguration
    static class TestEventConfig {

        @Bean
        @Primary
        public TestEventCapture testEventCapture() {
            return new TestEventCapture();
        }
    }

    /**
     * 발행된 이벤트를 캡처하는 테스트용 리스너
     */
    static class TestEventCapture implements ApplicationListener<ApplicationEvent> {
        private final AtomicReference<OrderCreatedEvent> capturedEvent = new AtomicReference<>();
        private final CountDownLatch eventLatch = new CountDownLatch(1);

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            if (event instanceof OrderCreatedEvent orderEvent) {
                capturedEvent.set(orderEvent);
                eventLatch.countDown();
            }
        }

        public OrderCreatedEvent getCapturedEvent() {
            return capturedEvent.get();
        }

        public boolean waitForEvent(long timeout, TimeUnit unit) throws InterruptedException {
            return eventLatch.await(timeout, unit);
        }

        public void reset() {
            capturedEvent.set(null);
        }
    }

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BuyersRepository buyersRepository;
    @Autowired private SellersRepository sellersRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private TestEventCapture testEventCapture;
    @Autowired private ObjectMapper objectMapper;

    private Users testBuyer;
    private Buyers buyerEntity;
    private Products testProduct;
    private UserPrincipal testUserPrincipal;

    @BeforeEach
    void setUp() {
        testEventCapture.reset();

        // 판매자 및 상품 설정
        Users sellerUser = userRepository.save(Users.builder()
                .provider("DUMMY")
                .providerId("seller-test")
                .userNameAttribute("seller")
                .name("테스트 판매자")
                .role(Role.ROLE_SELLER)
                .accountDisable(false)
                .build());

        Sellers seller = sellersRepository.save(Sellers.builder()
                .user(sellerUser)
                .vendorName("테스트 펫샵")
                .businessNumber("123-45-67890")
                .build());

        testProduct = productRepository.save(Products.builder()
                .seller(seller)
                .title("테스트 강아지 사료")
                .contents("EDA 테스트용 상품")
                .productNumber(9999L)
                .price(30000L)
                .stock(50)
                .build());

        // 구매자 설정
        testBuyer = userRepository.save(Users.builder()
                .provider("GOOGLE")
                .providerId("buyer-test-eda")
                .userNameAttribute("test-buyer")
                .name("EDA 테스트 구매자")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .build());

        buyerEntity = buyersRepository.save(Buyers.builder()
                .user(testBuyer)
                .build());

        testUserPrincipal = new UserPrincipal(
                testBuyer.getProvider(),
                testBuyer.getProviderId(),
                testBuyer.getUserNameAttribute()
        );
    }

    @Test
    @DisplayName("주문 생성 시 OrderCreatedEvent 발행 검증")
    @Transactional
    void createOrder_ShouldPublishOrderCreatedEvent() throws Exception {
        // Given
        OrderCreateRequest request = createTestOrderRequest();

        // When
        var response = orderService.createOrderByUserPrincipal(testUserPrincipal, request);

        // Then - 이벤트 발행 확인
        boolean eventReceived = testEventCapture.waitForEvent(3, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();

        OrderCreatedEvent capturedEvent = testEventCapture.getCapturedEvent();
        assertThat(capturedEvent).isNotNull();
        assertThat(capturedEvent.getOrderId()).isNotNull();
        assertThat(capturedEvent.getUserId()).isEqualTo(testBuyer.getId());
        assertThat(capturedEvent.getTotalPrice()).isEqualTo(60000L); // 30000 * 2
        assertThat(capturedEvent.getOrderItems()).hasSize(1);

        // 이벤트에 포함된 주문 아이템 정보 검증
        var orderItem = capturedEvent.getOrderItems().get(0);
        assertThat(orderItem.getProductId()).isEqualTo(testProduct.getId());
        assertThat(orderItem.getQuantity()).isEqualTo(2);
        assertThat(orderItem.getProductName()).isEqualTo("테스트 강아지 사료");
    }

    @Test
    @DisplayName("재고 차감 이벤트 리스너 동작 검증")
    @Transactional
    void orderCreated_ShouldDeductStock() throws Exception {
        // Given
        int initialStock = testProduct.getStock();
        int orderQuantity = 3;

        OrderCreateRequest request = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(testProduct.getId().toString())
                                .quantity(orderQuantity)
                                .build()
                ))
                .shippingAddress(createTestShippingAddress())
                .paymentInfo(createTestPaymentInfo())
                .build();

        // When
        var response = orderService.createOrderByUserPrincipal(testUserPrincipal, request);

        // Then - 비동기 재고 차감 완료 대기
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Products updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
                    assertThat(updatedProduct.getStock()).isEqualTo(initialStock - orderQuantity);
                });
    }

    @Test
    @DisplayName("결제 정보 생성 이벤트 리스너 동작 검증")
    @Transactional
    void orderCreated_ShouldCreatePaymentInfo() throws Exception {
        // Given
        OrderCreateRequest request = createTestOrderRequest();

        // When
        var response = orderService.createOrderByUserPrincipal(testUserPrincipal, request);

        // Then - 비동기 결제 정보 생성 완료 대기
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Orders order = orderRepository.findByOrderNumber(response.getOrderNumber()).orElseThrow();

                    // 주문 상태가 결제 준비 완료로 변경됨
                    assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.READY_FOR_PAYMENT);

                    // 결제 정보가 생성됨
                    Payments payment = paymentRepository.findByOrdersId(order.getId()).orElseThrow();
                    assertThat(payment.getBuyers().getId()).isEqualTo(buyerEntity.getId());
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
                    assertThat(payment.getTossPaymentKey()).startsWith("temp_");
                });
    }

    @Test
    @DisplayName("재고 부족 시 주문 취소 보상 트랜잭션 검증")
    @Transactional
    void orderCreated_WithInsufficientStock_ShouldCancelOrder() throws Exception {
        // Given - 재고보다 많은 수량 주문
        int availableStock = testProduct.getStock();
        int requestedQuantity = availableStock + 10;

        OrderCreateRequest request = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(testProduct.getId().toString())
                                .quantity(requestedQuantity)
                                .build()
                ))
                .shippingAddress(createTestShippingAddress())
                .paymentInfo(createTestPaymentInfo())
                .build();

        // When
        var response = orderService.createOrderByUserPrincipal(testUserPrincipal, request);

        // Then - 보상 트랜잭션으로 주문 취소 확인
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Orders order = orderRepository.findByOrderNumber(response.getOrderNumber()).orElseThrow();
                    assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
                });

        // 재고는 변경되지 않아야 함
        Products updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(availableStock);
    }

    @Test
    @DisplayName("이벤트 리스너들의 독립적 트랜잭션 동작 검증")
    @Transactional
    void eventListeners_ShouldWorkIndependently() throws Exception {
        // Given
        OrderCreateRequest request = createTestOrderRequest();

        // When
        var response = orderService.createOrderByUserPrincipal(testUserPrincipal, request);

        // Then - 각 리스너가 독립적으로 성공해야 함
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Orders order = orderRepository.findByOrderNumber(response.getOrderNumber()).orElseThrow();

                    // 재고 차감 리스너 성공 확인
                    Products product = productRepository.findById(testProduct.getId()).orElseThrow();
                    assertThat(product.getStock()).isEqualTo(48); // 50 - 2

                    // 결제 정보 생성 리스너 성공 확인
                    assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.READY_FOR_PAYMENT);

                    Payments payment = paymentRepository.findByOrdersId(order.getId()).orElseThrow();
                    assertThat(payment).isNotNull();

                    // 알림 리스너는 로그만 확인 (실제 테스트에서는 Mock을 사용할 수 있음)
                });
    }

    // 테스트 헬퍼 메서드들
    private OrderCreateRequest createTestOrderRequest() {
        return OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(testProduct.getId().toString())
                                .quantity(2)
                                .build()
                ))
                .shippingAddress(createTestShippingAddress())
                .paymentInfo(createTestPaymentInfo())
                .build();
    }

    private OrderCreateRequest.ShippingAddressRequest createTestShippingAddress() {
        return OrderCreateRequest.ShippingAddressRequest.builder()
                .recipientName("홍길동")
                .phone("010-1234-5678")
                .address("서울시 강남구 테헤란로 123")
                .detailAddress("456호")
                .zipCode("12345")
                .build();
    }

    private OrderCreateRequest.PaymentInfoRequest createTestPaymentInfo() {
        return OrderCreateRequest.PaymentInfoRequest.builder()
                .customerEmail("test@catdogeats.com")
                .customerName("EDA 테스트")
                .build();
    }
}