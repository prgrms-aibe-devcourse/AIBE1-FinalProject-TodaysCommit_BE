package com.team5.catdogeats.orders.event;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 주문 생성 이벤트
 *
 * 주문이 성공적으로 생성되었을 때 발행되는 이벤트입니다.
 * 이벤트 리스너들이 이 이벤트를 구독하여 후속 작업들을 수행합니다.
 *
 * 이벤트 기반 아키텍처(EDA)의 핵심 컴포넌트로,
 * 주문 생성과 부가 로직(재고 차감, 결제 정보 생성)을 분리합니다.
 */
@Getter
@Builder
@RequiredArgsConstructor
public class OrderCreatedEvent {

    // 생성된 주문의 고유 식별자
    private final UUID orderId;

    // 주문 번호 (사용자에게 노출되는 번호)
    private final Long orderNumber;

    // 주문한 사용자의 고유 식별자
    private final UUID userId;

    // 사용자 인증 정보 (Provider + ProviderId)
    private final String userProvider;
    private final String userProviderId;

    // 주문 총 금액
    private final Long totalPrice;

    // 주문 아이템 정보 목록
    private final List<OrderItemInfo> orderItems;

    // 이벤트 발생 시각
    private final LocalDateTime eventOccurredAt;

    /*
     * 주문 아이템 정보를 담는 내부 클래스
     * 이벤트 리스너에서 재고 차감 등의 작업을 수행할 때 필요한 정보를 포함합니다.
     */
    @Getter
    @Builder
    @RequiredArgsConstructor
    public static class OrderItemInfo {
        //상품 고유 식별자
        private final UUID productId;

        //상품명 (로깅 및 알림용)
        private final String productName;

        //주문 수량
        private final Integer quantity;

        //상품 단가
        private final Long unitPrice;

        //해당 아이템의 총 가격 (단가 * 수량)
        private final Long totalPrice;
    }

    /**
     * 이벤트 생성을 위한 정적 팩토리 메서드
     *
     * @param orderId 주문 ID
     * @param orderNumber 주문 번호
     * @param userId 사용자 ID
     * @param userProvider 사용자 인증 제공자
     * @param userProviderId 사용자 인증 제공자 ID
     * @param totalPrice 주문 총 금액
     * @param orderItems 주문 아이템 목록
     * @return OrderCreatedEvent 인스턴스
     */
    public static OrderCreatedEvent of(
            UUID orderId,
            Long orderNumber,
            UUID userId,
            String userProvider,
            String userProviderId,
            Long totalPrice,
            List<OrderItemInfo> orderItems) {

        return OrderCreatedEvent.builder()
                .orderId(orderId)
                .orderNumber(orderNumber)
                .userId(userId)
                .userProvider(userProvider)
                .userProviderId(userProviderId)
                .totalPrice(totalPrice)
                .orderItems(orderItems)
                .eventOccurredAt(LocalDateTime.now())
                .build();
    }

    // 로깅을 위한 문자열 표현
    @Override
    public String toString() {
        return String.format("OrderCreatedEvent{orderId=%s, orderNumber=%d, userId=%s, totalPrice=%d, itemCount=%d}",
                orderId, orderNumber, userId, totalPrice, orderItems.size());
    }
}