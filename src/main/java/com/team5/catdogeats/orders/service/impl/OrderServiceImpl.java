package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.addresses.domain.enums.AddressType;
import com.team5.catdogeats.addresses.dto.AddressResponseDto;
import com.team5.catdogeats.addresses.service.AddressService;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.common.DetailedOrderItemInfo;
import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.dto.response.OrderDetailResponse;
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
 * 주문 관리 서비스 구현체 (EDA + 쿠폰 할인 방식)
 * 이벤트 기반 아키텍처 적용으로 관심사를 분리했습니다:
 * - OrderService: 주문 엔티티 저장과 이벤트 발행만 담당
 * - EventListeners: 재고 차감, 결제 정보 생성, 알림 등 부가 로직 처리
 * 쿠폰 할인 방식 개선사항:
 * 1. 상품별 할인 제거 → 전체 주문 금액에 쿠폰 할인률 적용
 * 2. 단순화된 가격 계산 로직
 * 3. 할인 정보의 명확한 분리 (원가 vs 최종 가격)
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
    private final AddressService addressService;

    /**
     * UserPrincipal을 사용한 주문 생성 (EDA + 쿠폰 할인 방식)
     * 변경된 처리 흐름:
     * 1. 구매자 검증 (BuyerRepository 활용)
     * 2. 상품 정보 수집 (원가 기준)
     * 3. 전체 주문 금액 계산 (원가 총합)
     * 4. 쿠폰 할인 적용 (전체 금액에서 할인)
     * 5. 주문 엔티티 저장 (할인 적용된 최종 금액)
     * 6. 토스 페이먼츠 응답 생성
     * 7. OrderCreatedEvent 발행 (할인 정보 포함)
     */
    @Override
    @JpaTransactional
    public OrderCreateResponse createOrderByUserPrincipal(UserPrincipal userPrincipal, OrderCreateRequest request) {
        log.info("주문 생성 시작 (EDA + 쿠폰 할인): provider={}, providerId={}, 상품 개수={}, 쿠폰 할인률={}%",
                userPrincipal.provider(), userPrincipal.providerId(),
                request.getOrderItems().size(), request.getPaymentInfo().getCouponDiscountRate());

        // 1. 구매자 검증 (BuyerRepository 활용)
        BuyerDTO buyer = findBuyerByPrincipal(userPrincipal);
        Users user = userRepository.getReferenceById(buyer.userId());

        // 2. 주문 상품들 검증 및 정보 수집 (원가 기준)
        List<DetailedOrderItemInfo> detailedOrderItems = validateAndCollectOrderItems(request.getOrderItems());

        // 3. 원가 총 금액 계산
        Long originalTotalPrice = calculateOriginalTotalPrice(detailedOrderItems);

        // 4. 쿠폰 할인 적용
        Double couponDiscountRate = request.getPaymentInfo().getCouponDiscountRate();
        Long finalTotalPrice = applyCouponDiscount(originalTotalPrice, couponDiscountRate);

        // 5. 주문 엔티티 생성 및 저장 (최종 할인 금액으로)
        Orders savedOrder = createAndSaveOrder(user, finalTotalPrice);

        // 6. 토스 페이먼츠 응답 생성
        OrderCreateResponse response = buildTossPaymentResponse(savedOrder, request.getPaymentInfo());

        // 7. OrderCreatedEvent 발행 (할인 정보 포함)
        publishOrderCreatedEvent(savedOrder, user, userPrincipal, detailedOrderItems,
                originalTotalPrice, couponDiscountRate, finalTotalPrice);

        log.info("주문 생성 완료: orderId={}, orderNumber={}, 원가={}원, 쿠폰할인={}%, 최종금액={}원",
                savedOrder.getId(), savedOrder.getOrderNumber(), originalTotalPrice,
                couponDiscountRate != null ? couponDiscountRate : 0, finalTotalPrice);

        return response;
    }

    // ===== getOrderDetail 메서드 구현 =====
    /**
     * 주문 상세 조회 구현
     */
    @Override
    @JpaTransactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(UserPrincipal userPrincipal, Long orderNumber) {
        log.info("주문 상세 조회 시작 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

        // 1. 사용자 인증 및 구매자 권한 확인
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(
                userPrincipal.provider(),
                userPrincipal.providerId()
        ).orElseThrow(() -> {
            log.warn("구매자를 찾을 수 없음 - provider: {}, providerId: {}",
                    userPrincipal.provider(), userPrincipal.providerId());
            return new NoSuchElementException("구매자 정보를 찾을 수 없습니다.");
        });

        Users user = userRepository.findById(buyerDTO.userId())
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없음 - userId: {}", buyerDTO.userId());
                    return new NoSuchElementException("사용자 정보를 찾을 수 없습니다.");
                });

        // 2. 주문 조회 (OrderItems와 Products 함께 조회)
        Orders order = orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber)
                .orElseThrow(() -> {
                    log.warn("주문을 찾을 수 없음 - userId: {}, orderNumber: {}", user.getId(), orderNumber);
                    return new NoSuchElementException("주문을 찾을 수 없습니다.");
                });

        // 3. 사용자 기본 주소 조회 (배송지 정보로 사용)
        AddressResponseDto defaultAddress = addressService.getDefaultAddress(userPrincipal, AddressType.PERSONAL);

        // 4. 주문 상품 정보 변환
        List<OrderDetailResponse.OrderItemDetail> orderItemDetails = order.getOrderItems().stream()
                .map(orderItem -> new OrderDetailResponse.OrderItemDetail(
                        orderItem.getId(),
                        orderItem.getProducts().getId(),
                        orderItem.getProducts().getTitle(), // getName() → getTitle()로 수정
                        orderItem.getQuantity(),
                        orderItem.getPrice(),
                        orderItem.getPrice() * orderItem.getQuantity()
                )).toList();

        // 5. 총 상품 가격 계산
        Long totalProductPrice = orderItemDetails.stream()
                .mapToLong(OrderDetailResponse.OrderItemDetail::totalPrice)
                .sum();

        // 6. 할인 금액 및 배송비 계산
        Long discountAmount = calculateDiscountAmount(order, totalProductPrice);
        Long deliveryFee = calculateDeliveryFee(order, totalProductPrice);

        // 7. 받는 사람 정보 생성
        OrderDetailResponse.RecipientInfo recipientInfo = createRecipientInfo(defaultAddress, orderNumber);

        // 8. 결제 정보 생성
        OrderDetailResponse.PaymentInfo paymentInfo = OrderDetailResponse.PaymentInfo.of(
                totalProductPrice, discountAmount, deliveryFee);

        // 9. 응답 DTO 생성
        OrderDetailResponse response = new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCreatedAt().toLocalDateTime(),
                order.getOrderStatus(),
                recipientInfo,
                paymentInfo,
                orderItemDetails
        );

        log.info("주문 상세 조회 완료 - orderId: {}, orderNumber: {}, 상품 개수: {}",
                order.getId(), order.getOrderNumber(), orderItemDetails.size());

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
     * 주문 상품들 검증 및 정보 수집 (원가 기준)
     * 상품별 할인을 제거하고 모든 상품을 원가로 계산합니다.
     * 재고 차감은 StockEventListener에서 처리됩니다.
     */
    private List<DetailedOrderItemInfo> validateAndCollectOrderItems(List<OrderCreateRequest.OrderItemRequest> orderItems) {
        List<DetailedOrderItemInfo> detailedOrderItems = new ArrayList<>();

        for (OrderCreateRequest.OrderItemRequest orderItem : orderItems) {
            // 상품 존재 여부 및 기본 정보 검증
            Products product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("상품을 찾을 수 없습니다: %s", orderItem.getProductId())));

            // 수량 검증
            if (orderItem.getQuantity() <= 0) {
                throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다");
            }

            // DetailedOrderItemInfo 생성 (원가 기준)
            DetailedOrderItemInfo detailedItem = DetailedOrderItemInfo.of(
                    product.getId(),
                    product.getTitle(),
                    orderItem.getQuantity(),
                    product.getPrice()  // 원가 사용
            );

            detailedOrderItems.add(detailedItem);

            log.debug("상품 정보 수집 완료: 상품={}, 수량={}, 단가={}원",
                    product.getTitle(), orderItem.getQuantity(), product.getPrice());
        }

        log.debug("전체 주문 상품 검증 완료: 상품 개수={}", detailedOrderItems.size());
        return detailedOrderItems;
    }

    /**
     * 원가 기준 총 주문 금액 계산
     */
    private Long calculateOriginalTotalPrice(List<DetailedOrderItemInfo> detailedOrderItems) {
        Long originalTotalPrice = detailedOrderItems.stream()
                .mapToLong(DetailedOrderItemInfo::totalPrice)
                .sum();

        log.debug("원가 총 금액 계산 완료: {}원", originalTotalPrice);
        return originalTotalPrice;
    }

    /**
     * 쿠폰 할인 적용
     * 전체 주문 금액에서 쿠폰 할인률만큼 할인합니다.
     * @param originalTotalPrice 원가 총 금액
     * @param couponDiscountRate 쿠폰 할인률 (%, null이면 할인 없음)
     * @return 할인 적용된 최종 금액
     */
    private Long applyCouponDiscount(Long originalTotalPrice, Double couponDiscountRate) {
        // 쿠폰 할인이 없는 경우
        if (couponDiscountRate == null || couponDiscountRate <= 0) {
            log.debug("쿠폰 할인 없음: 최종 금액 = {}원", originalTotalPrice);
            return originalTotalPrice;
        }

        // 할인율 유효성 검증
        if (couponDiscountRate > 100) {
            throw new IllegalArgumentException("쿠폰 할인률은 100%를 초과할 수 없습니다: " + couponDiscountRate);
        }

        // 할인 금액 계산 (소수점 반올림)
        double discountMultiplier = 1.0 - (couponDiscountRate / 100.0);
        long finalTotalPrice = Math.round(originalTotalPrice * discountMultiplier);

        // 최소 결제 금액 검증 (1원 이상)
        if (finalTotalPrice < 1) {
            finalTotalPrice = 1L;
        }

        Long discountAmount = originalTotalPrice - finalTotalPrice;

        log.debug("쿠폰 할인 적용 완료: 원가={}원, 할인률={}%, 할인금액={}원, 최종금액={}원",
                originalTotalPrice, couponDiscountRate, discountAmount, finalTotalPrice);

        return finalTotalPrice;
    }

    /**
     * 주문 엔티티 생성 및 저장
     */
    private Orders createAndSaveOrder(Users user, Long finalTotalPrice) {
        Orders order = Orders.builder()
                .user(user)
                .orderNumber(generateOrderNumber())
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(finalTotalPrice)  // 할인 적용된 최종 금액
                .build();

        Orders savedOrder = orderRepository.save(order);
        log.debug("주문 엔티티 저장 완료: orderId={}, orderNumber={}, finalPrice={}원",
                savedOrder.getId(), savedOrder.getOrderNumber(), finalTotalPrice);

        return savedOrder;
    }

    /**
     * 토스 페이먼츠 응답 생성 (할인 적용된 금액으로)
     */
    private OrderCreateResponse buildTossPaymentResponse(Orders savedOrder,
                                                         OrderCreateRequest.PaymentInfoRequest paymentInfo) {

        // 주문명 생성
        String orderName = generateOrderName(paymentInfo.getOrderName(), savedOrder.getOrderNumber());

        // TossPaymentResponseBuilder의 실제 메서드 호출
        return tossPaymentResponseBuilder.buildTossPaymentResponse(savedOrder, paymentInfo, orderName);
    }

    /**
     * OrderCreatedEvent 발행 (쿠폰 할인 정보 포함)
     */
    private void publishOrderCreatedEvent(Orders savedOrder, Users user, UserPrincipal userPrincipal,
                                          List<DetailedOrderItemInfo> detailedOrderItems, Long originalTotalPrice,
                                          Double couponDiscountRate, Long finalTotalPrice) {

        // DetailedOrderItemInfo → OrderItemInfo 변환
        List<OrderItemInfo> orderItems = detailedOrderItems.stream()
                .map(DetailedOrderItemInfo::toOrderItemInfo)
                .toList();

        // 쿠폰 할인 정보를 포함한 이벤트 생성
        OrderCreatedEvent event = OrderCreatedEvent.of(
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                user.getId(),
                userPrincipal.provider(),
                userPrincipal.providerId(),
                originalTotalPrice,      // 원가 총 금액
                couponDiscountRate,      // 쿠폰 할인률
                finalTotalPrice,         // 최종 할인 금액
                orderItems
        );

        eventPublisher.publishEvent(event);

        log.debug("OrderCreatedEvent 발행 완료: orderId={}, 원가={}원, 쿠폰할인={}%, 최종={}원",
                savedOrder.getId(), originalTotalPrice,
                couponDiscountRate != null ? couponDiscountRate : 0, finalTotalPrice);
    }

    /**
     * 할인 금액 계산 (비즈니스 로직에 따라 구현)
     */
    private Long calculateDiscountAmount(Orders order, Long totalProductPrice) {
        // 현재는 주문 금액에서 실제 결제 금액을 빼서 할인 금액 계산
        return Math.max(0L, totalProductPrice - order.getTotalPrice());
    }

    /**
     * 배송비 계산 (비즈니스 로직에 따라 구현)
     */
    private Long calculateDeliveryFee(Orders order, Long totalProductPrice) {
        // 현재는 기본 배송비 설정 (3,000원, 30,000원 이상 무료배송)
        return totalProductPrice >= 30000L ? 0L : 3000L;
    }

    /**
     * 받는 사람 정보 생성
     */
    private OrderDetailResponse.RecipientInfo createRecipientInfo(AddressResponseDto defaultAddress, Long orderNumber) {
        if (defaultAddress == null) {
            // 기본 주소가 없는 경우 기본값 설정
            return new OrderDetailResponse.RecipientInfo(
                    "수령인 미등록",
                    "연락처 미등록",
                    "주소 미등록",
                    "배송 요청사항 없음"
            );
        }

        return OrderDetailResponse.RecipientInfo.of(
                "수령인", // TODO: 실제 수령인 이름 (별도 저장 필요)
                defaultAddress.getPhoneNumber(),
                defaultAddress.getCity(),
                defaultAddress.getDistrict(),
                defaultAddress.getNeighborhood(),
                defaultAddress.getStreetAddress(),
                defaultAddress.getDetailAddress(),
                "안전하게 배송 부탁드립니다." // TODO: 실제 배송 요청사항 (별도 저장 필요)
        );
    }

    /**
     * 주문 번호 생성 (기존 로직 유지)
     * 현재 시간 기반으로 고유한 주문 번호를 생성합니다.
     */
    private Long generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomSuffix = ThreadLocalRandom.current().nextInt(100, 1000);
        return Long.parseLong(timestamp + randomSuffix);
    }

    /**
     * 토스 페이먼츠용 주문명 생성
     *
     * @param requestOrderName 요청에서 받은 주문명
     * @param orderNumber 생성된 주문 번호
     * @return 토스 페이먼츠에 표시될 주문명
     */
    private String generateOrderName(String requestOrderName, Long orderNumber) {
        if (requestOrderName != null && !requestOrderName.trim().isEmpty()) {
            return requestOrderName;
        }
        return "반려동물 용품 주문 #" + orderNumber;
    }
}