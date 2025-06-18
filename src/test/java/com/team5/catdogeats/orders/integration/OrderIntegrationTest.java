package com.team5.catdogeats.orders.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.orders.domain.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.domain.dto.response.OrderCreateResponse;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 주문 통합 테스트 (수정됨)
 *
 * 🎯 실제 Service, Repository까지 모두 동작하는 완전한 통합 테스트
 * ✅ Spring Boot 3.5.0 호환 버전
 * ✅ 실제 Users 엔티티 구조에 맞게 수정
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc // 🔧 수정: @AutoConfigureWebMvc → @AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // 테스트 후 자동 롤백
@DisplayName("주문 통합 테스트")
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private Users testUser;
    private OrderCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        // 🔧 실제 Users 엔티티 구조에 맞게 수정
        testUser = Users.builder()
                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .provider("GOOGLE") // email 대신 provider 사용
                .providerId("test_provider_id_123") // providerId 추가
                .userNameAttribute("test_user_attr") // userNameAttribute 추가
                .name("테스트 사용자")
                .role(Role.ROLE_BUYER) // 🔧 수정: BUYER → ROLE_BUYER
                .accountDisable(false)
                .build();

        // 사용자 저장 (실제 DB에 저장됨)
        userRepository.save(testUser);

        // 테스트용 주문 요청 데이터
        validRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product-1") // Mock 데이터와 일치 (프리미엄 강아지 사료)
                                .quantity(1)
                                .build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product-2") // Mock 데이터와 일치 (고양이 간식)
                                .quantity(2)
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
    }

    @Test
    @DisplayName("✅ 실제 주문 생성 - 전체 플로우 테스트")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = "BUYER")
    void createOrder_FullIntegration() throws Exception {
        // When: 실제 주문 생성 요청
        MvcResult result = mockMvc.perform(post("/v1/buyers/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())

                // Then: HTTP 응답 검증
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.orderStatus").value("PAYMENT_PENDING"))
                .andExpect(jsonPath("$.totalPrice").value(55000)) // 25000*1 + 15000*2 = 55000
                .andExpect(jsonPath("$.orderItems").isArray())
                .andExpect(jsonPath("$.orderItems.length()").value(2))

                // 첫 번째 상품 검증 (product-1 = 프리미엄 강아지 사료)
                .andExpect(jsonPath("$.orderItems[0].productId").value("product-1"))
                .andExpect(jsonPath("$.orderItems[0].productName").value("프리미엄 강아지 사료"))
                .andExpect(jsonPath("$.orderItems[0].quantity").value(1))
                .andExpect(jsonPath("$.orderItems[0].unitPrice").value(25000))
                .andExpect(jsonPath("$.orderItems[0].totalPrice").value(25000))

                // 두 번째 상품 검증 (product-2 = 고양이 간식)
                .andExpect(jsonPath("$.orderItems[1].productId").value("product-2"))
                .andExpect(jsonPath("$.orderItems[1].productName").value("고양이 간식"))
                .andExpect(jsonPath("$.orderItems[1].quantity").value(2))
                .andExpect(jsonPath("$.orderItems[1].unitPrice").value(15000))
                .andExpect(jsonPath("$.orderItems[1].totalPrice").value(30000))

                // 토스 페이먼츠 정보 검증
                .andExpect(jsonPath("$.tossPaymentInfo.orderName").value("강아지 사료 외 1건"))
                .andExpect(jsonPath("$.tossPaymentInfo.amount").value(55000))
                .andExpect(jsonPath("$.tossPaymentInfo.customerName").value("홍길동"))
                .andExpect(jsonPath("$.tossPaymentInfo.customerEmail").value("test@catdogeats.com"))
                .andExpect(jsonPath("$.tossPaymentInfo.successUrl").value("http://localhost:8080/v1/buyers/payments/success"))
                .andExpect(jsonPath("$.tossPaymentInfo.failUrl").value("http://localhost:8080/v1/buyers/payments/fail"))
                .andExpect(jsonPath("$.tossPaymentInfo.clientKey").value("test_ck_placeholder"))

                .andReturn();

        // 응답 JSON을 객체로 변환하여 추가 검증
        String responseJson = result.getResponse().getContentAsString();
        OrderCreateResponse response = objectMapper.readValue(responseJson, OrderCreateResponse.class);

        // 추가 검증: 비즈니스 로직 확인
        assertThat(response.getOrderId()).isNotNull();
        assertThat(response.getOrderNumber()).isGreaterThan(0);
        assertThat(response.getTotalPrice()).isEqualTo(55000L);
        assertThat(response.getOrderItems()).hasSize(2);
        assertThat(response.getCreatedAt()).isNotNull();

        // UUID 형식 검증
        assertThat(UUID.fromString(response.getOrderId())).isNotNull();
        assertThat(UUID.fromString(response.getTossPaymentInfo().getTossOrderId())).isNotNull();

        System.out.println("🎉 주문 생성 완료!");
        System.out.println("📦 주문 ID: " + response.getOrderId());
        System.out.println("🔢 주문 번호: " + response.getOrderNumber());
        System.out.println("💰 총 금액: " + response.getTotalPrice() + "원");
        System.out.println("📋 주문 상품 수: " + response.getOrderItems().size() + "개");
        System.out.println("👤 테스트 사용자: " + testUser.getName() + " (" + testUser.getProvider() + ")");
    }

    @Test
    @DisplayName("✅ Mock 상품 데이터 검증 - 다양한 상품 테스트")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = "BUYER")
    void testMockProducts() throws Exception {
        // Given: 다양한 Mock 상품들로 주문 생성
        OrderCreateRequest multiProductRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product-1") // 프리미엄 강아지 사료 (25000원)
                                .quantity(1)
                                .build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product-2") // 고양이 간식 (15000원)
                                .quantity(1)
                                .build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product-3") // 반려동물 장난감 (8000원)
                                .quantity(1)
                                .build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("unknown-product") // 기본 상품 (10000원)
                                .quantity(1)
                                .build()
                ))
                .shippingAddress(validRequest.getShippingAddress())
                .paymentInfo(validRequest.getPaymentInfo())
                .build();

        // When & Then: 모든 Mock 상품이 올바르게 처리되는지 확인
        mockMvc.perform(post("/v1/buyers/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(multiProductRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPrice").value(58000)) // 25000+15000+8000+10000 = 58000
                .andExpect(jsonPath("$.orderItems.length()").value(4))

                // 각 상품별 검증
                .andExpect(jsonPath("$.orderItems[0].productName").value("프리미엄 강아지 사료"))
                .andExpect(jsonPath("$.orderItems[0].unitPrice").value(25000))

                .andExpect(jsonPath("$.orderItems[1].productName").value("고양이 간식"))
                .andExpect(jsonPath("$.orderItems[1].unitPrice").value(15000))

                .andExpect(jsonPath("$.orderItems[2].productName").value("반려동물 장난감"))
                .andExpect(jsonPath("$.orderItems[2].unitPrice").value(8000))

                .andExpect(jsonPath("$.orderItems[3].productName").value("일반 반려동물 용품"))
                .andExpect(jsonPath("$.orderItems[3].unitPrice").value(10000));
    }

    @Test
    @DisplayName("❌ 존재하지 않는 사용자 - 실제 에러 테스트")
    @WithMockUser(username = "00000000-0000-0000-0000-000000000000", roles = "BUYER") // 존재하지 않는 사용자
    void createOrder_UserNotFound() throws Exception {
        // When & Then: 사용자 없음 에러 발생
        mockMvc.perform(post("/v1/buyers/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().is5xxServerError()); // NoSuchElementException으로 인한 500 에러
    }

    @Test
    @DisplayName("❌ 잘못된 수량으로 주문 - 비즈니스 로직 검증")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = "BUYER")
    void createOrder_InvalidQuantity() throws Exception {
        // Given: 수량이 0인 잘못된 요청
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product-1")
                                .quantity(0) // 잘못된 수량
                                .build()
                ))
                .shippingAddress(validRequest.getShippingAddress())
                .paymentInfo(validRequest.getPaymentInfo())
                .build();

        // When & Then: 비즈니스 로직에서 예외 발생
        mockMvc.perform(post("/v1/buyers/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().is5xxServerError()); // IllegalArgumentException으로 인한 에러
    }

    @Test
    @DisplayName("🔍 실제 DB에 저장된 사용자 정보 확인")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = "BUYER")
    void verifyTestUserInDatabase() throws Exception {
        // Given: DB에서 사용자 조회
        Users foundUser = userRepository.findById(testUser.getId()).orElse(null);

        // Then: 사용자 정보 검증
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getName()).isEqualTo("테스트 사용자");
        assertThat(foundUser.getProvider()).isEqualTo("GOOGLE");
        assertThat(foundUser.getProviderId()).isEqualTo("test_provider_id_123");
        assertThat(foundUser.getRole()).isEqualTo(Role.ROLE_BUYER);
        assertThat(foundUser.isAccountDisable()).isFalse();

        System.out.println("👤 DB에 저장된 사용자 정보:");
        System.out.println("   - ID: " + foundUser.getId());
        System.out.println("   - 이름: " + foundUser.getName());
        System.out.println("   - 제공자: " + foundUser.getProvider());
        System.out.println("   - 역할: " + foundUser.getRole());
        System.out.println("   - 계정 비활성화: " + foundUser.isAccountDisable());

        // 실제 주문 생성도 테스트
        mockMvc.perform(post("/v1/buyers/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists());
    }
}