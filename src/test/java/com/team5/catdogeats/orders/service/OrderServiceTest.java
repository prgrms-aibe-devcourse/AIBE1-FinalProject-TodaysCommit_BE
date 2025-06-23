package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.service.impl.OrderServiceImpl;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 주문 서비스 테스트 (EDA 적용)
 * 통합 테스트를 대체하는 서비스 레벨 단위 테스트입니다.
 * 이벤트 발행까지만 검증하고, 이벤트 처리는 별도 리스너 테스트에서 담당합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("주문 서비스 테스트 (EDA)")
class OrderServiceTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Users testUser;
    private Products testProduct1;
    private Products testProduct2;
    private OrderCreateRequest testRequest;
    private Orders testOrder;

    @BeforeEach
    void setUp() {
        // 테스트 사용자
        testUser = Users.builder()
                .id("user123")
                .name("김철수")
                .role(Role.ROLE_BUYER)
                .provider("google")
                .providerId("google123")
                .build();

        // 테스트 상품들
        testProduct1 = Products.builder()
                .id("product1")
                .title("강아지 사료")
                .price(25000L)
                .stock(100)
                .build();

        testProduct2 = Products.builder()
                .id("product2")
                .title("고양이 간식")
                .price(5000L)
                .stock(50)
                .build();

        // 테스트 요청
        testRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product1")
                                .quantity(2)
                                .build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product2")
                                .quantity(1)
                                .build()
                ))
                .build();

        // 테스트 주문 (저장 후 반환될 객체)
        testOrder = Orders.builder()
                .id("order123")
                .orderNumber("ORD-20250624-001")
                .userId(testUser.getId())
                .totalAmount(55000L)
                .build();
    }

    @Test
    @DisplayName("✅ 주문 생성 성공 및 이벤트 발행")
    void createOrder_Success() {
        // Given
        given(userRepository.findById("user123")).willReturn(Optional.of(testUser));
        given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
        given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));
        given(orderRepository.save(any(Orders.class))).willReturn(testOrder);

        // When
        OrderCreateResponse response = orderService.createOrder("user123", testRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("order123");
        assertThat(response.getOrderNumber()).isEqualTo("ORD-20250624-001");
        assertThat(response.getTotalAmount()).isEqualTo(55000L);
        assertThat(response.getOrderItems()).hasSize(2);

        // 주문 저장 검증
        verify(orderRepository).save(any(Orders.class));

        // 이벤트 발행 검증
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        OrderCreatedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getOrderId()).isEqualTo("order123");
        assertThat(publishedEvent.getOrderNumber()).isEqualTo("ORD-20250624-001");
        assertThat(publishedEvent.getUserId()).isEqualTo("user123");
        assertThat(publishedEvent.getTotalAmount()).isEqualTo(55000L);
        assertThat(publishedEvent.getOrderItems()).hasSize(2);
    }

    @Test
    @DisplayName("❌ 사용자 없음 - 주문 생성 실패")
    void createOrder_UserNotFound() {
        // Given
        given(userRepository.findById("user123")).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder("user123", testRequest))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("사용자를 찾을 수 없습니다: user123");

        // 주문 저장 및 이벤트 발행이 일어나지 않음
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("❌ 상품 없음 - 주문 생성 실패")
    void createOrder_ProductNotFound() {
        // Given
        given(userRepository.findById("user123")).willReturn(Optional.of(testUser));
        given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
        given(productRepository.findById("product2")).willReturn(Optional.empty()); // 두 번째 상품 없음

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder("user123", testRequest))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("상품을 찾을 수 없습니다: product2");

        // 주문 저장 및 이벤트 발행이 일어나지 않음
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("❌ 빈 주문 아이템 - 검증 실패")
    void createOrder_EmptyOrderItems() {
        // Given
        OrderCreateRequest emptyRequest = OrderCreateRequest.builder()
                .orderItems(List.of())
                .build();

        given(userRepository.findById("user123")).willReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder("user123", emptyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주문 아이템이 비어있습니다.");

        // 주문 저장 및 이벤트 발행이 일어나지 않음
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("✅ 이벤트 데이터 정확성 검증")
    void createOrder_EventDataAccuracy() {
        // Given
        given(userRepository.findById("user123")).willReturn(Optional.of(testUser));
        given(productRepository.findById("product1")).willReturn(Optional.of(testProduct1));
        given(productRepository.findById("product2")).willReturn(Optional.of(testProduct2));
        given(orderRepository.save(any(Orders.class))).willReturn(testOrder);

        // When
        orderService.createOrder("user123", testRequest);

        // Then - 이벤트 내용 상세 검증
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        OrderCreatedEvent event = eventCaptor.getValue();

        // 기본 정보 검증
        assertThat(event.getOrderId()).isEqualTo("order123");
        assertThat(event.getUserId()).isEqualTo("user123");
        assertThat(event.getUserName()).isEqualTo("김철수");

        // 주문 아이템 검증
        List<OrderCreatedEvent.OrderItemInfo> eventItems = event.getOrderItems();
        assertThat(eventItems).hasSize(2);

        // 첫 번째 아이템
        OrderCreatedEvent.OrderItemInfo item1 = eventItems.get(0);
        assertThat(item1.getProductId()).isEqualTo("product1");
        assertThat(item1.getProductName()).isEqualTo("강아지 사료");
        assertThat(item1.getQuantity()).isEqualTo(2);
        assertThat(item1.getUnitPrice()).isEqualTo(25000L);

        // 두 번째 아이템
        OrderCreatedEvent.OrderItemInfo item2 = eventItems.get(1);
        assertThat(item2.getProductId()).isEqualTo("product2");
        assertThat(item2.getProductName()).isEqualTo("고양이 간식");
        assertThat(item2.getQuantity()).isEqualTo(1);
        assertThat(item2.getUnitPrice()).isEqualTo(5000L);

        // 시간 정보 검증
        assertThat(event.getEventTime()).isNotNull();
    }
}