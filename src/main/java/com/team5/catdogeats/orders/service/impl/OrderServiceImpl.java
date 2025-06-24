package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.config.TossPaymentsConfig;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.service.OrderService;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 주문 관리 서비스 구현체 (EDA 전환 + BuyerRepository 적용)
 * 이벤트 기반 아키텍처 적용으로 관심사를 분리했습니다:
 * - OrderService: 주문 엔티티 저장과 이벤트 발행만 담당
 * - EventListeners: 재고 차감, 결제 정보 생성, 알림 등 부가 로직 처리
 * 리팩터링 개선사항:
 * 1. BuyerRepository 활용으로 구매자 조회 + 권한 검증을 한 번에 처리
 * 2. 불필요한 validateBuyerPermission() 메서드 제거
 * 3. 효율적인 구매자 권한 확인 로직 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final BuyerRepository buyerRepository;
    private final ProductRepository productRepository;
    private final TossPaymentsConfig.TossPaymentsProperties tossPaymentsProperties;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * UserPrincipal을 사용한 주문 생성 (EDA 전환 + BuyerRepository 적용)
     * 변경된 처리 흐름:
     * 1. 구매자 검증 (한 번의 조회로 존재 여부 + 권한 확인)
     * 2. 상품 정보 수집 및 주문 엔티티 저장
     * 3. OrderCreatedEvent 발행
     * 4. 이벤트 리스너들이 비동기로 후속 작업 처리
     */
    @Override
    @Transactional(transactionManager = "jpaTransactionManager")
    public OrderCreateResponse createOrderByUserPrincipal(UserPrincipal userPrincipal, OrderCreateRequest request) {
        log.info("주문 생성 시작 (EDA + BuyerRepository 버전): provider={}, providerId={}, 상품 개수={}",
                userPrincipal.provider(), userPrincipal.providerId(), request.getOrderItems().size());

        // 1. UserPrincipal로 구매자 조회 및 권한 검증 (한 번에 처리)
        BuyerDTO buyerDTO = findBuyerByPrincipal(userPrincipal);

        // 2. Users 엔티티 조회 (주문 생성용)
        Users user = userRepository.getReferenceById(buyerDTO.userId());

        // 3. 주문 상품들 검증 및 정보 수집 (재고 차감은 제외)
        List<OrderItemInfo> orderItemInfos = validateAndCollectOrderItems(request.getOrderItems());

        // 4. 총 주문 금액 계산
        Long totalPrice = calculateTotalPrice(orderItemInfos);

        // 5. 주문 엔티티 생성 및 저장 (PAYMENT_PENDING 상태)
        Orders savedOrder = createAndSaveOrder(user, totalPrice);

        // 6. 토스 페이먼츠 응답 생성
        OrderCreateResponse response = buildTossPaymentResponse(savedOrder, request.getPaymentInfo());

        // 7. OrderCreatedEvent 발행 (트랜잭션 커밋 후 이벤트 리스너들이 처리)
        publishOrderCreatedEvent(savedOrder, user, userPrincipal, orderItemInfos);

        log.info("주문 생성 완료 (재고 차감은 이벤트 처리): orderId={}, orderNumber={}, totalPrice={}",
                savedOrder.getId(), savedOrder.getOrderNumber(), totalPrice);

        return response;
    }

    /**
     * UserPrincipal로 구매자 조회 및 권한 검증 (개선된 버전)
     * BuyerRepository를 사용하여 한 번의 쿼리로 구매자 확인 + 권한 검증을 동시에 처리합니다.
     * 구매자가 아니거나 존재하지 않으면 조회되지 않으므로 별도 권한 검증이 불필요합니다.
     */
    private BuyerDTO findBuyerByPrincipal(UserPrincipal userPrincipal) {
        return buyerRepository.findOnlyBuyerByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("구매자를 찾을 수 없거나 권한이 없습니다"));
    }

    /**
     * 주문 상품들 검증 및 정보 수집 (EDA 전환: 재고 차감 로직 제거)
     * 기존 validateOrderItems에서 재고 차감 부분을 제거하고,
     * 상품 존재 여부와 기본 검증만 수행합니다.
     * 실제 재고 차감은 StockEventListener에서 처리됩니다.
     */
    private List<OrderItemInfo> validateAndCollectOrderItems(List<OrderCreateRequest.OrderItemRequest> orderItems) {
        log.debug("주문 상품 검증 시작 (재고 차감 제외): 상품 개수={}", orderItems.size());

        List<OrderItemInfo> orderItemInfos = new ArrayList<>();

        for (OrderCreateRequest.OrderItemRequest item : orderItems) {
            // 상품 존재 여부 확인
            Products product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "상품을 찾을 수 없습니다: " + item.getProductId()));

            // 수량 유효성 검증
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다");
            }

            // 재고가 요청 수량보다 부족한지 확인
            if (product.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        String.format("재고가 부족한 상품입니다: 상품=%s, 요청수량=%d, 현재고=%d",
                                product.getTitle(), item.getQuantity(), product.getStock()));
            }

            // 주문 아이템 정보 수집
            OrderItemInfo itemInfo = OrderItemInfo.builder()
                    .productId(item.getProductId())
                    .productName(product.getTitle())
                    .quantity(item.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(product.getPrice() * item.getQuantity())
                    .build();

            orderItemInfos.add(itemInfo);

            log.debug("상품 검증 완료: productId={}, name={}, quantity={}, price={}",
                    item.getProductId(), product.getTitle(), item.getQuantity(), product.getPrice());
        }

        log.debug("전체 주문 상품 검증 완료: 상품 개수={}", orderItemInfos.size());
        return orderItemInfos;
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
     * OrderCreatedEvent 발행
     * 이벤트 리스너들이 다음 작업들을 비동기로 처리합니다:
     * - StockEventListener: 재고 차감
     * - PaymentEventListener: 결제 정보 생성
     * - NotificationEventListener: 사용자 알림
     */
    private void publishOrderCreatedEvent(Orders order, Users user, UserPrincipal userPrincipal,
                                          List<OrderItemInfo> orderItemInfos) {

        // OrderCreatedEvent용 OrderItemInfo 리스트 변환
        List<OrderCreatedEvent.OrderItemInfo> eventOrderItems = orderItemInfos.stream()
                .map(info -> OrderCreatedEvent.OrderItemInfo.builder()
                        .productId(info.getProductId())
                        .productName(info.getProductName())
                        .quantity(info.getQuantity())
                        .unitPrice(info.getUnitPrice())
                        .totalPrice(info.getTotalPrice())
                        .build())
                .toList();

        // OrderCreatedEvent 생성 및 발행
        OrderCreatedEvent event = OrderCreatedEvent.of(
                order.getId(),
                order.getOrderNumber(),
                user.getId(),
                userPrincipal.provider(),
                userPrincipal.providerId(),
                order.getTotalPrice(),
                eventOrderItems
        );

        eventPublisher.publishEvent(event);
        log.info("OrderCreatedEvent 발행 완료: {}", event);
    }

    /**
     * 토스 페이먼츠 응답 생성
     */
    private OrderCreateResponse buildTossPaymentResponse(Orders order, OrderCreateRequest.PaymentInfoRequest paymentInfo) {
        // TossPaymentInfo 생성
        OrderCreateResponse.TossPaymentInfo tossPaymentInfo = OrderCreateResponse.TossPaymentInfo.builder()
                .tossOrderId(order.getId())
                .orderName(generateOrderName(order))
                .amount(order.getTotalPrice())
                .customerName(paymentInfo.getCustomerName())
                .customerEmail(paymentInfo.getCustomerEmail())
                .successUrl(paymentInfo.getSuccessUrl() != null ?
                        paymentInfo.getSuccessUrl() : tossPaymentsProperties.getSuccessUrl())
                .failUrl(paymentInfo.getFailUrl() != null ?
                        paymentInfo.getFailUrl() : tossPaymentsProperties.getFailUrl())
                .clientKey(tossPaymentsProperties.getClientKey())
                .build();

        return OrderCreateResponse.builder()
                .orderNumber(order.getOrderNumber())
                .totalPrice(order.getTotalPrice())
                .orderId(order.getId())
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