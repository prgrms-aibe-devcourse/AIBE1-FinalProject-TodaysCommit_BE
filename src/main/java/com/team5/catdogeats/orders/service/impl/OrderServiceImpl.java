package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.global.config.TossPaymentsConfig;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.service.OrderService;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * 주문 관리 서비스 구현체 (1단계: 재고 차감 포함)
 *
 * 1단계 "주문(구매자)" 기능:
 * - 상품 존재 확인 및 재고 검증
 * - 재고 차감 (동시성 제어)
 * - 주문 엔티티 생성 (PENDING 상태)
 * - 토스 페이먼츠 정보 응답
 *
 * 모든 작업이 하나의 트랜잭션에서 수행되어 재고 차감 실패 시 주문도 롤백됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final TossPaymentsConfig.TossPaymentsProperties tossPaymentsProperties;

    /**
     * 주문을 생성합니다. (1단계: 재고 차감 포함)
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
        List<OrderItemInfo> orderItemInfos = validateOrderItems(request.getOrderItems());

        // 3. 재고 차감 (중요: 주문 생성 전에 수행)
        performStockDeduction(orderItemInfos);

        // 4. 총 주문 금액 계산
        Long totalPrice = calculateTotalPrice(orderItemInfos);

        // 5. 주문 엔티티 생성 및 저장
        Orders order = createAndSaveOrder(user, totalPrice);

        // 6. 주문 아이템들 생성 및 저장
        List<OrderItems> savedOrderItems = createAndSaveOrderItems(order, orderItemInfos);

        // 7. 토스 페이먼츠 정보 생성
        OrderCreateResponse.TossPaymentInfo tossPaymentInfo = createTossPaymentInfo(
                order, request.getPaymentInfo(), totalPrice);

        // 8. 응답 DTO 생성
        OrderCreateResponse response = buildOrderCreateResponse(order, savedOrderItems, tossPaymentInfo);

        log.info("주문 생성 완료 (재고 차감 완료): orderId={}, orderNumber={}, totalPrice={}",
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
     * 주문 상품들 검증 (존재 확인 + 재고 확인)
     */
    private List<OrderItemInfo> validateOrderItems(List<OrderCreateRequest.OrderItemRequest> orderItems) {
        List<OrderItemInfo> orderItemInfos = new ArrayList<>();

        for (OrderCreateRequest.OrderItemRequest item : orderItems) {
            // 실제 상품 조회
            Products product = productRepository.findById(UUID.fromString(item.getProductId()))
                    .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다: " + item.getProductId()));

            // 기본 수량 검증
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다.");
            }

            // 재고 확인 (차감 전 검증)
            if (item.getQuantity() > product.getQuantity()) {
                throw new IllegalArgumentException(
                        String.format("재고가 부족합니다. 상품: %s, 요청 수량: %d, 재고: %d",
                                product.getTitle(), item.getQuantity(), product.getQuantity()));
            }

            // 주문 상품 정보 생성
            OrderItemInfo orderItemInfo = OrderItemInfo.builder()
                    .productId(product.getId().toString())
                    .productName(product.getTitle())
                    .unitPrice(product.getPrice())
                    .quantity(item.getQuantity())
                    .totalPrice(product.getPrice() * item.getQuantity())
                    .build();

            orderItemInfos.add(orderItemInfo);
        }

        return orderItemInfos;
    }

    /**
     * 재고 차감 수행 (원자적 처리)
     *
     * 동시성 제어를 위해 데이터베이스 레벨에서 원자적으로 처리합니다.
     */
    private void performStockDeduction(List<OrderItemInfo> orderItemInfos) {
        log.info("재고 차감 시작: 상품 개수={}", orderItemInfos.size());

        for (OrderItemInfo itemInfo : orderItemInfos) {
            UUID productId = UUID.fromString(itemInfo.getProductId());
            Integer quantity = itemInfo.getQuantity();

            // 원자적 재고 차감
            int updatedRows = productRepository.decreaseQuantity(productId, quantity);

            if (updatedRows == 0) {
                // 재고 차감 실패: 동시성 충돌 또는 재고 부족
                throw new IllegalStateException(
                        String.format("재고 차감 실패 (동시성 충돌 또는 재고 부족): 상품=%s, 요청수량=%d",
                                itemInfo.getProductName(), quantity));
            }

            log.info("재고 차감 성공: 상품={}, 차감수량={}", itemInfo.getProductName(), quantity);
        }

        log.info("전체 재고 차감 완료");
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
                .user(user)
                .orderStatus(OrderStatus.PAYMENT_PENDING) // 결제 대기 상태 (재고는 이미 차감됨)
                .totalPrice(totalPrice)
                // createdAt은 BaseEntity의 @PrePersist에서 자동 설정
                .build();

        return orderRepository.save(order);
    }

    /**
     * 주문 아이템들 생성 및 저장
     */
    private List<OrderItems> createAndSaveOrderItems(Orders order, List<OrderItemInfo> orderItemInfos) {
        List<OrderItems> orderItems = new ArrayList<>();

        for (OrderItemInfo info : orderItemInfos) {
            Products product = productRepository.findById(UUID.fromString(info.getProductId()))
                    .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다: " + info.getProductId()));

            OrderItems orderItem = OrderItems.builder()
                    .orders(order)
                    .products(product)
                    .quantity(info.getQuantity())
                    .price(info.getUnitPrice())
                    .build();

            orderItems.add(orderItem);
        }

        // TODO: OrderItemRepository가 있다면 saveAll로 저장
        // return orderItemRepository.saveAll(orderItems);
        return orderItems; // 현재는 Orders와 cascade로 저장됨
    }

    /**
     * 토스 페이먼츠 정보 생성
     */
    private OrderCreateResponse.TossPaymentInfo createTossPaymentInfo(
            Orders order, OrderCreateRequest.PaymentInfoRequest paymentInfo, Long totalPrice) {

        String orderName = generateOrderName(order);

        return OrderCreateResponse.TossPaymentInfo.builder()
                .tossOrderId(order.getId().toString())
                .orderName(orderName)
                .amount(totalPrice)
                .customerName(paymentInfo.getCustomerName())
                .customerEmail(paymentInfo.getCustomerEmail())
                .successUrl(paymentInfo.getSuccessUrl() != null ?
                        paymentInfo.getSuccessUrl() : tossPaymentsProperties.getSuccessUrl())
                .failUrl(paymentInfo.getFailUrl() != null ?
                        paymentInfo.getFailUrl() : tossPaymentsProperties.getFailUrl())
                .clientKey(tossPaymentsProperties.getClientKey())
                .build();
    }

    /**
     * 주문명 생성 (토스 페이먼츠용)
     */
    private String generateOrderName(Orders order) {
        return String.format("%s%d번 주문",
                tossPaymentsProperties.getOrder().getPrefix(),
                order.getOrderNumber());
    }

    /**
     * 주문 생성 응답 DTO 생성
     */
    private OrderCreateResponse buildOrderCreateResponse(
            Orders order, List<OrderItems> orderItems, OrderCreateResponse.TossPaymentInfo tossPaymentInfo) {

        List<OrderCreateResponse.OrderItemResponse> orderItemResponses = new ArrayList<>();

        for (OrderItems item : orderItems) {
            orderItemResponses.add(OrderCreateResponse.OrderItemResponse.builder()
                    .orderItemId(item.getId() != null ? item.getId().toString() : "temp-" + System.nanoTime())
                    .productId(item.getProducts().getId().toString())
                    .productName(item.getProducts().getTitle())
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
     * 주문 상품 정보를 담는 내부 DTO
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
}