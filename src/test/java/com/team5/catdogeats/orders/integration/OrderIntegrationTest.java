package com.team5.catdogeats.orders.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("주문 통합 테스트")
class OrderIntegrationTest {

    // 테스트 환경에서만 사용될 트랜잭션 매니저 설정
    @TestConfiguration
    static class OrderIntegrationTestConfig {

        // @Primary 어노테이션을 사용하여 이 Bean을 테스트 환경의 기본 트랜잭션 매니저로 지정합니다.
        @Bean
        @Primary
        public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }
    }


    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SellersRepository sellersRepository;
    @Autowired
    private ProductRepository productRepository;

    private Users testUser;
    private OrderCreateRequest validRequest;
    private UsernamePasswordAuthenticationToken testAuthentication;
    private Products product1, product2, product3;

    @BeforeEach
    void setUp() {
        Users sellerUser = userRepository.save(Users.builder().provider("DUMMY").providerId("seller-id").userNameAttribute("dummy").name("판매자").role(Role.ROLE_SELLER).accountDisable(false).build());
        Sellers seller = sellersRepository.save(Sellers.builder().user(sellerUser).vendorName("테스트 상점").businessNumber("123-45-67890").build());

        testUser = Users.builder()
                .provider("GOOGLE")
                .providerId("test_provider_id_" + System.currentTimeMillis())
                .userNameAttribute("test_user_attr")
                .name("테스트 사용자")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .build();
        testUser = userRepository.saveAndFlush(testUser);

        product1 = productRepository.save(Products.builder().seller(seller).title("프리미엄 강아지 사료").contents("상품 상세 내용 1").productNumber(1001L).price(25000L).quantity(100).build());
        product2 = productRepository.save(Products.builder().seller(seller).title("유기농 고양이 사료").contents("상품 상세 내용 2").productNumber(1002L).price(15000L).quantity(50).build());
        product3 = productRepository.save(Products.builder().seller(seller).title("수제 강아지 간식").contents("상품 상세 내용 3").productNumber(1003L).price(8000L).quantity(30).build());

        UserPrincipal userPrincipal = new UserPrincipal(testUser.getProvider(), testUser.getProviderId());
        testAuthentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                Collections.singletonList(new SimpleGrantedAuthority(testUser.getRole().toString()))
        );

        validRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(product1.getId().toString())
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

        // --- 여기부터 수정 ---
        MvcResult result = mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderId").exists()) // JSON Path 수정
                .andExpect(jsonPath("$.data.totalPrice").value(50000)) // JSON Path 수정
                .andReturn();

        // ObjectMapper로 응답을 파싱하여 객체 검증
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseNode = objectMapper.readTree(responseContent);
        OrderCreateResponse response = objectMapper.treeToValue(responseNode.get("data"), OrderCreateResponse.class);

        assertThat(response.getOrderId()).isNotNull();
        assertThat(response.getTotalPrice()).isEqualTo(50000);
        // --- 여기까지 수정 ---
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
        UserPrincipal nonExistentUserPrincipal = new UserPrincipal("DUMMY_PROVIDER", "non-existent-user-id");
        UsernamePasswordAuthenticationToken nonExistentUserAuth = new UsernamePasswordAuthenticationToken(
                nonExistentUserPrincipal,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER"))
        );

        mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(nonExistentUserAuth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("❌ 잘못된 수량으로 주문 - 비즈니스 로직 검증")
    void createOrder_InvalidQuantity() throws Exception {
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(product1.getId().toString())
                                .quantity(0)
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
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("🎯 Mock 상품 데이터 검증 - 여러 상품 주문")
    void createOrder_MultipleProducts() throws Exception {
        OrderCreateRequest multiProductRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder().productId(product1.getId().toString()).quantity(1).build(),
                        OrderCreateRequest.OrderItemRequest.builder().productId(product2.getId().toString()).quantity(1).build(),
                        OrderCreateRequest.OrderItemRequest.builder().productId(product3.getId().toString()).quantity(1).build()
                ))
                .shippingAddress(validRequest.getShippingAddress())
                .paymentInfo(validRequest.getPaymentInfo())
                .build();

        // --- 여기부터 수정 ---
        mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(multiProductRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalPrice").value(48000)) // JSON Path 수정
                .andExpect(jsonPath("$.data.orderItems.length()").value(3)); // JSON Path 수정
        // --- 여기까지 수정 ---
    }
}