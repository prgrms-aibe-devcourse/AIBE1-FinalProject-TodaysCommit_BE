package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.domain.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.domain.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.service.OrderService;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

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
     */
    private List<OrderItemInfo> validateAndCollectOrderItems(List<OrderCreateRequest.OrderItemRequest> orderItems) {
        List<OrderItemInfo> orderItemInfos = new ArrayList<>();

        for (OrderCreateRequest.OrderItemRequest item : orderItems) {
            // 상품 존재 여부 확인
            Products product = productRepository.findById(UUID.fromString(item.getProductId()))
                    .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다: " + item.getProductId()));

            // 재고 확인 (TODO: 실제 재고 로직 구현 필요)
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다.");
            }

            orderItemInfos.add(OrderItemInfo.of(product, item.getQuantity()));
        }

        return orderItemInfos;
    }

    /**
     * 총 주문 금액 계산
     */
    private Long calculateTotalPrice(List<OrderItemInfo> orderItemInfos) {
        return orderItemInfos.stream()
                .mapToLong(info -> info.getProduct().getPrice() * info.getQuantity())
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
     */
    private List<OrderItems> createAndSaveOrderItems(Orders order, List<OrderItemInfo> orderItemInfos) {
        List<OrderItems> orderItems = new ArrayList<>();

        for (OrderItemInfo info : orderItemInfos) {
            OrderItems orderItem = OrderItems.builder()
                    .orders(order)
                    .products(info.getProduct())
                    .quantity(info.getQuantity())
                    .price(info.getProduct().getPrice()) // 주문 시점 가격 저장
                    .build();

            orderItems.add(orderItem);
        }

        // TODO: OrderItems Repository를 통한 저장 (현재는 예시)
        // return orderItemRepository.saveAll(orderItems);
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

        // 주문 아이템 응답 목록 생성
        List<OrderCreateResponse.OrderItemResponse> orderItemResponses = orderItems.stream()
                .map(item -> OrderCreateResponse.OrderItemResponse.builder()
                        .orderItemId(item.getId().toString())
                        .productId(item.getProducts().getId().toString())
                        .productName(item.getProducts().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getPrice())
                        .totalPrice(item.getPrice() * item.getQuantity())
                        .build())
                .toList();

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
     * 주문 아이템 정보를 담는 내부 클래스
     */
    private static class OrderItemInfo {
        private final Products product;
        private final Integer quantity;

        private OrderItemInfo(Products product, Integer quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        public static OrderItemInfo of(Products product, Integer quantity) {
            return new OrderItemInfo(product, quantity);
        }

        public Products getProduct() { return product; }
        public Integer getQuantity() { return quantity; }
    }
}