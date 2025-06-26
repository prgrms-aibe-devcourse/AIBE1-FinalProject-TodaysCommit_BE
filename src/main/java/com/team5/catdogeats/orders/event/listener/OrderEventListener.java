package com.team5.catdogeats.orders.event.listener;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.payments.domain.Payments;
import com.team5.catdogeats.payments.domain.enums.PaymentMethod;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.service.StockReservationService;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final StockReservationService stockReservationService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final BuyerRepository buyerRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(transactionManager = "jpaTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void handleStockReservation(OrderCreatedEvent event) {
        String orderId = event.getOrderId();
        log.info("재고 예약 처리 시작: orderId={}, orderNumber={}, 상품 개수={}, 쿠폰할인={}",
                orderId, event.getOrderNumber(), event.getOrderItemCount(),
                event.isCouponApplied() ? event.getCouponDiscountRate() + "%" : "없음");

        try {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            List<StockReservationService.ReservationRequest> reservationRequests = createReservationRequests(event.getOrderItems());

            List<StockReservation> reservations = stockReservationService.createBulkReservations(order, reservationRequests);

            log.info("재고 예약 완료: orderId={}, 예약된 상품 개수={}, 총 수량={}",
                    orderId, reservations.size(), event.getTotalQuantity());

        } catch (NoSuchElementException e) {
            log.error("재고 예약 실패 (주문 없음): orderId={}, error={}", orderId, e.getMessage());

        } catch (IllegalArgumentException e) {
            log.error("재고 예약 실패 (재고 부족): orderId={}, error={}", orderId, e.getMessage());
            performStockReservationCompensation(orderId, "재고 부족: " + e.getMessage());

        } catch (OptimisticLockingFailureException e) {
            log.error("재고 예약 실패 (동시성 충돌): orderId={}, error={}", orderId, e.getMessage());
            performStockReservationCompensation(orderId, "동시성 충돌로 인한 재고 예약 실패");

        } catch (Exception e) {
            log.error("재고 예약 실패 (시스템 오류): orderId={}, error={}", orderId, e.getMessage(), e);
            performStockReservationCompensation(orderId, "시스템 오류: " + e.getMessage());
        }
    }

    private List<StockReservationService.ReservationRequest> createReservationRequests(List<OrderItemInfo> orderItems) {
        return orderItems.stream()
                .map(orderItem -> {
                    Products product = productRepository.findById(orderItem.productId())
                            .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다: " + orderItem.productId()));
                    return new StockReservationService.ReservationRequest(product, orderItem.quantity());
                })
                .collect(Collectors.toList());
    }

    @Transactional(transactionManager = "jpaTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void performStockReservationCompensation(String orderId, String reason) {
        try {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("보상 처리할 주문을 찾을 수 없습니다: " + orderId));

            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            log.warn("재고 예약 실패 보상 처리 완료: orderId={}, reason={}", orderId, reason);

        } catch (Exception e) {
            log.error("재고 예약 보상 처리 실패: orderId={}, reason={}, error={}",
                    orderId, reason, e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(transactionManager = "jpaTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentInfoCreation(OrderCreatedEvent event) {
        String orderId = event.getOrderId();

        log.info("결제 정보 생성 시작: orderId={}, orderNumber={}, 최종금액={}원",
                orderId, event.getOrderNumber(), event.getTotalPrice());

        try {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            if (order.getOrderStatus() == OrderStatus.CANCELLED) {
                log.info("취소된 주문 - 결제 정보 생성 건너뜀: orderId={}", orderId);
                return;
            }

            // 1. DTO로 구매자 정보 조회
            BuyerDTO buyerInfo = buyerRepository.findOnlyBuyerByProviderAndProviderId(
                            event.getUserProvider(), event.getUserProviderId())
                    .orElseThrow(() -> new NoSuchElementException("구매자 정보를 찾을 수 없습니다"));

            // 2. DTO에서 ID를 사용하여 Buyers 엔티티 참조 가져오기
            Buyers buyer = buyerRepository.getReferenceById(buyerInfo.userId());

            if (paymentRepository.findByOrdersId(orderId).isPresent()) {
                log.warn("이미 결제 정보가 존재하여 생성 건너뜀: orderId={}", orderId);
                return;
            }

            // 3. 빌더에 DTO가 아닌 엔티티 참조를 전달
            Payments payment = Payments.builder()
                    .orders(order)
                    .buyers(buyer)
                    .amount(event.getTotalPrice())
                    .method(PaymentMethod.TOSS)
                    .status(PaymentStatus.PENDING)
                    .build();

            paymentRepository.save(payment);

            log.info("결제 정보 생성 완료: orderId={}, paymentId={}, amount={}원",
                    orderId, payment.getId(), payment.getAmount());

        } catch (NoSuchElementException e) {
            log.error("결제 정보 생성 실패 (데이터 없음): orderId={}, error={}", orderId, e.getMessage());

        } catch (Exception e) {
            log.error("결제 정보 생성 실패 (시스템 오류): orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void handleUserNotification(OrderCreatedEvent event) {
        String orderId = event.getOrderId();
        log.info("사용자 알림 처리 시작: orderId={}, orderNumber={}",
                orderId, event.getOrderNumber());

        try {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            if (order.getOrderStatus() == OrderStatus.CANCELLED) {
                log.info("취소된 주문 - 알림 발송 건너뜀: orderId={}", orderId);
                return;
            }

            String productInfo = event.getFirstProductName() +
                    (event.getOrderItemCount() > 1 ? String.format(" 외 %d개", event.getOrderItemCount() - 1) : "");

            String discountInfo = "";
            if (event.isCouponApplied()) {
                Long discountAmount = event.getOriginalTotalPrice() - event.getTotalPrice();
                discountInfo = String.format("\n🎟️ 쿠폰 할인: %.1f%% (-%,d원)",
                        event.getCouponDiscountRate(), discountAmount);
            }

            log.info("""
                    [Catdogeats] 주문이 완료되었습니다! 🐱🐶
                    주문번호: {}
                    상품: {}{}
                    총 금액: {}원
                    결제를 진행해 주세요.
                    """,
                    event.getOrderNumber(),
                    productInfo,
                    discountInfo,
                    String.format("%,d", event.getTotalPrice())
            );

            log.info("사용자 알림 발송 완료: orderId={}, userId={}, itemCount={}, 쿠폰할인={}",
                    orderId, event.getUserId(), event.getOrderItemCount(),
                    event.isCouponApplied() ? "적용됨" : "없음");

        } catch (Exception e) {
            log.error("사용자 알림 발송 실패: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    @EventListener
    public void handleOrderProcessingComplete(OrderCreatedEvent event) {
        log.info("=== 주문 처리 감사 로그 ===");
        log.info("주문 ID: {}", event.getOrderId());
        log.info("주문 번호: {}", event.getOrderNumber());
        log.info("사용자 ID: {}", event.getUserId());

        if (event.isCouponApplied()) {
            Long discountAmount = event.getOriginalTotalPrice() - event.getTotalPrice();
            log.info("원가 금액: {}원", String.format("%,d", event.getOriginalTotalPrice()));
            log.info("쿠폰 할인: {}% (-{}원)", event.getCouponDiscountRate(), String.format("%,d", discountAmount));
            log.info("최종 금액: {}원", String.format("%,d", event.getTotalPrice()));
        } else {
            log.info("주문 금액: {}원 (할인 없음)", String.format("%,d", event.getTotalPrice()));
        }

        log.info("상품 개수: {}개", event.getOrderItemCount());
        log.info("총 수량: {}개", event.getTotalQuantity());
        log.info("첫 번째 상품: {}", event.getFirstProductName());
        log.info("이벤트 발생 시각: {}", event.getEventOccurredAt());

        event.getOrderItems().forEach(item ->
                log.debug("- 상품: {} (ID: {}), 수량: {}개, 단가: {}원, 총가격: {}원",
                        item.productName(), item.productId(), item.quantity(),
                        String.format("%,d", item.unitPrice()), String.format("%,d", item.totalPrice()))
        );

        log.info("=== 감사 로그 완료 ===");
    }
}