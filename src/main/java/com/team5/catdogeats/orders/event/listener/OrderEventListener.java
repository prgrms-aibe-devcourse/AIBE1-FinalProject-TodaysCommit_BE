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
import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.service.StockReservationService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 주문 이벤트 리스너 (재고 예약 시스템 적용)
 * 주문 생성 이벤트를 처리하는 이벤트 리스너입니다.
 * 기존 즉시 재고 차감 방식에서 재고 예약 시스템으로 전환하여
 * 더 안전하고 확장 가능한 재고 관리를 제공합니다.
 * 처리 순서:
 * 1. 재고 예약 처리
 * 2. 결제 정보 생성
 * 3. 사용자 알림 처리
 * 각 리스너 메서드는 독립적인 트랜잭션으로 동작하여,
 * 하나의 작업 실패가 다른 작업에 영향을 주지 않도록 설계되었습니다.
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
    private final UserRepository userRepository; // UserRepository 주입

    /**
     * 재고 예약 처리 리스너 (EDA + 재고 예약 시스템)
     * 주문 생성 트랜잭션이 커밋된 후에 실행되어 재고를 예약합니다.
     * 기존 즉시 차감 방식과 달리 예약 시스템을 통해 안전한 재고 관리를 제공합니다.
     * 예약 실패 시 주문 상태를 CANCELLED로 변경하는 보상 트랜잭션을 수행합니다.
     * @param event 주문 생성 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStockReservation(OrderCreatedEvent event) {
        log.info("재고 예약 처리 시작: orderId={}, orderNumber={}",
                event.getOrderId(), event.getOrderNumber());

        try {
            // 주문 정보 조회
            Orders order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + event.getOrderId()));

            // 예약 요청 목록 생성
            List<StockReservationService.ReservationRequest> reservationRequests = createReservationRequests(event);

            // 일괄 재고 예약 생성
            List<StockReservation> reservations = stockReservationService.createBulkReservations(order, reservationRequests);

            log.info("재고 예약 완료: orderId={}, 예약된 상품 개수={}",
                    event.getOrderId(), reservations.size());

        } catch (IllegalArgumentException e) {
            // 재고 부족 등 비즈니스 로직 예외
            log.error("재고 예약 실패 (재고 부족): orderId={}, error={}", event.getOrderId(), e.getMessage());
            performStockReservationCompensation(event.getOrderId(), "재고 부족: " + e.getMessage());

        } catch (OptimisticLockingFailureException e) {
            // 동시성 제어 실패 (재시도 로직은 StockReservationService에서 처리)
            log.error("재고 예약 실패 (동시성 충돌): orderId={}, error={}", event.getOrderId(), e.getMessage());
            performStockReservationCompensation(event.getOrderId(), "동시성 충돌로 인한 재고 예약 실패");

        } catch (Exception e) {
            // 기타 예외
            log.error("재고 예약 실패 (시스템 오류): orderId={}, error={}", event.getOrderId(), e.getMessage(), e);
            performStockReservationCompensation(event.getOrderId(), "시스템 오류: " + e.getMessage());
        }
    }

    /**
     * 주문 이벤트로부터 예약 요청 목록 생성
     *
     * @param event 주문 생성 이벤트
     * @return 예약 요청 목록
     */
    private List<StockReservationService.ReservationRequest> createReservationRequests(OrderCreatedEvent event) {
        return event.getOrderItems().stream()
                .map(item -> {
                    Products product = productRepository.findById(item.getProductId()) // UUID.fromString 제거
                            .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다: " + item.getProductId()));
                    return new StockReservationService.ReservationRequest(product, item.getQuantity());
                })
                .collect(Collectors.toList());
    }

    /**
     * 재고 예약 실패 시 보상 트랜잭션
     * 주문 상태를 CANCELLED로 변경하고 관련 로그를 기록합니다.
     * 독립적인 트랜잭션으로 실행되어 원본 트랜잭션과 분리됩니다.
     * @param orderId 주문 ID
     * @param errorMessage 오류 메시지
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void performStockReservationCompensation(UUID orderId, String errorMessage) {
        try {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            // 주문 상태를 취소로 변경
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            log.warn("재고 예약 실패로 주문 취소 처리: orderId={}, reason={}", orderId, errorMessage);

        } catch (Exception compensationError) {
            log.error("보상 트랜잭션 실패: orderId={}, originalError={}, compensationError={}",
                    orderId, errorMessage, compensationError.getMessage(), compensationError);
        }
    }

    /**
     * 결제 정보 생성 처리 리스너
     * 재고 예약이 성공한 후에 토스 페이먼츠를 위한 결제 정보를 생성합니다.
     * 주문이 이미 취소된 상태라면 결제 정보 생성을 건너뜁니다.
     * @param event 주문 생성 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentInfoCreation(OrderCreatedEvent event) {
        log.info("결제 정보 생성 처리 시작: orderId={}, orderNumber={}",
                event.getOrderId(), event.getOrderNumber());

        try {
            // 주문 상태 확인 (재고 예약 실패로 취소된 경우 건너뜀)
            Orders order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + event.getOrderId()));

            if (order.getOrderStatus() == OrderStatus.CANCELLED) {
                log.info("취소된 주문이므로 결제 정보 생성을 건너뜁니다: orderId={}", event.getOrderId());
                return;
            }

            // 이미 결제 정보가 생성되었는지 확인
            boolean paymentExists = paymentRepository.existsByOrdersIdAndStatus(
                    event.getOrderId(), PaymentStatus.PENDING);

            if (paymentExists) {
                log.info("이미 결제 정보가 존재합니다: orderId={}", event.getOrderId());
                return;
            }

            // 구매자 정보 조회
            Buyers buyer = buyerRepository.findById(event.getUserId()) // findByUserId -> findById 로 수정
                    .orElseThrow(() -> new NoSuchElementException("구매자 정보를 찾을 수 없습니다: " + event.getUserId()));

            // 토스 페이먼츠 결제 정보 생성
            Payments payment = Payments.builder()
                    .orders(order)
                    .buyers(buyer)
                    .method(PaymentMethod.TOSS) // 결제 수단 추가
                    .status(PaymentStatus.PENDING)
                    .tossPaymentKey(generateTossPaymentKey(order.getOrderNumber()))
                    .build(); // amount 필드 제거

            paymentRepository.save(payment);

            log.info("결제 정보 생성 완료: orderId={}, paymentId={}, amount={}",
                    event.getOrderId(), payment.getId(), event.getTotalPrice()); // getTotalAmount -> getTotalPrice

        } catch (Exception e) {
            log.error("결제 정보 생성 실패: orderId={}, error={}", event.getOrderId(), e.getMessage(), e);
            // 결제 정보 생성 실패는 주문을 취소하지 않음 (재시도 가능)
        }
    }

    /**
     * 토스 페이먼츠 키 생성
     * 주문 번호를 기반으로 고유한 결제 키를 생성합니다.
     *
     * @param orderNumber 주문 번호
     * @return 토스 페이먼츠 키
     */
    private String generateTossPaymentKey(Long orderNumber) {
        return "catdogeats_" + orderNumber + "_" + System.currentTimeMillis();
    }

    /**
     * 사용자 알림 처리 리스너 (비동기)
     * 주문 생성 알림을 비동기로 처리합니다.
     * 외부 알림 서비스의 장애가 주문 프로세스에 영향을 주지 않도록 분리되어 있습니다.
     * @param event 주문 생성 이벤트
     */
    @Async
    @EventListener
    public void handleUserNotification(OrderCreatedEvent event) {
        log.info("사용자 알림 처리 시작: orderId={}, userId={}",
                event.getOrderId(), event.getUserId());

        try {
            // 주문 상태 재확인 (취소된 주문은 알림 발송하지 않음)
            Orders order = orderRepository.findById(event.getOrderId()).orElse(null);
            if (order == null || order.getOrderStatus() == OrderStatus.CANCELLED) {
                log.info("취소된 주문이므로 알림을 발송하지 않습니다: orderId={}", event.getOrderId());
                return;
            }

            // 사용자 이름 조회를 위해 UserRepository 사용
            String userName = userRepository.findById(event.getUserId())
                    .map(Users::getName)
                    .orElse("고객");


            // TODO: 실제 알림 서비스 연동
            // - 이메일 알림 발송
            // - SMS 알림 발송
            // - 푸시 알림 발송
            // - 슬랙/디스코드 알림 등

            log.info("주문 생성 알림 발송 완료: orderId={}, orderNumber={}, userName={}",
                    event.getOrderId(), event.getOrderNumber(), userName); // getUserName -> userName

        } catch (Exception e) {
            log.error("사용자 알림 발송 실패: orderId={}, error={}", event.getOrderId(), e.getMessage(), e);
            // 알림 실패는 주문에 영향을 주지 않음
        }
    }

    /**
     * 주문 처리 완료 감사 로깅 (동기)
     * 주문 처리 과정을 감사하고 모니터링을 위한 로그를 기록합니다.
     * @param event 주문 생성 이벤트
     */
    @EventListener
    public void handleOrderProcessingComplete(OrderCreatedEvent event) {
        log.info("주문 처리 감사 로그: orderId={}, orderNumber={}, userId={}, amount={}, itemCount={}, timestamp={}",
                event.getOrderId(),
                event.getOrderNumber(),
                event.getUserId(),
                event.getTotalPrice(), // getTotalAmount -> getTotalPrice
                event.getOrderItems().size(),
                event.getEventOccurredAt()); // getCreatedAt -> getEventOccurredAt
    }
}