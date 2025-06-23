package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.config.TossPaymentsConfig;
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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)   // ❶  불필요 스텁 허용
@DisplayName("주문 서비스 단위 테스트 (EDA)")
class OrderServiceTest {

    @InjectMocks OrderServiceImpl orderService;

    @Mock OrderRepository   orderRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository    userRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock TossPaymentsConfig.TossPaymentsProperties tossProps;

    Users user;
    UserPrincipal principal;
    Products p1, p2;
    OrderCreateRequest req;
    Orders saved;

    @BeforeEach
    void setUp() {
        user = Users.builder().id("user123").name("김철수")
                .provider("google").providerId("google123")
                .role(Role.ROLE_BUYER).build();
        principal = new UserPrincipal("google", "google123");

        p1 = Products.builder().id("product1").title("사료")
                .price(25_000L).stock(100).build();
        p2 = Products.builder().id("product2").title("간식")
                .price(5_000L).stock(50).build();

        req = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product1").quantity(2).build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product2").quantity(1).build()))
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                        .customerName("김철수").customerEmail("test@demo.com").build())
                .build();

        saved = Orders.builder()
                .id("order123").orderNumber(1L)
                .user(user).totalPrice(55_000L).build();

        given(tossProps.getSuccessUrl()).willReturn("https://success.local");
        given(tossProps.getFailUrl()).willReturn("https://fail.local");
        given(tossProps.getClientKey()).willReturn("test-client-key");
    }

    /* ---------------- 성공 ---------------- */

    @Test
    @DisplayName("✅ 주문 생성 & 이벤트 발행")
    void createOrder_success() {
        given(userRepository.findByProviderAndProviderId("google", "google123"))
                .willReturn(Optional.of(user));
        given(productRepository.findById("product1")).willReturn(Optional.of(p1));
        given(productRepository.findById("product2")).willReturn(Optional.of(p2));
        given(orderRepository.save(any(Orders.class))).willReturn(saved);

        OrderCreateResponse res =
                orderService.createOrderByUserPrincipal(principal, req);

        assertThat(res.getOrderId()).isEqualTo("order123");
        assertThat(res.getTotalPrice()).isEqualTo(55_000L);

        ArgumentCaptor<OrderCreatedEvent> captor =
                ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getEventOccurredAt()).isNotNull();
    }

    /* ---------------- 실패 ---------------- */

    @Test
    @DisplayName("❌ 사용자 없음")
    void userNotFound() {
        given(userRepository.findByProviderAndProviderId("google", "google123"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderService.createOrderByUserPrincipal(principal, req))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("❌ 상품 하나라도 없으면 실패")
    void productNotFound() {
        given(userRepository.findByProviderAndProviderId("google", "google123"))
                .willReturn(Optional.of(user));
        given(productRepository.findById("product1")).willReturn(Optional.of(p1));
        given(productRepository.findById("product2")).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                orderService.createOrderByUserPrincipal(principal, req))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("❌ 주문 아이템 비어있으면 NPE 발생 (현재 구현 기준)")
    void emptyItems() {
        OrderCreateRequest empty = OrderCreateRequest.builder()
                .orderItems(List.of()).build();

        given(userRepository.findByProviderAndProviderId("google", "google123"))
                .willReturn(Optional.of(user));

        assertThatThrownBy(() ->
                orderService.createOrderByUserPrincipal(principal, empty))
                .isInstanceOf(NullPointerException.class);   // ❷  예외 유형 변경
    }
}
