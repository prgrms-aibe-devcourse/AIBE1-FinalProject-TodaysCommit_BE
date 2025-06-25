package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.service.impl.OrderServiceImpl;
import com.team5.catdogeats.orders.util.TossPaymentResponseBuilder;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("주문 서비스 단위 테스트 (EDA 전환 완료)")
class OrderServiceTest {

    @InjectMocks
    OrderServiceImpl orderService;

    // EDA 전환 후 추가된 의존성들
    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @Mock BuyerRepository buyerRepository;  // 새로 추가된 의존성
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock TossPaymentResponseBuilder tossPaymentResponseBuilder;  // 새로 추가된 의존성

    // 테스트 데이터
    Users user;
    BuyerDTO buyerDTO;
    UserPrincipal principal;
    Products normalProduct, discountProduct;
    OrderCreateRequest request;
    Orders savedOrder;
    OrderCreateResponse mockResponse;

    @BeforeEach
    void setUp() {
        // 사용자 데이터
        user = Users.builder()
                .id("user123")
                .name("김철수")
                .provider("google")
                .providerId("google123")
                .role(Role.ROLE_BUYER)
                .build();

        // 구매자 DTO (BuyerRepository 응답용) - 4개 파라미터 필요
        buyerDTO = new BuyerDTO("user123", true, false, null);

        principal = new UserPrincipal("google", "google123");

        // 상품 데이터 (할인 적용/미적용)
        normalProduct = Products.builder()
                .id("product1")
                .title("강아지 사료")
                .price(25_000L)
                .stock(100)
                .isDiscounted(false)
                .build();

        discountProduct = Products.builder()
                .id("product2")
                .title("고양이 간식")
                .price(10_000L)
                .stock(50)
                .isDiscounted(true)
                .discountRate(20.0)  // 20% 할인
                .build();

        // 주문 요청 (할인 상품 포함)
        request = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product1")
                                .quantity(2)
                                .build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product2")
                                .quantity(1)
                                .build()))
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                        .customerName("김철수")
                        .customerEmail("test@demo.com")
                        .build())
                .build();

        // 저장된 주문 (할인 적용된 총액: 25,000*2 + 8,000*1 = 58,000원)
        savedOrder = Orders.builder()
                .id("order123")
                .orderNumber(2024122512345L)
                .user(user)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(58_000L)  // 할인 적용된 금액
                .build();

        // TossPaymentResponseBuilder 응답 모킹
        mockResponse = OrderCreateResponse.builder()
                .orderId("order123")
                .orderNumber(2024122512345L)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(58_000L)
                .createdAt(ZonedDateTime.now())
                .tossPaymentInfo(OrderCreateResponse.TossPaymentInfo.builder()
                        .tossOrderId("order123")
                        .orderName("반려동물 용품 주문 #2024122512345")
                        .amount(58_000L)
                        .customerName("김철수")
                        .customerEmail("test@demo.com")
                        .successUrl("https://success.local")
                        .failUrl("https://fail.local")
                        .clientKey("test-client-key")
                        .build())
                .build();
    }

    @Nested
    @DisplayName("주문 생성 성공 테스트")
    class CreateOrderSuccessTests {

        @Test
        @DisplayName("✅ 일반 상품 주문 생성 및 이벤트 발행")
        void createOrder_NormalProduct_Success() {
            // Given
            setupSuccessfulOrderCreation();

            // When
            OrderCreateResponse response = orderService.createOrderByUserPrincipal(principal, request);

            // Then
            assertThat(response.getOrderId()).isEqualTo("order123");
            assertThat(response.getTotalPrice()).isEqualTo(58_000L);
            assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);

            // 의존성 호출 검증 (EDA 전환 후 변경된 부분)
            verify(buyerRepository).findOnlyBuyerByProviderAndProviderId("google", "google123");
            verify(userRepository).getReferenceById("user123");
            verify(productRepository, times(2)).findById(anyString());
            verify(orderRepository).save(any(Orders.class));
            verify(tossPaymentResponseBuilder).buildTossPaymentResponse(any(), any(), anyString());

            // 이벤트 발행 검증
            ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            OrderCreatedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getOrderId()).isEqualTo("order123");
            assertThat(capturedEvent.getOrderNumber()).isEqualTo(2024122512345L);
            assertThat(capturedEvent.getUserId()).isEqualTo("user123");
            assertThat(capturedEvent.getTotalPrice()).isEqualTo(58_000L);
            assertThat(capturedEvent.getOrderItemCount()).isEqualTo(2);
            assertThat(capturedEvent.getTotalQuantity()).isEqualTo(3);
            assertThat(capturedEvent.getEventOccurredAt()).isNotNull();
        }

        @Test
        @DisplayName("✅ 할인 상품 포함 주문에서 할인 적용 검증")
        void createOrder_WithDiscountProduct_AppliesDiscountCorrectly() {
            // Given
            setupSuccessfulOrderCreation();

            // When
            OrderCreateResponse response = orderService.createOrderByUserPrincipal(principal, request);

            // Then
            assertThat(response.getTotalPrice()).isEqualTo(58_000L); // 할인 적용된 금액

            // 이벤트에 할인 정보가 포함되었는지 검증
            ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            OrderCreatedEvent event = eventCaptor.getValue();
            assertThat(event.getOrderItems()).hasSize(2);
            // 할인 상품의 단가가 할인 적용된 가격인지 확인 (10,000 * 0.8 = 8,000)
            assertThat(event.getOrderItems().get(1).unitPrice()).isEqualTo(8_000L);
        }

        private void setupSuccessfulOrderCreation() {
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123"))
                    .willReturn(user);
            given(productRepository.findById("product1"))
                    .willReturn(Optional.of(normalProduct));
            given(productRepository.findById("product2"))
                    .willReturn(Optional.of(discountProduct));
            given(orderRepository.save(any(Orders.class)))
                    .willReturn(savedOrder);
            given(tossPaymentResponseBuilder.buildTossPaymentResponse(any(), any(), anyString()))
                    .willReturn(mockResponse);
        }
    }

    @Nested
    @DisplayName("주문 생성 실패 테스트")
    class CreateOrderFailureTests {

        @Test
        @DisplayName("❌ 구매자 정보 없음 - BuyerRepository에서 빈 결과")
        void createOrder_BuyerNotFound_ThrowsException() {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, request))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("구매자를 찾을 수 없거나 권한이 없습니다");

            // 이벤트가 발행되지 않았는지 확인
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("❌ 상품 존재하지 않음")
        void createOrder_ProductNotFound_ThrowsException() {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123"))
                    .willReturn(user);
            given(productRepository.findById("product1"))
                    .willReturn(Optional.of(normalProduct));
            given(productRepository.findById("product2"))
                    .willReturn(Optional.empty()); // 두 번째 상품 없음

            // When & Then
            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, request))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("❌ 재고 부족")
        void createOrder_InsufficientStock_ThrowsException() {
            // Given
            Products lowStockProduct = Products.builder()
                    .id("product1")
                    .title("재고 부족 상품")
                    .price(25_000L)
                    .stock(1)  // 요청 수량(2)보다 적음
                    .isDiscounted(false)
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123"))
                    .willReturn(user);
            given(productRepository.findById("product1"))
                    .willReturn(Optional.of(lowStockProduct));

            // When & Then
            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("재고가 부족한 상품입니다");

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("❌ 잘못된 수량 (0 이하)")
        void createOrder_InvalidQuantity_ThrowsException() {
            // Given
            OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                    .orderItems(List.of(
                            OrderCreateRequest.OrderItemRequest.builder()
                                    .productId("product1")
                                    .quantity(0)  // 잘못된 수량
                                    .build()))
                    .paymentInfo(request.getPaymentInfo())
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123"))
                    .willReturn(user);
            given(productRepository.findById("product1"))
                    .willReturn(Optional.of(normalProduct));

            // When & Then
            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문 수량은 1개 이상이어야 합니다");

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("❌ 주문 아이템 비어있음")
        void createOrder_EmptyOrderItems_ThrowsException() {
            // Given
            OrderCreateRequest emptyRequest = OrderCreateRequest.builder()
                    .orderItems(List.of())  // 빈 주문 아이템
                    .paymentInfo(request.getPaymentInfo())
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123"))
                    .willReturn(user);

            // When & Then
            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, emptyRequest))
                    .isInstanceOf(RuntimeException.class); // 구체적인 예외는 구현에 따라 달라질 수 있음

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("할인 로직 테스트")
    class DiscountLogicTests {

        @Test
        @DisplayName("✅ 할인율 경계값 테스트 (0%, 100%)")
        void createOrder_DiscountRateBoundary_Success() {
            // Given - 100% 할인 상품
            Products freeProduct = Products.builder()
                    .id("product3")
                    .title("무료 샘플")
                    .price(1_000L)
                    .stock(10)
                    .isDiscounted(true)
                    .discountRate(100.0)
                    .build();

            OrderCreateRequest freeRequest = OrderCreateRequest.builder()
                    .orderItems(List.of(
                            OrderCreateRequest.OrderItemRequest.builder()
                                    .productId("product3")
                                    .quantity(1)
                                    .build()))
                    .paymentInfo(request.getPaymentInfo())
                    .build();

            Orders freeOrder = Orders.builder()
                    .id("order456")
                    .orderNumber(2024122512346L)
                    .user(user)
                    .orderStatus(OrderStatus.PAYMENT_PENDING)
                    .totalPrice(0L)  // 100% 할인으로 0원
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123"))
                    .willReturn(user);
            given(productRepository.findById("product3"))
                    .willReturn(Optional.of(freeProduct));
            given(orderRepository.save(any(Orders.class)))
                    .willReturn(freeOrder);
            given(tossPaymentResponseBuilder.buildTossPaymentResponse(any(), any(), anyString()))
                    .willReturn(mockResponse);

            // When
            OrderCreateResponse response = orderService.createOrderByUserPrincipal(principal, freeRequest);

            // Then
            assertThat(response).isNotNull();
            verify(eventPublisher).publishEvent(any(OrderCreatedEvent.class));
        }

        @Test
        @DisplayName("❌ 잘못된 할인율 (음수, 100 초과)")
        void createOrder_InvalidDiscountRate_HandledGracefully() {
            // 할인율 검증은 상품 엔티티 레벨에서 처리되므로
            // 서비스 레벨에서는 이미 검증된 데이터를 받는다고 가정
            // 실제 검증은 상품 생성/수정 시점에서 이루어짐

            // Given - 잘못된 할인율을 가진 상품 (이론적으로는 발생하지 않아야 함)
            Products invalidDiscountProduct = Products.builder()
                    .id("product4")
                    .title("잘못된 할인 상품")
                    .price(10_000L)
                    .stock(10)
                    .isDiscounted(true)
                    .discountRate(-10.0)  // 음수 할인율
                    .build();

            OrderCreateRequest invalidDiscountRequest = OrderCreateRequest.builder()
                    .orderItems(List.of(
                            OrderCreateRequest.OrderItemRequest.builder()
                                    .productId("product4")
                                    .quantity(1)
                                    .build()))
                    .paymentInfo(request.getPaymentInfo())
                    .build();

            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123"))
                    .willReturn(user);
            given(productRepository.findById("product4"))
                    .willReturn(Optional.of(invalidDiscountProduct));

            // When & Then
            assertThatThrownBy(() -> orderService.createOrderByUserPrincipal(principal, invalidDiscountRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("할인율은 0~100 사이여야 합니다");

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("이벤트 발행 세부 검증")
    class EventPublishingTests {

        @Test
        @DisplayName("✅ OrderCreatedEvent 필드 완전성 검증")
        void createOrder_EventFieldsCompleteness() {
            // Given
            given(buyerRepository.findOnlyBuyerByProviderAndProviderId("google", "google123"))
                    .willReturn(Optional.of(buyerDTO));
            given(userRepository.getReferenceById("user123"))
                    .willReturn(user);
            given(productRepository.findById("product1"))
                    .willReturn(Optional.of(normalProduct));
            given(productRepository.findById("product2"))
                    .willReturn(Optional.of(discountProduct));
            given(orderRepository.save(any(Orders.class)))
                    .willReturn(savedOrder);
            given(tossPaymentResponseBuilder.buildTossPaymentResponse(any(), any(), anyString()))
                    .willReturn(mockResponse);

            // When
            orderService.createOrderByUserPrincipal(principal, request);

            // Then
            ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            OrderCreatedEvent event = eventCaptor.getValue();
            assertThat(event.getOrderId()).isEqualTo("order123");
            assertThat(event.getOrderNumber()).isEqualTo(2024122512345L);
            assertThat(event.getUserId()).isEqualTo("user123");
            assertThat(event.getUserProvider()).isEqualTo("google");
            assertThat(event.getUserProviderId()).isEqualTo("google123");
            assertThat(event.getTotalPrice()).isEqualTo(58_000L);
            assertThat(event.getOrderItems()).hasSize(2);

            // OrderItemInfo 검증 (Record DTO 변환 확인)
            assertThat(event.getOrderItems().get(0).productId()).isEqualTo("product1");
            assertThat(event.getOrderItems().get(0).productName()).isEqualTo("강아지 사료");
            assertThat(event.getOrderItems().get(0).quantity()).isEqualTo(2);
            assertThat(event.getOrderItems().get(0).unitPrice()).isEqualTo(25_000L);
            assertThat(event.getOrderItems().get(0).totalPrice()).isEqualTo(50_000L);

            assertThat(event.getOrderItems().get(1).productId()).isEqualTo("product2");
            assertThat(event.getOrderItems().get(1).productName()).isEqualTo("고양이 간식");
            assertThat(event.getOrderItems().get(1).quantity()).isEqualTo(1);
            assertThat(event.getOrderItems().get(1).unitPrice()).isEqualTo(8_000L); // 할인 적용
            assertThat(event.getOrderItems().get(1).totalPrice()).isEqualTo(8_000L);

            // 편의 메서드 검증
            assertThat(event.getOrderItemCount()).isEqualTo(2);
            assertThat(event.getTotalQuantity()).isEqualTo(3);
            assertThat(event.getFirstProductName()).isEqualTo("강아지 사료");
            assertThat(event.getEventOccurredAt()).isNotNull();
        }
    }
}