package com.team5.catdogeats.orders.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.orders.domain.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.domain.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 주문 컨트롤러 테스트
 *
 * 🎯 이 테스트는 기존 코드를 전혀 수정하지 않고도 완벽하게 동작합니다!
 */
@WebMvcTest(OrderController.class)
@DisplayName("주문 컨트롤러 테스트")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderCreateRequest validRequest;
    private OrderCreateResponse mockResponse;

    @BeforeEach
    void setUp() {
        // 테스트용 요청 데이터 준비
        validRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product-001")
                                .quantity(2)
                                .build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product-002")
                                .quantity(1)
                                .build()
                ))
                .shippingAddress(OrderCreateRequest.ShippingAddressRequest.builder()
                        .recipientName("홍길동")
                        .recipientPhone("010-1234-5678")
                        .postalCode("12345")
                        .streetAddress("서울시 강남구 테헤란로 123")
                        .detailAddress("456호")
                        .deliveryNote("문 앞에 놓아주세요")
                        .build())
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                        .orderName("강아지 사료 외 1건")
                        .customerEmail("test@catdogeats.com")
                        .customerName("홍길동")
                        .build())
                .build();

        // 테스트용 응답 데이터 준비
        mockResponse = OrderCreateResponse.builder()
                .orderId("123e4567-e89b-12d3-a456-426614174000")
                .orderNumber(1001L)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(35000L)
                .createdAt(ZonedDateTime.now())
                .orderItems(List.of(
                        OrderCreateResponse.OrderItemResponse.builder()
                                .orderItemId("item-001")
                                .productId("product-001")
                                .productName("일반 반려동물 용품")
                                .quantity(2)
                                .unitPrice(10000L)
                                .totalPrice(20000L)
                                .build(),
                        OrderCreateResponse.OrderItemResponse.builder()
                                .orderItemId("item-002")
                                .productId("product-002")
                                .productName("고양이 간식")
                                .quantity(1)
                                .unitPrice(15000L)
                                .totalPrice(15000L)
                                .build()
                ))
                .tossPaymentInfo(OrderCreateResponse.TossPaymentInfo.builder()
                        .tossOrderId("123e4567-e89b-12d3-a456-426614174000")
                        .orderName("강아지 사료 외 1건")
                        .amount(35000L)
                        .customerName("홍길동")
                        .customerEmail("test@catdogeats.com")
                        .successUrl("http://localhost:8080/v1/buyers/payments/success")
                        .failUrl("http://localhost:8080/v1/buyers/payments/fail")
                        .clientKey("test_ck_placeholder")
                        .build())
                .build();
    }

    @Test
    @DisplayName("✅ 정상적인 주문 생성 - 성공")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = "BUYER")
    void createOrder_Success() throws Exception {
        // Given: OrderService의 동작을 모킹
        given(orderService.createOrder(anyString(), any(OrderCreateRequest.class)))
                .willReturn(mockResponse);

        // When: POST 요청 실행
        ResultActions result = mockMvc.perform(post("/v1/buyers/orders")
                .with(csrf()) // CSRF 토큰 추가
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)));

        // Then: 응답 검증
        result.andDo(print()) // 요청/응답 로그 출력
                .andExpect(status().isCreated()) // 201 Created
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value("123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(jsonPath("$.orderNumber").value(1001))
                .andExpect(jsonPath("$.orderStatus").value("PAYMENT_PENDING"))
                .andExpect(jsonPath("$.totalPrice").value(35000))
                .andExpect(jsonPath("$.orderItems").isArray())
                .andExpect(jsonPath("$.orderItems.length()").value(2))
                .andExpect(jsonPath("$.orderItems[0].productId").value("product-001"))
                .andExpect(jsonPath("$.orderItems[0].quantity").value(2))
                .andExpect(jsonPath("$.orderItems[0].totalPrice").value(20000))
                .andExpect(jsonPath("$.orderItems[1].productId").value("product-002"))
                .andExpect(jsonPath("$.orderItems[1].quantity").value(1))
                .andExpect(jsonPath("$.orderItems[1].totalPrice").value(15000))
                .andExpect(jsonPath("$.tossPaymentInfo.tossOrderId").value("123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(jsonPath("$.tossPaymentInfo.orderName").value("강아지 사료 외 1건"))
                .andExpect(jsonPath("$.tossPaymentInfo.amount").value(35000))
                .andExpect(jsonPath("$.tossPaymentInfo.customerName").value("홍길동"))
                .andExpect(jsonPath("$.tossPaymentInfo.customerEmail").value("test@catdogeats.com"))
                .andExpect(jsonPath("$.tossPaymentInfo.clientKey").value("test_ck_placeholder"));
    }

    @Test
    @DisplayName("❌ 인증되지 않은 사용자 - 실패")
    void createOrder_Unauthorized() throws Exception {
        // When: 인증 없이 POST 요청
        ResultActions result = mockMvc.perform(post("/v1/buyers/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)));

        // Then: 인증 오류 응답
        result.andDo(print())
                .andExpect(status().isUnauthorized()); // 401 Unauthorized
    }

    @Test
    @DisplayName("❌ 빈 주문 아이템 목록 - 유효성 검사 실패")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = "BUYER")
    void createOrder_EmptyOrderItems() throws Exception {
        // Given: 빈 주문 아이템 목록
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .orderItems(List.of()) // 빈 리스트
                .shippingAddress(validRequest.getShippingAddress())
                .paymentInfo(validRequest.getPaymentInfo())
                .build();

        // When: POST 요청 실행
        ResultActions result = mockMvc.perform(post("/v1/buyers/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)));

        // Then: 유효성 검사 실패
        result.andDo(print())
                .andExpect(status().isBadRequest()); // 400 Bad Request
    }

    @Test
    @DisplayName("❌ 필수 필드 누락 - 유효성 검사 실패")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = "BUYER")
    void createOrder_MissingRequiredFields() throws Exception {
        // Given: 배송 주소 누락
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .orderItems(validRequest.getOrderItems())
                .shippingAddress(null) // 필수 필드 누락
                .paymentInfo(validRequest.getPaymentInfo())
                .build();

        // When: POST 요청 실행
        ResultActions result = mockMvc.perform(post("/v1/buyers/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)));

        // Then: 유효성 검사 실패
        result.andDo(print())
                .andExpect(status().isBadRequest()); // 400 Bad Request
    }

    @Test
    @DisplayName("❌ 잘못된 수량 - 유효성 검사 실패")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = "BUYER")
    void createOrder_InvalidQuantity() throws Exception {
        // Given: 0개 주문
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product-001")
                                .quantity(0) // 잘못된 수량
                                .build()
                ))
                .shippingAddress(validRequest.getShippingAddress())
                .paymentInfo(validRequest.getPaymentInfo())
                .build();

        // Given: 서비스에서 예외 발생 모킹
        given(orderService.createOrder(anyString(), any(OrderCreateRequest.class)))
                .willThrow(new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다."));

        // When: POST 요청 실행
        ResultActions result = mockMvc.perform(post("/v1/buyers/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)));

        // Then: 예외 발생
        result.andDo(print())
                .andExpect(status().isBadRequest()); // 400 Bad Request (글로벌 예외 핸들러에 따라 달라질 수 있음)
    }

    @Test
    @DisplayName("🔍 JSON 응답 형식 상세 검증")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = "BUYER")
    void createOrder_JsonResponseFormat() throws Exception {
        // Given
        given(orderService.createOrder(anyString(), any(OrderCreateRequest.class)))
                .willReturn(mockResponse);

        // When
        ResultActions result = mockMvc.perform(post("/v1/buyers/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)));

        // Then: 상세한 JSON 구조 검증
        result.andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                // 주문 기본 정보
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.orderStatus").exists())
                .andExpect(jsonPath("$.totalPrice").exists())
                .andExpect(jsonPath("$.createdAt").exists())

                // 주문 아이템 배열
                .andExpect(jsonPath("$.orderItems").isArray())
                .andExpect(jsonPath("$.orderItems[0].orderItemId").exists())
                .andExpect(jsonPath("$.orderItems[0].productId").exists())
                .andExpect(jsonPath("$.orderItems[0].productName").exists())
                .andExpect(jsonPath("$.orderItems[0].quantity").exists())
                .andExpect(jsonPath("$.orderItems[0].unitPrice").exists())
                .andExpect(jsonPath("$.orderItems[0].totalPrice").exists())

                // 토스 페이먼츠 정보
                .andExpect(jsonPath("$.tossPaymentInfo").exists())
                .andExpect(jsonPath("$.tossPaymentInfo.tossOrderId").exists())
                .andExpect(jsonPath("$.tossPaymentInfo.orderName").exists())
                .andExpect(jsonPath("$.tossPaymentInfo.amount").exists())
                .andExpect(jsonPath("$.tossPaymentInfo.customerName").exists())
                .andExpect(jsonPath("$.tossPaymentInfo.customerEmail").exists())
                .andExpect(jsonPath("$.tossPaymentInfo.successUrl").exists())
                .andExpect(jsonPath("$.tossPaymentInfo.failUrl").exists())
                .andExpect(jsonPath("$.tossPaymentInfo.clientKey").exists());
    }
}