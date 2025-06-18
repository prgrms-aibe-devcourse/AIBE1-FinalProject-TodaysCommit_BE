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
import org.springframework.test.annotation.DirtiesContext;
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
 * ✅ UUID 랜덤화로 충돌 방지
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("dev") // 🔧 수정: test → dev
@Transactional // 테스트 후 자동 롤백
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // 🔧 추가: 테스트 간 격리
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
    private String testUserId; // 🔧 추가: 동적 사용자 ID

    @BeforeEach
    void setUp() {
        // 🔧 수정: UUID를 랜덤하게 생성하여 충돌 방지
        UUID randomUserId = UUID.randomUUID();
        testUserId = randomUserId.toString();

        // 🔧 실제 Users 엔티티 구조에 맞게 수정
        testUser = Users.builder()
                .id(randomUserId) // 🔧 수정: 랜덤 UUID 사용
                .provider("GOOGLE")
                .providerId("test_provider_id_" + System.currentTimeMillis()) // 🔧 추가: 시간 기반 고유값
                .userNameAttribute("test_user_attr_" + randomUserId.toString().substring(0, 8))
                .name("테스트 사용자")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .build();

        // 🔧 수정: saveAndFlush 사용으로 즉시 DB 반영
        userRepository.saveAndFlush(testUser);

        // 테스트용 주문 요청 데이터
        validRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product-1") // Mock 데이터와 일치 (프리미엄 강아지 사료)
                                .quantity(2)
                                .build()
                ))
                .shippingAddress(OrderCreateRequest.ShippingAddressRequest.builder()
                        .recipientName("김철수")
                        .recipientPhone("010-1234-5678")
                        .postalCode("12345")
                        .streetAddress("서울시 강남구 테헤란로 123")
                        .detailAddress("456호")
                        .deliveryNote("문 앞에 놓아주세요")
                        .build())
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                        .orderName("반려동물 용품 주문")
                        .customerEmail("test@catdogeats.com")
                        .customerName("김철수")
                        .build())
                .build();
    }

    @Test
    @DisplayName("✅ 정상적인 주문 생성 - 실제 주문까지 완료")
    void createOrder_Success() throws Exception {
        // 🔧 수정: 동적 사용자 ID 사용
        String mockUserAnnotation = "@WithMockUser(username = \"" + testUserId + "\", roles = \"BUYER\")";
        System.out.println("🔧 현재 테스트 사용자 ID: " + testUserId);

        // When & Then: 주문 생성 성공
        MvcResult result = mockMvc.perform(post("/v1/buyers/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .header("X-User-ID", testUserId)) // 🔧 추가: 헤더로 사용자 ID 전달
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.totalPrice").value(50000)) // 25000 * 2 = 50000
                .andExpect(jsonPath("$.orderItems.length()").value(1))
                .andExpect(jsonPath("$.orderItems[0].productName").value("프리미엄 강아지 사료"))
                .andExpect(jsonPath("$.orderItems[0].quantity").value(2))
                .andExpect(jsonPath("$.orderItems[0].unitPrice").value(25000))
                .andReturn();

        // 응답 검증
        String responseContent = result.getResponse().getContentAsString();
        OrderCreateResponse response = objectMapper.readValue(responseContent, OrderCreateResponse.class);

        assertThat(response.getOrderId()).isNotNull();
        assertThat(response.getTotalPrice()).isEqualTo(50000);
        assertThat(response.getOrderItems()).hasSize(1);

        System.out.println("✅ 주문 생성 성공 - Order ID: " + response.getOrderId());
    }

    @Test
    @DisplayName("🔍 실제 DB에 저장된 사용자 정보 확인")
    void verifyTestUserInDatabase() throws Exception {
        // Given: DB에서 사용자 조회
        Users foundUser = userRepository.findById(testUser.getId()).orElse(null);

        // Then: 사용자 정보 검증
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getName()).isEqualTo("테스트 사용자");
        assertThat(foundUser.getProvider()).isEqualTo("GOOGLE");
        assertThat(foundUser.getProviderId()).startsWith("test_provider_id_"); // 🔧 수정: 동적 값
        assertThat(foundUser.getRole()).isEqualTo(Role.ROLE_BUYER);
        assertThat(foundUser.isAccountDisable()).isFalse();

        System.out.println("👤 DB에 저장된 사용자 정보:");
        System.out.println("   - ID: " + foundUser.getId());
        System.out.println("   - 이름: " + foundUser.getName());
        System.out.println("   - 제공자: " + foundUser.getProvider());
        System.out.println("   - 역할: " + foundUser.getRole());
        System.out.println("   - 계정 비활성화: " + foundUser.isAccountDisable());

        // 🔧 수정: 동적 사용자 ID로 주문 생성 테스트
        mockMvc.perform(post("/v1/buyers/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .header("X-User-ID", testUserId))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists());
    }

    @Test
    @DisplayName("❌ 존재하지 않는 사용자 - 실제 에러 테스트")
    void createOrder_UserNotFound() throws Exception {
        // Given: 존재하지 않는 사용자 ID
        String nonExistentUserId = UUID.randomUUID().toString();

        // When & Then: 사용자 없음 에러 발생
        mockMvc.perform(post("/v1/buyers/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .header("X-User-ID", nonExistentUserId))
                .andDo(print())
                .andExpect(status().is5xxServerError()); // NoSuchElementException으로 인한 500 에러
    }

    @Test
    @DisplayName("❌ 잘못된 수량으로 주문 - 비즈니스 로직 검증")
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
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .header("X-User-ID", testUserId))
                .andDo(print())
                .andExpect(status().is5xxServerError()); // IllegalArgumentException으로 인한 에러
    }

    @Test
    @DisplayName("🎯 Mock상품 데이터 검증 - 여러 상품 주문")
    void createOrder_MultipleProducts() throws Exception {
        // Given: 여러 상품 주문 요청 (Mock 상품 ID들 사용)
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
                                .productId("product-4") // 일반 반려동물 용품 (10000원)
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
                        .content(objectMapper.writeValueAsString(multiProductRequest))
                        .header("X-User-ID", testUserId))
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
}