package com.team5.catdogeats.orders.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.payments.client.TossPaymentsClient;
import com.team5.catdogeats.payments.dto.response.TossPaymentConfirmResponse;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.repository.StockReservationRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableAsync
abstract class BaseOrderIntegrationTest {

    /* ===== 공통 Bean ===== */
    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    @Autowired protected OrderRepository orderRepository;
    @Autowired protected PaymentRepository paymentRepository;
    @Autowired protected StockReservationRepository stockReservationRepository;
    @Autowired protected ProductRepository productRepository;
    @Autowired protected UserRepository userRepository;
    @Autowired protected BuyerRepository buyerRepository;
    @Autowired protected SellersRepository sellersRepository;

    @MockitoBean
    protected TossPaymentsClient tossPaymentsClient;

    /* ===== 테스트 공용 데이터 ===== */
    protected Users     testUser;
    protected Buyers    testBuyer;
    protected Products  testProduct1;
    protected Products  testProduct2;
    protected Authentication             testAuthentication;
    protected OrderCreateRequest         testOrderRequest;
    protected TossPaymentConfirmResponse mockTossResponse;

    /**
     * 여러 엔티티를 **하나의 트랜잭션**으로 저장해 detach/proxy 문제를 방지한다.
     */
    @BeforeEach
    @Transactional
    void commonSetUp() {

        /* ---------- 판매자 & 셀러 ---------- */
        Users sellerUser = userRepository.save(
                Users.builder()
                        .provider("DUMMY")
                        .providerId("seller-" + System.nanoTime())
                        .userNameAttribute("dummy")
                        .name("판매자")
                        .role(Role.ROLE_SELLER)
                        .accountDisable(false)
                        .build()
        );

        Sellers seller = sellersRepository.save(
                Sellers.builder()
                        .user(sellerUser)                  // ✅ 프록시 NO
                        .vendorName("테스트 상점")
                        .businessNumber("123-45-" + String.format("%05d",
                                ThreadLocalRandom.current().nextInt(100000)))
                        .build()
        );

        /* ---------- 구매자 & 바이어 ---------- */
        testUser = userRepository.save(
                Users.builder()
                        .name("김철수")
                        .role(Role.ROLE_BUYER)
                        .provider("google")
                        .providerId("google123")
                        .userNameAttribute("google123")
                        .accountDisable(false)
                        .build()
        );

        testBuyer = buyerRepository.save(
                Buyers.builder()
                        .user(testUser)                     // ✅ 프록시 NO
                        .build()
        );

        /* ---------- 상품 2개 ---------- */
        testProduct1 = productRepository.save(
                Products.builder()
                        .seller(seller)
                        .title("강아지 사료")
                        .contents("프리미엄 강아지 사료")
                        .productNumber(ThreadLocalRandom.current().nextLong(100000, 999999))
                        .price(25_000L)
                        .stock(100)
                        .build()
        );

        testProduct2 = productRepository.save(
                Products.builder()
                        .seller(seller)
                        .title("고양이 간식")
                        .contents("고급 고양이 간식")
                        .productNumber(ThreadLocalRandom.current().nextLong(100000, 999999))
                        .price(15_000L)
                        .stock(50)
                        .build()
        );

        /* ---------- 인증 정보 ---------- */
        UserPrincipal principal = new UserPrincipal("google", "google123");
        testAuthentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_BUYER")));

        /* ---------- 주문 DTO ---------- */
        testOrderRequest = OrderCreateRequest.builder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(testProduct1.getId())
                                .quantity(2)
                                .build(),
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(testProduct2.getId())
                                .quantity(1)
                                .build()
                ))
                .shippingAddress(OrderCreateRequest.ShippingAddressRequest.builder()
                        .recipientName("홍길동")
                        .recipientPhone("010-1234-5678")
                        .postalCode("12345")
                        .streetAddress("서울시 강남구 테헤란로 123")
                        .detailAddress("4층 401호")
                        .build())
                .paymentInfo(OrderCreateRequest.PaymentInfoRequest.builder()
                        .orderName("강아지 사료 외 1건")
                        .customerEmail("test@catdogeats.com")
                        .customerName("김철수")
                        .build())
                .build();

        /* ---------- Mock Toss 응답 ---------- */
        mockTossResponse = TossPaymentConfirmResponse.builder()
                .paymentKey("confirmed_payment_key_123")
                .orderId("")                // 테스트 내부에서 덮어씀
                .orderName("강아지 사료 외 1건")
                .totalAmount(65_000L)
                .status("DONE")
                .approvedAt(OffsetDateTime.now().toZonedDateTime())
                .build();
    }
}
