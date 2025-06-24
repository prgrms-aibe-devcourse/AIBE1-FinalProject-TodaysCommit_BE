package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.common.DetailedOrderItemInfo;
import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.service.OrderService;
import com.team5.catdogeats.orders.util.TossPaymentResponseBuilder;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 주문 관리 서비스 구현체 (EDA + Record DTO + 할인 적용 + 토스 Util 분리)
 * 이벤트 기반 아키텍처 적용으로 관심사를 분리했습니다:
 * - OrderService: 주문 엔티티 저장과 이벤트 발행만 담당
 * - EventListeners: 재고 차감, 결제 정보 생성, 알림 등 부가 로직 처리
 * Record DTO 도입으로 개선된 사항:
 * 1. Lombok 어노테이션 제거 → Record 타입으로 대체
 * 2. 내부 클래스 제거 → 공통 DTO 패키지로 분리
 * 3. 불변성 보장 → Record의 자동 불변성 활용
 * 4. 할인 로직 유지 → 기존 비즈니스 로직 그대로 보존
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
     * UserPrincipal을 사용한 주문 생성 (EDA + Record DTO + 할인 적용)
     * 변경된 처리 흐름:
     * 1. 구매자 검증 (BuyerRepository 활용)
     * 2. 상품 정보 수집 및 할인 적용된 가격 계산
     * 3. 주문 엔티티 저장 (할인 반영된 총 금액)
     * 4. 토스 페이먼츠 응답 생성 (유틸리티 클래스 활용)
     * 5. OrderCreatedEvent 발행 (할인 적용된 금액 정보 포함)
     */
    @Override
    @JpaTransactional
    public OrderCreateResponse createOrderByUserPrincipal(UserPrincipal userPrincipal, OrderCreateRequest request) {
        log.info("주문 생성 시작 (EDA + Record DTO + 할인 적용): provider={}, providerId={}, 상품 개수={}",
                userPrincipal.provider(), userPrincipal.providerId(), request.getOrderItems().size());

        // 1. 구매자 검증 (BuyerRepository 활용)
        BuyerDTO buyer = findBuyerByPrincipal(userPrincipal);
        Users user = userRepository.getReferenceById(buyer.userId());

        // 2. 주문 상품들 검증 및 할인 적용된 정보 수집
        List<DetailedOrderItemInfo> detailedOrderItems = validateAndCollectOrderItems(request.getOrderItems());

        // 3. 총 주문 금액 계산 (할인 적용)
        Long totalPrice = calculateTotalPrice(detailedOrderItems);

        // 4. 주문 엔티티 생성 및 저장
        Orders savedOrder = createAndSaveOrder(user, totalPrice);

        // 5. 토스 페이먼츠 응답 생성
        OrderCreateResponse response = buildTossPaymentResponse(savedOrder, request.getPaymentInfo());

        // 6. OrderCreatedEvent 발행 (DetailedOrderItemInfo → OrderItemInfo 변환)
        publishOrderCreatedEvent(savedOrder, user, userPrincipal, detailedOrderItems);

        log.info("주문 생성 완료: orderId={}, orderNumber={}, totalPrice={}, 할인적용={}",
                savedOrder.getId(), savedOrder.getOrderNumber(), totalPrice,
                detailedOrderItems.stream().anyMatch(DetailedOrderItemInfo::isDiscounted));

        return response;
    }

    /**
     * UserPrincipal로 구매자 조회 및 검증 (BuyerRepository 활용)
     * 한 번의 쿼리로 구매자 존재 여부와 권한을 동시에 확인합니다.
     */
    private BuyerDTO findBuyerByPrincipal(UserPrincipal userPrincipal) {
        return buyerRepository.findOnlyBuyerByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("구매자를 찾을 수 없거나 권한이 없습니다"));
    }

    /**
     * 주문 상품들 검증 및 할인 적용된 정보 수집
     * DetailedOrderItemInfo Record 타입을 사용하여 할인 정보를 포함한 상세 정보를 수집합니다.
     * 재고 차감은 StockEventListener에서 처리됩니다.
     */
    private List<DetailedOrderItemInfo> validateAndCollectOrderItems(List<OrderCreateRequest.OrderItemRequest> orderItems) {
        log.debug("주문 상품 검증 시작 (재고 차감 제외, 할인 적용): 상품 개수={}", orderItems.size());

        List<DetailedOrderItemInfo> detailedOrderItems = new ArrayList<>();

        for (OrderCreateRequest.OrderItemRequest item : orderItems) {
            // 상품 존재 여부 확인
            Products product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "상품을 찾을 수 없습니다: " + item.getProductId()));

            // 수량 유효성 검증
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다");
            }

            // 재고 부족 여부 확인
            if (product.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        String.format("재고가 부족한 상품입니다: 상품=%s, 요청수량=%d, 현재고=%d",
                                product.getTitle(), item.getQuantity(), product.getStock()));
            }

            // 할인 적용된 상세 주문 아이템 정보 생성 (Record 정적 팩토리 메서드 활용)
            DetailedOrderItemInfo detailedItem = createDetailedOrderItem(product, item.getQuantity());
            detailedOrderItems.add(detailedItem);

            log.debug("상품 검증 완료: productId={}, name={}, quantity={}, 원가={}, 할인가={}, 할인율={}%",
                    item.getProductId(), product.getTitle(), item.getQuantity(),
                    product.getPrice(), detailedItem.unitPrice(),
                    detailedItem.isDiscounted() ? detailedItem.discountRate() : 0);
        }

        log.debug("전체 주문 상품 검증 완료: 상품 개수={}", detailedOrderItems.size());
        return detailedOrderItems;
    }

    /**
     * 상품 정보를 기반으로 DetailedOrderItemInfo 생성
     * 할인 적용 여부에 따라 적절한 정적 팩토리 메서드를 선택합니다.
     */
    private DetailedOrderItemInfo createDetailedOrderItem(Products product, Integer quantity) {
        if (product.isDiscounted() && product.getDiscountRate() != null) {
            // 할인 적용 상품
            validateDiscountRate(product.getDiscountRate());
            Long discountedPrice = calculateDiscountedPrice(product);

            return DetailedOrderItemInfo.withDiscount(
                    product.getId(),
                    product.getTitle(),
                    quantity,
                    product.getPrice(),
                    discountedPrice,
                    product.getDiscountRate()
            );
        } else {
            // 할인 없는 상품
            return DetailedOrderItemInfo.withoutDiscount(
                    product.getId(),
                    product.getTitle(),
                    quantity,
                    product.getPrice()
            );
        }
    }

    /**
     * 상품의 할인 적용된 가격 계산
     */
    private Long calculateDiscountedPrice(Products product) {
        // 할인이 적용되지 않은 경우 원가 반환
        if (!product.isDiscounted() || product.getDiscountRate() == null) {
            return product.getPrice();
        }

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
     * 총 주문 금액 계산 (할인 적용)
     * DetailedOrderItemInfo Record의 totalPrice를 사용합니다.
     */
    private Long calculateTotalPrice(List<DetailedOrderItemInfo> detailedOrderItems) {
        Long totalPrice = detailedOrderItems.stream()
                .mapToLong(DetailedOrderItemInfo::totalPrice)
                .sum();

        // 할인 적용 통계 로깅
        long totalOriginalPrice = detailedOrderItems.stream()
                .mapToLong(item -> item.originalPrice() * item.quantity())
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
     * OrderCreatedEvent 발행 (DetailedOrderItemInfo → OrderItemInfo 변환)
     * Record의 toOrderItemInfo() 메서드를 활용하여 이벤트용 DTO로 변환합니다.
     */
    private void publishOrderCreatedEvent(Orders order, Users user, UserPrincipal userPrincipal,
                                          List<DetailedOrderItemInfo> detailedOrderItems) {

        // DetailedOrderItemInfo → OrderItemInfo 변환 (Record 메서드 활용)
        List<OrderItemInfo> eventOrderItems = detailedOrderItems.stream()
                .map(DetailedOrderItemInfo::toOrderItemInfo)
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
     * 토스 페이먼츠 응답 생성 (TossPaymentResponseBuilder 활용)
     */
    private OrderCreateResponse buildTossPaymentResponse(Orders order, OrderCreateRequest.PaymentInfoRequest paymentInfo) {
        String orderName = generateOrderName(order);
        return tossPaymentResponseBuilder.buildTossPaymentResponse(order, paymentInfo, orderName);
    }

    /**
     * 고유한 주문 번호 생성
     */
    private Long generateOrderNumber() {
        String timestamp = DateTimeFormatter.ofPattern("yyMMddHHmmss")
                .format(LocalDateTime.now());
        int randomNum = ThreadLocalRandom.current().nextInt(1000, 10000);

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
}