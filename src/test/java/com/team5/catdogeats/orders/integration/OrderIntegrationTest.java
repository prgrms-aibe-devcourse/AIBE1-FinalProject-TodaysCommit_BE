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

/**
 * 주문 통합 테스트 (수정된 DTO 구조 적용)
 *
 * 실제 DTO 필드명에 맞게 테스트 코드를 수정하였습니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("주문 통합 테스트")
class OrderIntegrationTest {

    /**
     * 테스트 환경에서만 사용될 트랜잭션 매니저 설정
     */
    @TestConfiguration
    static class OrderIntegrationTestConfig {

        /**
         * @Primary 어노테이션을 사용하여 이 Bean을 테스트 환경의 기본 트랜잭션 매니저로 지정합니다.
         */
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
        // 판매자 사용자 및 판매자 정보 생성
        Users sellerUser = userRepository.save(Users.builder()
                .provider("DUMMY")
                .providerId("seller-id")
                .userNameAttribute("dummy")
                .name("판매자")
                .role(Role.ROLE_SELLER)
                .accountDisable(false)
                .build());

        Sellers seller = sellersRepository.save(Sellers.builder()
                .user(sellerUser)
                .vendorName("테스트 상점")
                .businessNumber("123-45-67890")
                .build());

        // 테스트 구매자 사용자 생성
        testUser = Users.builder()
                .provider("GOOGLE")
                .providerId("test_provider_id_" + System.currentTimeMillis())
                .userNameAttribute("test_user_attr")
                .name("테스트 사용자")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .build();
        testUser = userRepository.saveAndFlush(testUser);

        // 테스트 상품들 생성 (stock 필드 사용)
        product1 = productRepository.save(Products.builder()
                .seller(seller)
                .title("프리미엄 강아지 사료")
                .contents("상품 상세 내용 1")
                .productNumber(1001L)
                .price(25000L)
                .stock(100) // stock 필드 사용
                .build());

        product2 = productRepository.save(Products.builder()
                .seller(seller)
                .title("유기농 고양이 사료")
                .contents("상품 상세 내용 2")
                .productNumber(1002L)
                .price(15000L)
                .stock(50) // stock 필드 사용
                .build());

        product3 = productRepository.save(Products.builder()
                .seller(seller)
                .title("수제 강아지 간식")
                .contents("상품 상세 내용 3")
                .productNumber(1003L)
                .price(8000L)
                .stock(30) // stock 필드 사용
                .build());

        // 인증 정보 설정
        UserPrincipal userPrincipal = new UserPrincipal(testUser.getProvider(), testUser.getProviderId());
        testAuthentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER"))
        );

        // 유효한 주문 요청 데이터 설정 (실제 DTO 필드명 사용)
        validRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(product1.getId().toString())
                                .quantity(2)
                                .build()
                ))
                .shippingAddress(OrderCreateRequest.ShippingAddressRequest.builder()
                        .recipientName("김철수") // 받는 사람 이름
                        .recipientPhone("010-1234-5678") // 받는 사람 연락처
                        .postalCode("12345") // 우편번호 (zipCode 아님)
                        .streetAddress("서울특별시 강남구 테헤란로 123") // 기본 주소 (addressLine1 아님)
                        .detailAddress("456호") // 상세 주소 (addressLine2 아님)
                        .deliveryNote("문 앞에 놓아주세요") // 배송 요청사항
                        .build())
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                        .orderName("반려동물 용품 주문") // 주문명 (method 아님)
                        .customerEmail("test@catdogeats.com") // 구매자 이메일
                        .customerName("김철수") // 구매자 이름
                        .build())
                .build();
    }

    @Test
    @DisplayName("✅ 정상적인 주문 생성 테스트")
    void createOrder_Success() throws Exception {
        // Given - BeforeEach에서 설정된 validRequest 사용

        // When & Then
        MvcResult result = mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderNumber").exists())
                .andExpect(jsonPath("$.data.totalPrice").value(50000)) // totalPrice 필드 사용 (amount 아님)
                .andReturn();

        // 응답 검증 (실제 응답 구조에 맞게 수정)
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseNode = objectMapper.readTree(responseContent);
        OrderCreateResponse response = objectMapper.treeToValue(responseNode.get("data"), OrderCreateResponse.class);

        assertThat(response.getOrderNumber()).isNotNull();
        assertThat(response.getTotalPrice()).isEqualTo(50000); // getTotalPrice() 사용
        assertThat(response.getTossPaymentInfo().getCustomerName()).isEqualTo("김철수"); // TossPaymentInfo 내부의 customerName
    }

    @Test
    @DisplayName("🔍 실제 DB에 저장된 사용자 정보 확인")
    void verifyTestUserInDatabase() {
        // When
        Users foundUser = userRepository.findById(testUser.getId()).orElse(null);

        // Then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(testUser.getId());
        assertThat(foundUser.getName()).isEqualTo("테스트 사용자");
        assertThat(foundUser.getRole()).isEqualTo(Role.ROLE_BUYER);

        System.out.println("👤 DB에 저장된 사용자 정보 확인 완료: " + foundUser.getId());
    }

    @Test
    @DisplayName("❌ 존재하지 않는 사용자 - 실제 에러 테스트")
    void createOrder_UserNotFound() throws Exception {
        // Given
        UserPrincipal nonExistentUserPrincipal = new UserPrincipal("DUMMY_PROVIDER", "non-existent-user-id");
        UsernamePasswordAuthenticationToken nonExistentUserAuth = new UsernamePasswordAuthenticationToken(
                nonExistentUserPrincipal,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER"))
        );

        // When & Then
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
        // Given
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(product1.getId().toString())
                                .quantity(0) // 잘못된 수량
                                .build()
                ))
                .shippingAddress(validRequest.getShippingAddress())
                .paymentInfo(validRequest.getPaymentInfo())
                .build();

        // When & Then
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
        // Given
        OrderCreateRequest multiProductRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(product1.getId().toString())
                                .quantity(1)
                                .build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(product2.getId().toString())
                                .quantity(1)
                                .build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(product3.getId().toString())
                                .quantity(1)
                                .build()
                ))
                .shippingAddress(validRequest.getShippingAddress())
                .paymentInfo(validRequest.getPaymentInfo())
                .build();

        // When & Then
        mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(multiProductRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalPrice").value(48000)) // 25000 + 15000 + 8000
                .andExpect(jsonPath("$.data.orderNumber").exists());
    }

    @Test
    @DisplayName("❌ 재고 부족 시 주문 실패 테스트")
    void createOrder_InsufficientStock() throws Exception {
        // Given - product1의 재고는 100개인데 150개 주문
        OrderCreateRequest insufficientStockRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(product1.getId().toString())
                                .quantity(150) // 재고(100개)보다 많은 수량
                                .build()
                ))
                .shippingAddress(validRequest.getShippingAddress())
                .paymentInfo(validRequest.getPaymentInfo())
                .build();

        // When & Then
        mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insufficientStockRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("🔍 상품 재고 검증 - 테스트 데이터 확인")
    void verifyProductStockInDatabase() {
        // When
        Products foundProduct1 = productRepository.findById(product1.getId()).orElse(null);
        Products foundProduct2 = productRepository.findById(product2.getId()).orElse(null);
        Products foundProduct3 = productRepository.findById(product3.getId()).orElse(null);

        // Then
        assertThat(foundProduct1).isNotNull();
        assertThat(foundProduct1.getStock()).isEqualTo(100); // stock 필드 사용
        assertThat(foundProduct1.getPrice()).isEqualTo(25000L);

        assertThat(foundProduct2).isNotNull();
        assertThat(foundProduct2.getStock()).isEqualTo(50); // stock 필드 사용
        assertThat(foundProduct2.getPrice()).isEqualTo(15000L);

        assertThat(foundProduct3).isNotNull();
        assertThat(foundProduct3.getStock()).isEqualTo(30); // stock 필드 사용
        assertThat(foundProduct3.getPrice()).isEqualTo(8000L);

        System.out.println("🛍️ 상품 재고 정보 확인 완료");
        System.out.println("Product1 재고: " + foundProduct1.getStock());
        System.out.println("Product2 재고: " + foundProduct2.getStock());
        System.out.println("Product3 재고: " + foundProduct3.getStock());
    }
}