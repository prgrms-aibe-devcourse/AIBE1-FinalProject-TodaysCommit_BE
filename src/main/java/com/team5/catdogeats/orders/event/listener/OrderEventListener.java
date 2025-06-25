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

/**
 * 주문 이벤트 리스너 (쿠폰 할인 지원)
 * 주문 생성 이벤트를 처리하는 이벤트 리스너입니다.
 * 재고 예약 시스템을 통해 안전하고 확장 가능한 재고 관리를 제공합니다.
 * 쿠폰 할인 정보를 활용하여 더 풍부한 알림 및 로깅을 제공합니다.
 * 처리 순서:
 * 1. 재고 예약 처리 (TransactionalEventListener)
 * 2. 결제 정보 생성 (TransactionalEventListener)
 * 3. 사용자 알림 처리 (비동기, 할인 정보 포함)
 * 4. 감사 로깅 (동기, 할인 정보 포함)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final StockReservationService stockReservationService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final BuyerRepository buyerRepository;

    /**
     * 재고 예약 처리 리스너 (Record DTO 적용)
     * 주문 생성 트랜잭션이 커밋된 후에 실행되어 재고를 예약합니다.
     * OrderItemInfo Record의 메서드를 사용하여 타입 안전한 데이터 접근을 수행합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(transactionManager = "jpaTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void handleStockReservation(OrderCreatedEvent event) {
        String orderId = event.getOrderId();
        log.info("재고 예약 처리 시작: orderId={}, orderNumber={}, 상품 개수={}, 쿠폰할인={}",
                orderId, event.getOrderNumber(), event.getOrderItemCount(),
                event.isCouponApplied() ? event.getCouponDiscountRate() + "%" : "없음");

        try {
            // 주문 정보 조회
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            // 예약 요청 목록 생성 (Record DTO 메서드 활용)
            List<StockReservationService.ReservationRequest> reservationRequests = createReservationRequests(event.getOrderItems());

            // 일괄 재고 예약 생성
            List<StockReservation> reservations = stockReservationService.createBulkReservations(order, reservationRequests);

            log.info("재고 예약 완료: orderId={}, 예약된 상품 개수={}, 총 수량={}",
                    orderId, reservations.size(), event.getTotalQuantity());

        } catch (NoSuchElementException e) {
            // 주문을 찾을 수 없는 경우 - 보상 트랜잭션 불필요
            log.error("재고 예약 실패 (주문 없음): orderId={}, error={}", orderId, e.getMessage());

        } catch (IllegalArgumentException e) {
            // 재고 부족 등 비즈니스 로직 예외
            log.error("재고 예약 실패 (재고 부족): orderId={}, error={}", orderId, e.getMessage());
            performStockReservationCompensation(orderId, "재고 부족: " + e.getMessage());

        } catch (OptimisticLockingFailureException e) {
            // 동시성 제어 실패
            log.error("재고 예약 실패 (동시성 충돌): orderId={}, error={}", orderId, e.getMessage());
            performStockReservationCompensation(orderId, "동시성 충돌로 인한 재고 예약 실패");

        } catch (Exception e) {
            // 기타 예외
            log.error("재고 예약 실패 (시스템 오류): orderId={}, error={}", orderId, e.getMessage(), e);
            performStockReservationCompensation(orderId, "시스템 오류: " + e.getMessage());
        }
    }

    /**
     * 주문 아이템 목록으로부터 예약 요청 목록 생성 (Record DTO 적용)
     * OrderItemInfo Record의 메서드를 사용하여 타입 안전한 데이터 접근을 수행합니다.
     */
    private List<StockReservationService.ReservationRequest> createReservationRequests(List<OrderItemInfo> orderItems) {
        return orderItems.stream()
                .map(orderItem -> StockReservationService.ReservationRequest.builder()
                        .productId(orderItem.productId())
                        .quantity(orderItem.quantity())
                        .build())
                .toList();
    }

    /**
     * 재고 예약 실패에 대한 보상 트랜잭션
     * 별도의 트랜잭션에서 실행되어 주문 상태를 CANCELLED로 변경합니다.
     */
    @Transactional(transactionManager = "jpaTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void performStockReservationCompensation(String orderId, String reason) {
        try {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("보상 처리할 주문을 찾을 수 없습니다: " + orderId));

            order.changeOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            log.warn("재고 예약 실패 보상 처리 완료: orderId={}, reason={}", orderId, reason);

        } catch (Exception e) {
            log.error("재고 예약 보상 처리 실패: orderId={}, reason={}, error={}",
                    orderId, reason, e.getMessage(), e);
        }
    }

    /**
     * 결제 정보 생성 리스너 (쿠폰 할인 정보 포함)
     * 주문 생성 트랜잭션이 커밋된 후에 결제 정보를 생성합니다.
     * 취소된 주문은 건너뛰는 스마트 처리로 불필요한 작업을 방지합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(transactionManager = "jpaTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentInfoCreation(OrderCreatedEvent event) {
        String orderId = event.getOrderId();

        log.info("결제 정보 생성 시작: orderId={}, orderNumber={}, 최종금액={}원",
                orderId, event.getOrderNumber(), event.getTotalPrice());

        try {
            // 주문 상태 확인 (재고 예약 실패로 취소된 주문은 건너뜀)
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            if (order.getOrderStatus() == OrderStatus.CANCELLED) {
                log.info("취소된 주문 - 결제 정보 생성 건너뜀: orderId={}", orderId);
                return;
            }

            // 구매자 정보 조회
            BuyerDTO buyer = buyerRepository.findOnlyBuyerByProviderAndProviderId(
                            event.getUserProvider(), event.getUserProviderId())
                    .orElseThrow(() -> new NoSuchElementException("구매자 정보를 찾을 수 없습니다"));

            // 결제 정보 생성 (쿠폰 할인 적용된 최종 금액으로)
            Payments payment = Payments.builder()
                    .orders(order)
                    .amount(event.getTotalPrice())  // 최종 할인 적용 금액
                    .paymentMethod(PaymentMethod.CARD)  // 기본값
                    .paymentStatus(PaymentStatus.PENDING)
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

    /**
     * 사용자 알림 처리 (쿠폰 할인 정보 포함)
     * 비동기로 처리되어 메인 플로우에 영향을 주지 않습니다.
     * OrderCreatedEvent의 편의 메서드를 활용하여 알림 메시지를 구성합니다.
     */
    @Async
    @EventListener
    public void handleUserNotification(OrderCreatedEvent event) {
        String orderId = event.getOrderId();
        log.info("사용자 알림 처리 시작: orderId={}, orderNumber={}",
                orderId, event.getOrderNumber());

        try {
            // 주문 상태 확인 (취소된 주문은 알림 발송하지 않음)
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            if (order.getOrderStatus() == OrderStatus.CANCELLED) {
                log.info("취소된 주문 - 알림 발송 건너뜀: orderId={}", orderId);
                return;
            }

            // Record의 편의 메서드를 활용한 알림 메시지 구성 및 발송
            String productInfo = event.getFirstProductName() +
                    (event.getOrderItemCount() > 1 ? String.format(" 외 %d개", event.getOrderItemCount() - 1) : "");

            // 쿠폰 할인 정보 포함한 메시지 구성
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
            // 알림 발송 실패는 치명적이지 않으므로 예외를 던지지 않음
        }
    }

    /**
     * 주문 처리 완료 감사 로깅 (쿠폰 할인 정보 포함)
     * 주문 생성 프로세스의 모든 단계가 완료된 후 감사 로그를 기록합니다.
     * 모니터링 및 비즈니스 분석 목적으로 사용됩니다.
     */
    @EventListener
    public void handleOrderProcessingComplete(OrderCreatedEvent event) {
        // Record의 편의 메서드들을 활용한 상세 로깅
        log.info("=== 주문 처리 감사 로그 ===");
        log.info("주문 ID: {}", event.getOrderId());
        log.info("주문 번호: {}", event.getOrderNumber());
        log.info("사용자 ID: {}", event.getUserId());

        // 쿠폰 할인 정보 포함한 금액 정보
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

        // 주문 아이템 상세 정보 (Record의 불변성 활용)
        event.getOrderItems().forEach(item ->
                log.debug("- 상품: {} (ID: {}), 수량: {}개, 단가: {}원, 총가격: {}원",
                        item.productName(), item.productId(), item.quantity(),
                        String.format("%,d", item.unitPrice()), String.format("%,d", item.totalPrice()))
        );

        log.info("=== 감사 로그 완료 ===");
    }
}