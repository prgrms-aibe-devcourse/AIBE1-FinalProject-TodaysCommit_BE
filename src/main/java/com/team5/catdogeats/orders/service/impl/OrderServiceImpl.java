package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
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
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 주문 관리 서비스 구현체 (업데이트된 Products 엔티티 적용)
 *
 * Products 엔티티의 quantity 필드가 stock으로 변경됨에 따라
 * 모든 재고 관련 로직을 수정하였습니다.
 *
 * 1단계 "주문(구매자)" 기능:
 * - 상품 존재 확인 및 재고 검증 (stock 필드 사용)
 * - 재고 차감 (동시성 제어)
 * - 주문 엔티티 생성 (PENDING 상태)
 * - 토스 페이먼츠 정보 응답
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final TossPaymentsConfig.TossPaymentsProperties tossPaymentsProperties;

    /**
     * UserPrincipal을 사용한 안전한 주문 생성 (보안 개선 버전)
     */
    @Override
    @Transactional
    public OrderCreateResponse createOrderByUserPrincipal(UserPrincipal userPrincipal, OrderCreateRequest request) {
        log.info("주문 생성 시작: provider={}, providerId={}, 상품 개수={}",
                userPrincipal.provider(), userPrincipal.providerId(), request.getOrderItems().size());

        // 1. UserPrincipal로 사용자 조회 및 검증
        Users user = findUserByPrincipal(userPrincipal);

        // 2. 구매자 권한 검증 (보안 강화)
        validateBuyerPermission(user);

        // 3. 주문 상품들 검증 및 정보 수집
        List<OrderItemInfo> orderItemInfos = validateOrderItems(request.getOrderItems());

        // 4. 재고 차감 (중요: 주문 생성 전에 수행)
        performStockDeduction(orderItemInfos);

        // 5. 총 주문 금액 계산
        Long totalPrice = calculateTotalPrice(orderItemInfos);

        // 6. 주문 엔티티 생성 및 저장
        Orders savedOrder = createAndSaveOrder(user, totalPrice);

        // 7. 주문 아이템들 생성 및 저장
        List<OrderItems> orderItems = createAndSaveOrderItems(savedOrder, orderItemInfos);

        // 8. 토스 페이먼츠 응답 생성
        OrderCreateResponse response = buildTossPaymentResponse(savedOrder, request.getPaymentInfo());

        log.info("주문 생성 완료: orderId={}, orderNumber={}, totalPrice={}",
                savedOrder.getId(), savedOrder.getOrderNumber(), totalPrice);

        return response;
    }

    /**
     * UserPrincipal로 사용자 조회
     */
    private Users findUserByPrincipal(UserPrincipal userPrincipal) {
        return userRepository.findByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다"));
    }

    /**
     * 구매자 권한 검증
     */
    private void validateBuyerPermission(Users user) {
        if (user.getRole() != Role.ROLE_BUYER) {
            throw new IllegalArgumentException("구매자만 주문을 생성할 수 있습니다");
        }

        if (user.isAccountDisable()) {
            throw new IllegalStateException("비활성화된 계정입니다");
        }
    }

    /**
     * 주문 상품들 검증 및 정보 수집 (stock 필드 사용)
     */
    private List<OrderItemInfo> validateOrderItems(List<OrderCreateRequest.OrderItemRequest> orderItems) {
        log.debug("주문 상품 검증 시작: 상품 개수={}", orderItems.size());

        List<OrderItemInfo> orderItemInfos = new ArrayList<>();

        for (OrderCreateRequest.OrderItemRequest item : orderItems) {
            // 상품 존재 여부 확인
            Products product = productRepository.findById(UUID.fromString(item.getProductId()))
                    .orElseThrow(() -> new NoSuchElementException(
                            "상품을 찾을 수 없습니다: " + item.getProductId()));

            // 수량 유효성 검증
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다");
            }

            // 재고 충분성 검증 (stock 필드 사용)
            if (product.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        String.format("재고 부족: 상품=%s, 요청수량=%d, 현재재고=%d",
                                product.getTitle(), item.getQuantity(), product.getStock()));
            }

            // 주문 상품 정보 수집
            OrderItemInfo orderItemInfo = OrderItemInfo.builder()
                    .productId(product.getId().toString())
                    .productName(product.getTitle())
                    .quantity(item.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(product.getPrice() * item.getQuantity())
                    .build();

            orderItemInfos.add(orderItemInfo);

            log.debug("상품 검증 완료: 상품={}, 요청수량={}, 단가={}, 총액={}",
                    product.getTitle(), item.getQuantity(), product.getPrice(), orderItemInfo.getTotalPrice());
        }

        log.debug("전체 주문 상품 검증 완료");
        return orderItemInfos;
    }

    /**
     * 재고 차감 (업데이트된 Repository 메서드 사용)
     *
     * ProductRepository의 decreaseStock 메서드를 사용하여
     * 원자적 재고 차감을 수행합니다.
     */
    private void performStockDeduction(List<OrderItemInfo> orderItemInfos) {
        log.info("재고 차감 시작: 상품 개수={}", orderItemInfos.size());

        for (OrderItemInfo itemInfo : orderItemInfos) {
            UUID productId = UUID.fromString(itemInfo.getProductId());
            Integer quantity = itemInfo.getQuantity();

            // 원자적 재고 차감 (decreaseStock 메서드 사용)
            int updatedRows = productRepository.decreaseStock(productId, quantity);

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
        Long totalPrice = orderItemInfos.stream()
                .mapToLong(OrderItemInfo::getTotalPrice)
                .sum();

        log.debug("총 주문 금액 계산 완료: {}원", totalPrice);
        return totalPrice;
    }

    /**
     * 주문 엔티티 생성 및 저장
     */
    private Orders createAndSaveOrder(Users user, Long totalPrice) {
        Orders order = Orders.builder()
                .user(user)
                .orderNumber(generateOrderNumber())
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(totalPrice)
                .build();

        Orders savedOrder = orderRepository.save(order);
        log.debug("주문 엔티티 저장 완료: orderId={}, orderNumber={}",
                savedOrder.getId(), savedOrder.getOrderNumber());

        return savedOrder;
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

        log.debug("주문 아이템 생성 완료: 아이템 개수={}", orderItems.size());
        return orderItems;
    }

    /**
     * 토스 페이먼츠 응답 생성
     */
    private OrderCreateResponse buildTossPaymentResponse(Orders order, OrderCreateRequest.PaymentInfoRequest paymentInfo) {
        // TossPaymentInfo 생성
        OrderCreateResponse.TossPaymentInfo tossPaymentInfo = OrderCreateResponse.TossPaymentInfo.builder()
                .tossOrderId(order.getId().toString())
                .orderName(generateOrderName(order))
                .amount(order.getTotalPrice()) // amount는 TossPaymentInfo에 있음
                .customerName(paymentInfo.getCustomerName()) // PaymentInfoRequest에서 가져옴
                .customerEmail(paymentInfo.getCustomerEmail()) // PaymentInfoRequest에서 가져옴
                .successUrl(paymentInfo.getSuccessUrl() != null ?
                        paymentInfo.getSuccessUrl() : tossPaymentsProperties.getSuccessUrl())
                .failUrl(paymentInfo.getFailUrl() != null ?
                        paymentInfo.getFailUrl() : tossPaymentsProperties.getFailUrl())
                .clientKey(tossPaymentsProperties.getClientKey())
                .build();

        return OrderCreateResponse.builder()
                .orderNumber(order.getOrderNumber())
                .totalPrice(order.getTotalPrice()) // totalPrice는 메인 클래스에 있음
                .orderId(order.getId().toString())
                .orderStatus(order.getOrderStatus())
                .createdAt(order.getCreatedAt())
                .tossPaymentInfo(tossPaymentInfo)
                .build();
    }

    /**
     * 고유한 주문 번호를 생성하는 메서드
     * (yyyyMMddHHmmss + 6자리 랜덤 숫자)
     */
    private Long generateOrderNumber() {
        // yyyy -> yy 로 변경하여 길이를 줄임
        String timestamp = DateTimeFormatter.ofPattern("yyMMddHHmmss")
                .format(LocalDateTime.now());
        // 6자리 -> 4자리 랜덤 숫자로 변경
        int randomNum = ThreadLocalRandom.current().nextInt(1000, 10000);

        // timestamp(12자리) + randomNum(4자리) = 16자리 숫자 (Long 범위 내)
        Long orderNumber = Long.parseLong(timestamp + randomNum);
        log.debug("주문 번호 생성: {}", orderNumber);

        return orderNumber;
    }

    /**
     * 주문명 생성
     */
    private String generateOrderName(Orders order) {
        return "반려동물 용품 주문 #" + order.getOrderNumber();
    }

    /**
     * 주문 상품 정보를 담는 내부 클래스
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class OrderItemInfo {
        private String productId;
        private String productName;
        private Integer quantity;
        private Long unitPrice;
        private Long totalPrice;
    }
}