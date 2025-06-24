package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
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
import com.team5.catdogeats.orders.util.TossPaymentResponseBuilder;
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
 * 주문 관리 서비스 구현체 (EDA + BuyerRepository + 할인 적용 + 토스 Util 분리)
 * 이벤트 기반 아키텍처 적용으로 관심사를 분리했습니다:
 * - OrderService: 주문 엔티티 저장과 이벤트 발행만 담당
 * - EventListeners: 재고 차감, 결제 정보 생성, 알림 등 부가 로직 처리
 * SOLID 원칙 적용을 위한 개선사항:
 * 1. BuyerRepository 활용으로 구매자 조회 + 권한 검증을 한 번에 처리
 * 2. 상품별 할인 적용 로직 추가 (isDiscounted, discountRate 반영)
 * 3. 토스 페이먼츠 관련 유틸리티 분리로 단일 책임 원칙(SRP) 부분 적용
 *    - TossPaymentResponseBuilder: 토스 페이먼츠 응답 생성 전담
 * 4. 핵심 비즈니스 로직에 집중하여 유지보수성 향상
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final BuyerRepository buyerRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TossPaymentResponseBuilder tossPaymentResponseBuilder;

    /**
     * UserPrincipal을 사용한 주문 생성 (EDA + 할인 적용 + 토스 Util 분리)
     * 변경된 처리 흐름:
     * 1. 구매자 검증 (한 번의 조회로 존재 여부 + 권한 확인)
     * 2. 상품 정보 수집 및 할인 적용된 가격 계산
     * 3. 주문 엔티티 저장 (할인 반영된 총 금액)
     * 4. 토스 페이먼츠 응답 생성 (유틸리티 클래스 활용)
     * 5. OrderCreatedEvent 발행 (할인 적용된 금액 정보 포함)
     * 6. 이벤트 리스너들이 비동기로 후속 작업 처리
     */
    @Override
    @Transactional(transactionManager = "jpaTransactionManager")
    public OrderCreateResponse createOrderByUserPrincipal(UserPrincipal userPrincipal, OrderCreateRequest request) {
        log.info("주문 생성 시작 (EDA + 토스 Util 분리 + 할인적용): provider={}, providerId={}, 상품 개수={}",
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

        // 6. 토스 페이먼츠 응답 생성 (주문명을 미리 생성해서 전달)
        String orderName = generateOrderName(savedOrder);
        OrderCreateResponse response = tossPaymentResponseBuilder
                .buildTossPaymentResponse(savedOrder, request.getPaymentInfo(), orderName);

        // 7. OrderCreatedEvent 발행 (트랜잭션 커밋 후 이벤트 리스너들이 처리)
        publishOrderCreatedEvent(savedOrder, user, userPrincipal, orderItemInfos);

        log.info("주문 생성 완료 (토스 Util 분리 + 할인 적용 + 재고 차감은 이벤트 처리): orderId={}, orderNumber={}, totalPrice={}",
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
     * 주문 상품들 검증 및 정보 수집 (EDA 전환 + 할인 적용 로직)
     * 기존 validateOrderItems에서 재고 차감 부분을 제거하고,
     * 상품 존재 여부와 기본 검증만 수행합니다.
     * 할인 적용 로직을 추가하여 정확한 주문 금액을 계산합니다.
     * 실제 재고 차감은 StockEventListener에서 처리됩니다.
     */
    private List<OrderItemInfo> validateAndCollectOrderItems(List<OrderCreateRequest.OrderItemRequest> orderItems) {
        log.debug("주문 상품 검증 시작 (재고 차감 제외, 할인 적용): 상품 개수={}", orderItems.size());

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

            // 할인 적용된 단가 계산
            Long discountedPrice = calculateDiscountedPrice(product);

            // 주문 아이템 정보 수집 (할인 정보 포함)
            OrderItemInfo itemInfo = OrderItemInfo.builder()
                    .productId(item.getProductId())
                    .productName(product.getTitle())
                    .quantity(item.getQuantity())
                    .originalPrice(product.getPrice())
                    .unitPrice(discountedPrice)
                    .totalPrice(discountedPrice * item.getQuantity())
                    .isDiscounted(product.isDiscounted())
                    .discountRate(product.getDiscountRate())
                    .build();

            orderItemInfos.add(itemInfo);

            log.debug("상품 검증 완료: productId={}, name={}, quantity={}, 원가={}, 할인가={}, 할인율={}%",
                    item.getProductId(), product.getTitle(), item.getQuantity(),
                    product.getPrice(), discountedPrice,
                    product.isDiscounted() ? product.getDiscountRate() : 0);
        }

        log.debug("전체 주문 상품 검증 완료: 상품 개수={}", orderItemInfos.size());
        return orderItemInfos;
    }

    /**
     * 총 주문 금액 계산 (할인 적용)
     */
    private Long calculateTotalPrice(List<OrderItemInfo> orderItemInfos) {
        Long totalPrice = orderItemInfos.stream()
                .mapToLong(OrderItemInfo::getTotalPrice)
                .sum();

        // 할인 적용 통계 로깅
        long totalOriginalPrice = orderItemInfos.stream()
                .mapToLong(info -> info.getOriginalPrice() * info.getQuantity())
                .sum();
        long totalDiscountAmount = totalOriginalPrice - totalPrice;

        if (totalDiscountAmount > 0) {
            log.debug("총 주문 금액 계산 완료: 할인전={}원, 할인후={}원, 할인금액={}원",
                    totalOriginalPrice, totalPrice, totalDiscountAmount);
        } else {
            log.debug("총 주문 금액 계산 완료: {}원 (할인 없음)", totalPrice);
        }

        return totalPrice;
    }

    /**
     * 상품의 할인 적용된 가격 계산
     */
    private Long calculateDiscountedPrice(Products product) {
        // 할인이 적용되지 않은 경우 원가 반환
        if (!product.isDiscounted() || product.getDiscountRate() == null) {
            return product.getPrice();
        }

        // 할인율 유효성 검증
        validateDiscountRate(product.getDiscountRate());

        // 할인 적용 계산 (소수점 반올림)
        double discountMultiplier = 1.0 - (product.getDiscountRate() / 100.0);
        Long discountedPrice = Math.round(product.getPrice() * discountMultiplier);

        log.debug("할인 가격 계산: 상품={}, 원가={}원, 할인율={}%, 할인가={}원",
                product.getTitle(), product.getPrice(), product.getDiscountRate(), discountedPrice);

        return discountedPrice;
    }

    /**
     * 할인율 유효성 검증
     */
    private void validateDiscountRate(Double discountRate) {
        if (discountRate < 0 || discountRate > 100) {
            throw new IllegalArgumentException(
                    String.format("할인율은 0~100 사이여야 합니다: %.2f", discountRate));
        }
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

        // OrderCreatedEvent용 OrderItemInfo 리스트 변환 (할인 정보 포함)
        List<OrderCreatedEvent.OrderItemInfo> eventOrderItems = orderItemInfos.stream()
                .map(info -> OrderCreatedEvent.OrderItemInfo.builder()
                        .productId(info.getProductId())
                        .productName(info.getProductName())
                        .quantity(info.getQuantity())
                        .unitPrice(info.getUnitPrice())        // 할인 적용된 단가
                        .totalPrice(info.getTotalPrice())      // 할인 적용된 총가격
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
     * 주문 상품 정보를 담는 내부 클래스 (할인 정보 포함)
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class OrderItemInfo {
        private String productId;
        private String productName;
        private Integer quantity;
        private Long originalPrice;    // 원가 (할인 전 가격)
        private Long unitPrice;        // 할인 적용된 단가
        private Long totalPrice;       // 할인 적용된 총 가격
        private Boolean isDiscounted;  // 할인 적용 여부
        private Double discountRate;   // 할인율 (%)
    }
}