// prgrms-aibe-devcourse/aibe1-finalproject-todayscommit_be/AIBE1-FinalProject-TodaysCommit_BE-feat-order-creation-10/src/test/java/com/team5/catdogeats/orders/integration/OrderIntegrationTest.java

package com.team5.catdogeats.orders.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
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
    private UsernamePasswordAuthenticationToken testAuthentication; // 인증 객체

    @BeforeEach
    void setUp() {
        // 1. 테스트 사용자 생성 및 DB에 저장
        testUser = Users.builder()
                .provider("GOOGLE")
                .providerId("test_provider_id_" + System.currentTimeMillis())
                .userNameAttribute("test_user_attr")
                .name("테스트 사용자")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .build();
        testUser = userRepository.saveAndFlush(testUser);

        // 2. 동적으로 생성된 사용자 ID로 인증 객체 생성
        testAuthentication = new UsernamePasswordAuthenticationToken(
                testUser.getId().toString(), // Principal의 이름으로 UUID를 사용
                null,
                Collections.singletonList(new SimpleGrantedAuthority(testUser.getRole().toString()))
        );

        // 3. 테스트용 주문 요청 데이터 생성
        validRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId("product-1") // Mock 데이터: 프리미엄 강아지 사료
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
        System.out.println("🔧 현재 테스트 사용자 ID: " + testUser.getId());

        MvcResult result = mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication)) // 수정: 동적 인증 정보 주입
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.totalPrice").value(50000)) // 25000 * 2
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        OrderCreateResponse response = objectMapper.readValue(responseContent, OrderCreateResponse.class);
        assertThat(response.getOrderId()).isNotNull();
        assertThat(response.getTotalPrice()).isEqualTo(50000);
    }

    @Test
    @DisplayName("🔍 실제 DB에 저장된 사용자 정보 확인")
    void verifyTestUserInDatabase() {
        Users foundUser = userRepository.findById(testUser.getId()).orElse(null);

        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(testUser.getId());
        assertThat(foundUser.getName()).isEqualTo("테스트 사용자");
        assertThat(foundUser.getRole()).isEqualTo(Role.ROLE_BUYER);

        System.out.println("👤 DB에 저장된 사용자 정보 확인 완료: " + foundUser.getId());
    }

    @Test
    @DisplayName("❌ 존재하지 않는 사용자 - 실제 에러 테스트")
    void createOrder_UserNotFound() throws Exception {
        // 존재하지 않는 UUID로 인증 객체 생성
        UsernamePasswordAuthenticationToken nonExistentUserAuth = new UsernamePasswordAuthenticationToken(
                UUID.randomUUID().toString(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER"))
        );

        mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(nonExistentUserAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isNotFound()); // HTTP 500이 아닌 404 Not Found를 기대
    }

    @Test
    @DisplayName("❌ 잘못된 수량으로 주문 - 비즈니스 로직 검증")
    void createOrder_InvalidQuantity() throws Exception {
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

        mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // HTTP 500이 아닌 400 Bad Request를 기대
    }

    @Test
    @DisplayName("🎯 Mock 상품 데이터 검증 - 여러 상품 주문")
    void createOrder_MultipleProducts() throws Exception {
        OrderCreateRequest multiProductRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder().productId("product-1").quantity(1).build(), // 25000원
                        OrderCreateRequest.OrderItemRequest.builder().productId("product-2").quantity(1).build(), // 15000원
                        OrderCreateRequest.OrderItemRequest.builder().productId("product-3").quantity(1).build()  // 8000원
                ))
                .shippingAddress(validRequest.getShippingAddress())
                .paymentInfo(validRequest.getPaymentInfo())
                .build();

        mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication)) // 수정: 동적 인증 정보 주입
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(multiProductRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPrice").value(48000)) // 25000 + 15000 + 8000
                .andExpect(jsonPath("$.orderItems.length()").value(3));
    }
}