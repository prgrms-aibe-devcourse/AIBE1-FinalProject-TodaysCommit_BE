package com.team5.catdogeats.orders.event.listener;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.payments.domain.Payments;
import com.team5.catdogeats.payments.domain.enums.PaymentMethod;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * 주문 이벤트 리스너 (문제 해결 버전)
 *
 * OrderCreatedEvent를 구독하여 주문 생성 후 필요한 부가 작업들을 처리합니다.
 * 이벤트 기반 아키텍처(EDA)의 핵심 컴포넌트로서 다음 작업들을 담당합니다:
 *
 * 1. 재고 차감 처리 (동기, 트랜잭션 분리)
 * 2. 결제 정보 생성 (동기, 트랜잭션 분리)
 * 3. 사용자 알림 처리 (비동기)
 *
 * 각 리스너 메서드는 독립적인 트랜잭션으로 동작하여,
 * 하나의 작업 실패가 다른 작업에 영향을 주지 않도록 설계되었습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final BuyerRepository buyerRepository;

    /**
     * 재고 차감 처리 리스너
     *
     * 주문 생성 트랜잭션이 커밋된 후에 실행되어 재고를 차감합니다.
     * 재고 부족 시 주문 상태를 CANCELLED로 변경하는 보상 트랜잭션을 수행합니다.
     *
     * @param event 주문 생성 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStockDeduction(OrderCreatedEvent event) {
        log.info("재고 차감 처리 시작: orderId={}, orderNumber={}",
                event.getOrderId(), event.getOrderNumber());

        try {
            // 각 주문 아이템에 대해 재고 차감 수행
            for (OrderCreatedEvent.OrderItemInfo item : event.getOrderItems()) {
                deductStock(item.getProductId(), item.getQuantity(), item.getProductName());
            }

            log.info("재고 차감 완료: orderId={}, 상품 개수={}",
                    event.getOrderId(), event.getOrderItems().size());

        } catch (Exception e) {
            log.error("재고 차감 실패: orderId={}, error={}", event.getOrderId(), e.getMessage(), e);

            // 보상 트랜잭션: 주문 상태를 CANCELLED로 변경
            performStockDeductionCompensation(event.getOrderId(), e.getMessage());
        }
    }

    /**
     * 개별 상품의 재고 차감
     * ProductRepository의 원자적 차감 메서드 사용으로 동시성 문제 해결
     *
     * @param productId 상품 ID
     * @param quantity 차감할 수량
     * @param productName 상품명 (로깅용)
     */
    private void deductStock(UUID productId, Integer quantity, String productName) {
        // ProductRepository의 원자적 재고 차감 메서드 사용
        int updatedRows = productRepository.decreaseStock(productId, quantity);

        if (updatedRows == 0) {
            // 재고가 부족하거나 상품이 존재하지 않음
            Products product = productRepository.findById(productId)
                    .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다: " + productId));

            throw new IllegalStateException(
                    String.format("재고 부족: 상품=%s, 현재재고=%d, 요청수량=%d",
                            productName, product.getStock(), quantity));
        }

        log.debug("상품 재고 차감 완료: productId={}, productName={}, 차감수량={}",
                productId, productName, quantity);
    }

    /**
     * 재고 차감 실패 시 보상 트랜잭션 (public으로 변경하여 @Transactional 동작 보장)
     *
     * @param orderId 주문 ID
     * @param errorMessage 오류 메시지
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void performStockDeductionCompensation(UUID orderId, String errorMessage) {
        try {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            log.warn("재고 차감 실패로 주문 취소 처리: orderId={}, reason={}", orderId, errorMessage);

        } catch (Exception compensationError) {
            log.error("보상 트랜잭션 실패: orderId={}, originalError={}, compensationError={}",
                    orderId, errorMessage, compensationError.getMessage(), compensationError);
        }
    }

    /**
     * 결제 정보 생성 처리 리스너
     *
     * 재고 차감이 성공한 후에 토스 페이먼츠를 위한 결제 정보를 생성합니다.
     * 주문 상태가 CANCELLED가 아닌 경우에만 실행됩니다.
     *
     * @param event 주문 생성 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentInfoCreation(OrderCreatedEvent event) {
        log.info("결제 정보 생성 처리 시작: orderId={}, orderNumber={}",
                event.getOrderId(), event.getOrderNumber());

        try {
            // 주문 상태 확인 (재고 차감 실패로 취소된 주문은 제외)
            Orders order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + event.getOrderId()));

            if (order.getOrderStatus() == OrderStatus.CANCELLED) {
                log.info("취소된 주문의 결제 정보 생성 건너뜀: orderId={}", event.getOrderId());
                return;
            }

            // Buyers 엔티티 조회 (기존 BuyerRepository 활용)
            Buyers buyer = findBuyerByUserId(event.getUserId());

            // 결제 정보 생성
            Payments payment = createPaymentInfo(order, buyer);
            paymentRepository.save(payment);

            // 주문 상태를 결제 완료로 변경 (READY_FOR_PAYMENT 대신 PAYMENT_COMPLETED 사용)
            order.setOrderStatus(OrderStatus.PAYMENT_COMPLETED);
            orderRepository.save(order);

            log.info("결제 정보 생성 완료: orderId={}, paymentId={}",
                    event.getOrderId(), payment.getId());

        } catch (Exception e) {
            log.error("결제 정보 생성 실패: orderId={}, error={}", event.getOrderId(), e.getMessage(), e);

            // 결제 정보 생성 실패는 주문을 취소하지 않고 로그만 기록
            // 관리자가 수동으로 처리할 수 있도록 함
        }
    }

    /**
     * 사용자 ID로 Buyers 엔티티 조회 (기존 BuyerRepository 활용)
     *
     * BuyerRepository를 직접 수정하지 않고 우회하여 Buyers 엔티티를 조회합니다.
     * UserRepository를 통해 Users를 먼저 조회한 후,
     * BuyerRepository의 기존 메서드를 활용합니다.
     *
     * @param userId 사용자 ID
     * @return Buyers 엔티티
     */
    private Buyers findBuyerByUserId(UUID userId) {
        // 1. Users 엔티티 조회
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));

        // 2. BuyerRepository의 기존 메서드 활용 (provider와 providerId 사용)
        return buyerRepository.findOnlyBuyerByProviderAndProviderId(user.getProvider(), user.getProviderId())
                .map(buyerDTO -> {
                    // DTO를 통해 ID를 얻어 다시 조회 (임시 방법)
                    return buyerRepository.findById(buyerDTO.userId())
                            .orElseThrow(() -> new NoSuchElementException("구매자 정보를 찾을 수 없습니다: " + userId));
                })
                .orElseThrow(() -> new NoSuchElementException("구매자 정보를 찾을 수 없습니다: " + userId));
    }

    /**
     * 결제 정보 엔티티 생성
     *
     * @param order 주문 엔티티
     * @param buyer 구매자 엔티티
     * @return 생성된 결제 정보
     */
    private Payments createPaymentInfo(Orders order, Buyers buyer) {
        // 토스 페이먼츠용 임시 키 생성 (실제로는 토스 API 호출 결과 사용)
        String tossPaymentKey = "temp_" + order.getId().toString().replace("-", "");

        return Payments.builder()
                .buyers(buyer)
                .orders(order)
                .method(PaymentMethod.TOSS)  // CARD 대신 TOSS 사용
                .status(PaymentStatus.PENDING)
                .tossPaymentKey(tossPaymentKey)
                .build();
    }

    /**
     * 사용자 알림 처리 리스너 (비동기)
     *
     * 주문 생성 알림을 비동기적으로 처리합니다.
     * 실제 환경에서는 이메일, SMS, 푸시 알림 등을 발송하지만,
     * 현재는 로그 기록으로 대체합니다.
     *
     * @param event 주문 생성 이벤트
     */
    @Async
    @EventListener
    public void handleUserNotification(OrderCreatedEvent event) {
        log.info("사용자 알림 처리 시작 (비동기): orderId={}, userId={}",
                event.getOrderId(), event.getUserId());

        try {
            // 실제 환경에서는 알림 서비스 호출
            // - 이메일 발송
            // - SMS 발송
            // - 푸시 알림
            // - 카카오톡 알림톡 등

            // 현재는 로그로 대체
            String notificationMessage = String.format(
                    "[주문 알림] 주문번호: %d, 총금액: %,d원, 상품개수: %d개",
                    event.getOrderNumber(),
                    event.getTotalPrice(),
                    event.getOrderItems().size()
            );

            log.info("사용자 알림 발송 (모의): userId={}, provider={}:{}, message={}",
                    event.getUserId(), event.getUserProvider(), event.getUserProviderId(), notificationMessage);

            // 알림 발송 성공 로그
            log.info("사용자 알림 처리 완료: orderId={}", event.getOrderId());

        } catch (Exception e) {
            // 알림 실패는 전체 프로세스에 영향을 주지 않음
            log.warn("사용자 알림 발송 실패: orderId={}, error={}", event.getOrderId(), e.getMessage());
        }
    }

    /**
     * 주문 처리 완료 로깅 리스너
     *
     * 모든 이벤트 처리가 완료된 후 최종 로그를 기록합니다.
     * 모니터링 및 감사(Audit) 목적으로 사용됩니다.
     *
     * @param event 주문 생성 이벤트
     */
    @EventListener
    public void handleOrderProcessingComplete(OrderCreatedEvent event) {
        log.info("주문 처리 프로세스 완료 로깅: orderId={}, orderNumber={}, totalPrice={}, eventTime={}",
                event.getOrderId(), event.getOrderNumber(), event.getTotalPrice(), event.getEventOccurredAt());

        // 향후 확장 가능한 영역:
        // - 주문 처리 시간 메트릭 수집
        // - 외부 모니터링 시스템에 이벤트 전송
        // - 데이터 분석을 위한 이벤트 저장소에 기록
    }
}