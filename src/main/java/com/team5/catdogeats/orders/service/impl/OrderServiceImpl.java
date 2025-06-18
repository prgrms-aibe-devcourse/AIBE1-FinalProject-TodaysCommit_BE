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

            // 재고 검증 (Mock)
            if (item.getQuantity() > mockProduct.getStock()) {
                throw new IllegalArgumentException("재고가 부족합니다. 상품: " + mockProduct.getName());
            }

            orderItemInfos.add(OrderItemInfo.builder()
                    .productId(item.getProductId())
                    .productName(mockProduct.getName())
                    .unitPrice(mockProduct.getPrice())
                    .quantity(item.getQuantity())
                    .totalPrice(mockProduct.getPrice() * item.getQuantity())
                    .build());
        }

        return orderItemInfos;
    }

    /**
     * Mock 상품 정보 생성 (임시)
     * TODO: Products 도메인 완성 후 제거
     */
    private MockProduct createMockProduct(String productId) {
        // 상품 ID에 따른 임시 Mock 데이터
        return switch (productId) {
            case "product-1" -> MockProduct.builder()
                    .id(productId)
                    .name("프리미엄 강아지 사료")
                    .price(25000L)
                    .stock(100)
                    .build();
            case "product-2" -> MockProduct.builder()
                    .id(productId)
                    .name("고양이 간식")
                    .price(15000L)
                    .stock(50)
                    .build();
            case "product-3" -> MockProduct.builder()
                    .id(productId)
                    .name("반려동물 장난감")
                    .price(8000L)
                    .stock(200)
                    .build();
            default -> MockProduct.builder()
                    .id(productId)
                    .name("일반 반려동물 용품")
                    .price(10000L)
                    .stock(30)
                    .build();
        };
    }

    /**
     * 총 주문 금액 계산
     */
    private Long calculateTotalPrice(List<OrderItemInfo> orderItemInfos) {
        return orderItemInfos.stream()
                .mapToLong(OrderItemInfo::getTotalPrice)
                .sum();
    }

    /**
     * 주문 엔티티 생성 및 저장
     */
    private Orders createAndSaveOrder(Users user, Long totalPrice) {
        Orders order = Orders.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .orderStatus(OrderStatus.PAYMENT_PENDING) // 결제 대기 상태로 시작
                .totalPrice(totalPrice)
                .build();

        return orderRepository.save(order);
    }

    /**
     * 주문 번호 생성 (타임스탬프 기반)
     * TODO: 더 견고한 주문번호 생성 로직으로 개선 가능
     */
    private Long generateOrderNumber() {
        return System.currentTimeMillis() % 10000000000L; // 10자리 숫자로 제한
    }

    /**
     * 주문 아이템들 생성 및 저장
     */
    private List<OrderItems> createAndSaveOrderItems(Orders order, List<OrderItemInfo> orderItemInfos) {
        List<OrderItems> orderItems = new ArrayList<>();

        for (OrderItemInfo info : orderItemInfos) {
            OrderItems orderItem = OrderItems.builder()
                    .orders(order)
                    // TODO: 실제 Products 엔티티 연결 (Products 도메인 완성 후)
                    // .products(productRepository.findById(UUID.fromString(info.getProductId())).orElse(null))
                    .quantity(info.getQuantity())
                    .price(info.getUnitPrice())
                    .build();

            orderItems.add(orderItem);
        }

        // 별도의 OrderItemRepository가 필요하다면 생성, 현재는 Orders를 통해 저장
        // return orderItemRepository.saveAll(orderItems);
        return orderItems; // 임시로 반환
    }

    /**
     * 토스 페이먼츠 정보 생성
     */
    private OrderCreateResponse.TossPaymentInfo createTossPaymentInfo(
            Orders order, OrderCreateRequest.PaymentInfoRequest paymentInfo, Long totalPrice) {

        // 주문명 생성 (첫 번째 상품명 + 외 N건)
        String orderName = generateOrderName(order);

        return OrderCreateResponse.TossPaymentInfo.builder()
                .tossOrderId(order.getId().toString())
                .orderName(orderName)
                .amount(totalPrice)
                .customerName(paymentInfo.getCustomerName())
                .customerEmail(paymentInfo.getCustomerEmail())
                .successUrl(paymentInfo.getSuccessUrl() != null ?
                        paymentInfo.getSuccessUrl() : defaultSuccessUrl)
                .failUrl(paymentInfo.getFailUrl() != null ?
                        paymentInfo.getFailUrl() : defaultFailUrl)
                .clientKey(tossClientKey)
                .build();
    }

    /**
     * 주문명 생성 (토스 페이먼츠용)
     */
    private String generateOrderName(Orders order) {
        // TODO: 실제 상품명으로 생성 (현재는 Mock)
        return "반려동물 용품 외 1건"; // 임시
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
                    .orderItemId(item.getId() != null ? item.getId().toString() : "temp-" + i)
                    .productId("temp-product-" + i) // 임시
                    .productName("임시 상품명 " + (i + 1)) // 임시
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
                .createdAt(order.getCreatedAt())
                .orderItems(orderItemResponses)
                .tossPaymentInfo(tossPaymentInfo)
                .build();
    }

    /**
     * 주문 아이템 정보를 담는 내부 DTO
     */
    @lombok.Builder
    @lombok.Getter
    private static class OrderItemInfo {
        private String productId;
        private String productName;
        private Long unitPrice;
        private Integer quantity;
        private Long totalPrice;
    }

    /**
     * Mock 상품 정보 (임시)
     */
    @lombok.Builder
    @lombok.Getter
    private static class MockProduct {
        private String id;
        private String name;
        private Long price;
        private Integer stock;
    }
}