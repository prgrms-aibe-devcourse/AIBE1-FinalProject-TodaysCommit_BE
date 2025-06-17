package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.domain.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.domain.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.service.OrderService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * 주문 관리 서비스 구현체
 *
 * 주문 생성 및 관리를 위한 핵심 비즈니스 로직을 처리합니다.
 *
 * TODO: Products 도메인 완성 후 실제 상품 검증 로직 추가 예정
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    // TODO: Products 도메인 완성 후 추가 예정
    // private final ProductRepository productRepository;

    // 토스 페이먼츠 설정값들 (application-dev.yml에서 주입)
    @Value("${toss.payments.client-key}")
    private String tossClientKey;

    @Value("${toss.payments.success-url}")
    private String defaultSuccessUrl;

    @Value("${toss.payments.fail-url}")
    private String defaultFailUrl;

    /**
     * 주문을 생성합니다.
     *
     * @param userId 주문을 생성하는 사용자 ID
     * @param request 주문 생성 요청 정보
     * @return 생성된 주문 정보 (토스 페이먼츠 연동 정보 포함)
     */
    @Override
    @Transactional
    public OrderCreateResponse createOrder(String userId, OrderCreateRequest request) {
        log.info("주문 생성 시작: userId={}, 상품 개수={}", userId, request.getOrderItems().size());

        // 1. 사용자 조회 및 검증
        Users user = findUserById(userId);

        // 2. 주문 상품들 검증 및 정보 수집
        List<OrderItemInfo> orderItemInfos = validateAndCollectOrderItems(request.getOrderItems());

        // 3. 총 주문 금액 계산
        Long totalPrice = calculateTotalPrice(orderItemInfos);

        // 4. 주문 엔티티 생성 및 저장
        Orders order = createAndSaveOrder(user, totalPrice);

        // 5. 주문 아이템들 생성 및 저장
        List<OrderItems> savedOrderItems = createAndSaveOrderItems(order, orderItemInfos);

        // 6. 토스 페이먼츠 정보 생성
        OrderCreateResponse.TossPaymentInfo tossPaymentInfo = createTossPaymentInfo(
                order, request.getPaymentInfo(), totalPrice
        );

        // 7. 응답 DTO 생성
        OrderCreateResponse response = buildOrderCreateResponse(order, savedOrderItems, tossPaymentInfo);

        log.info("주문 생성 완료: orderId={}, orderNumber={}, totalPrice={}",
                order.getId(), order.getOrderNumber(), totalPrice);

        return response;
    }

    /**
     * 사용자 조회
     */
    private Users findUserById(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));
    }

    /**
     * 주문 상품들 검증 및 정보 수집
     * TODO: Products 도메인 완성 후 실제 상품 검증 로직으로 교체
     */
    private List<OrderItemInfo> validateAndCollectOrderItems(List<OrderCreateRequest.OrderItemRequest> orderItems) {
        List<OrderItemInfo> orderItemInfos = new ArrayList<>();

        for (OrderCreateRequest.OrderItemRequest item : orderItems) {
            // TODO: 실제 상품 검증 로직 (Products 도메인 완성 후 활성화)
            /*
            Products product = productRepository.findById(UUID.fromString(item.getProductId()))
                .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다: " + item.getProductId()));
            */

            // 임시 Mock 상품 정보 (개발 완료 전까지 사용)
            MockProduct mockProduct = createMockProduct(item.getProductId());

            // 기본 수량 검증
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다.");
            }

            // TODO: 실제 재고 확인 로직 추가 필요

            orderItemInfos.add(OrderItemInfo.of(mockProduct, item.getQuantity()));
        }

        return orderItemInfos;
    }

    /**
     * 총 주문 금액 계산
     */
    private Long calculateTotalPrice(List<OrderItemInfo> orderItemInfos) {
        return orderItemInfos.stream()
                .mapToLong(info -> info.getMockProduct().getPrice() * info.getQuantity())  // Mock 상품 사용
                .sum();
    }

    /**
     * 주문 엔티티 생성 및 저장
     */
    private Orders createAndSaveOrder(Users user, Long totalPrice) {
        // 주문 번호 생성 (현재 시간 기반)
        Long orderNumber = System.currentTimeMillis();

        Orders order = Orders.builder()
                .user(user)
                .orderNumber(orderNumber)
                .orderStatus(OrderStatus.PAYMENT_COMPLETED) // 초기 상태 (실제로는 결제 완료 후 변경)
                .totalPrice(totalPrice)
                .build();

        return orderRepository.save(order);
    }

    /**
     * 주문 아이템들 생성 및 저장
     * TODO: Products 도메인 완성 후 실제 Products 엔티티 사용
     */
    private List<OrderItems> createAndSaveOrderItems(Orders order, List<OrderItemInfo> orderItemInfos) {
        List<OrderItems> orderItems = new ArrayList<>();

        for (OrderItemInfo info : orderItemInfos) {
            OrderItems orderItem = OrderItems.builder()
                    .orders(order)
                    // .products(info.getProduct())  // TODO: Products 도메인 완성 후 활성화
                    .products(null)  // 임시로 null (실제로는 Products 엔티티 필요)
                    .quantity(info.getQuantity())
                    .price(info.getMockProduct().getPrice()) // Mock 상품 가격 사용
                    .build();

            orderItems.add(orderItem);
        }

        // TODO: OrderItems Repository를 통한 저장 (현재는 예시)
        // return orderItemRepository.saveAll(orderItems);
        log.warn("⚠️  OrderItems 저장은 실제 OrderItemRepository 구현 후 활성화 예정");
        return orderItems; // 임시 반환
    }

    /**
     * 토스 페이먼츠 정보 생성
     */
    private OrderCreateResponse.TossPaymentInfo createTossPaymentInfo(
            Orders order, OrderCreateRequest.PaymentInfoRequest paymentInfo, Long totalPrice) {

        // 토스 페이먼츠용 고유 주문 ID 생성 (UUID)
        String tossOrderId = UUID.randomUUID().toString();

        return OrderCreateResponse.TossPaymentInfo.builder()
                .tossOrderId(tossOrderId)
                .orderName(paymentInfo.getOrderName())
                .amount(totalPrice)
                .customerName(paymentInfo.getCustomerName())
                .customerEmail(paymentInfo.getCustomerEmail())
                .successUrl(paymentInfo.getSuccessUrl() != null ? paymentInfo.getSuccessUrl() : defaultSuccessUrl)
                .failUrl(paymentInfo.getFailUrl() != null ? paymentInfo.getFailUrl() : defaultFailUrl)
                .clientKey(tossClientKey)
                .build();
    }

    /**
     * 주문 생성 응답 DTO 생성
     */
    private OrderCreateResponse buildOrderCreateResponse(
            Orders order, List<OrderItems> orderItems, OrderCreateResponse.TossPaymentInfo tossPaymentInfo) {

        // 주문 아이템 응답 목록 생성 (임시 Mock 데이터 사용)
        List<OrderCreateResponse.OrderItemResponse> orderItemResponses = new ArrayList<>();

        for (int i = 0; i < orderItems.size(); i++) {
            OrderItems item = orderItems.get(i);

            // TODO: 실제 Products 엔티티 정보 사용 (Products 도메인 완성 후)
            orderItemResponses.add(OrderCreateResponse.OrderItemResponse.builder()
                    .orderItemId(item.getId() != null ? item.getId().toString() : "temp-" + i) // 임시 ID
                    .productId("mock-product-" + i) // 임시 상품 ID
                    .productName("임시 상품 " + (i + 1)) // 임시 상품명
                    .quantity(item.getQuantity())
                    .unitPrice(item.getPrice())
                    .totalPrice(item.getPrice() * item.getQuantity())
                    .build());
        }

        return OrderCreateResponse.builder()
                .orderId(order.getId().toString())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .totalPrice(order.getTotalPrice())
                .createdAt(ZonedDateTime.now()) // TODO: 실제 엔티티의 createdAt 사용
                .orderItems(orderItemResponses)
                .tossPaymentInfo(tossPaymentInfo)
                .build();
    }

    /**
     * 임시 Mock 상품 생성 (Products 도메인 완성 전까지 사용)
     * TODO: Products 도메인 완성 후 제거
     */
    private MockProduct createMockProduct(String productId) {
        // 상품 ID에 따라 다른 Mock 데이터 생성
        String productName = "임시 상품 " + productId.substring(0, 8);
        Long price = 10000L + (productId.hashCode() % 50000); // 10,000원 ~ 60,000원 랜덤

        return new MockProduct(productId, productName, price);
    }

    /**
     * 임시 Mock 상품 클래스 (Products 엔티티 대체용)
     * TODO: Products 도메인 완성 후 제거
     */
    private static class MockProduct {
        private final String id;
        private final String name;
        private final Long price;

        public MockProduct(String id, String name, Long price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public Long getPrice() { return price; }
    }

    /**
     * 주문 아이템 정보를 담는 내부 클래스 (수정됨)
     */
    private static class OrderItemInfo {
        // private final Products product;  // TODO: Products 도메인 완성 후 활성화
        private final MockProduct mockProduct;  // 임시 Mock 상품 사용
        private final Integer quantity;

        private OrderItemInfo(MockProduct mockProduct, Integer quantity) {
            this.mockProduct = mockProduct;
            this.quantity = quantity;
        }

        public static OrderItemInfo of(MockProduct mockProduct, Integer quantity) {
            return new OrderItemInfo(mockProduct, quantity);
        }

        // public Products getProduct() { return product; }  // TODO: 나중에 활성화
        public MockProduct getMockProduct() { return mockProduct; }
        public Integer getQuantity() { return quantity; }
    }
}