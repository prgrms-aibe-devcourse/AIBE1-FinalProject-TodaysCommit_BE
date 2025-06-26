package com.team5.catdogeats.orders.event;

import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 생성 이벤트 (쿠폰 할인 적용)
 * 주문이 성공적으로 생성되었을 때 발행되는 이벤트입니다.
 * 이벤트 리스너들이 이 이벤트를 구독하여 후속 작업들을 수행합니다.
 * 쿠폰 할인 정보를 포함하여 전체 주문 금액에 대한 할인 처리를 지원합니다.
 */
@Getter
@Builder
@RequiredArgsConstructor
public class OrderCreatedEvent {

    // 생성된 주문의 고유 식별자
    private final String orderId;

    // 주문 번호 (사용자에게 노출되는 번호)
    private final Long orderNumber;

    // 주문한 사용자의 고유 식별자
    private final String userId;

    // 사용자 인증 정보 (Provider + ProviderId)
    private final String userProvider;
    private final String userProviderId;

    // 주문 원가 총 금액 (할인 적용 전)
    private final Long originalTotalPrice;

    // 쿠폰 할인률 (%)
    private final Double couponDiscountRate;

    // 최종 주문 금액 (쿠폰 할인 적용 후)
    private final Long finalTotalPrice;

    // 주문 아이템 정보 목록 (원가 기준)
    private final List<OrderItemInfo> orderItems;

    // 이벤트 발생 시각
    private final LocalDateTime eventOccurredAt;

    /**
     * 이벤트 생성을 위한 정적 팩토리 메서드 (쿠폰 할인 적용)
     *
     * @param orderId 주문 ID
     * @param orderNumber 주문 번호
     * @param userId 사용자 ID
     * @param userProvider 사용자 인증 제공자
     * @param userProviderId 사용자 인증 제공자 ID
     * @param originalTotalPrice 원가 총 금액
     * @param couponDiscountRate 쿠폰 할인률 (%)
     * @param finalTotalPrice 최종 금액 (할인 적용 후)
     * @param orderItems 주문 아이템 목록
     * @return OrderCreatedEvent 인스턴스
     */
    public static OrderCreatedEvent of(
            String orderId,
            Long orderNumber,
            String userId,
            String userProvider,
            String userProviderId,
            Long originalTotalPrice,
            Double couponDiscountRate,
            Long finalTotalPrice,
            List<OrderItemInfo> orderItems) {

        return OrderCreatedEvent.builder()
                .orderId(orderId)
                .orderNumber(orderNumber)
                .userId(userId)
                .userProvider(userProvider)
                .userProviderId(userProviderId)
                .originalTotalPrice(originalTotalPrice)
                .couponDiscountRate(couponDiscountRate)
                .finalTotalPrice(finalTotalPrice)
                .orderItems(orderItems)
                .eventOccurredAt(LocalDateTime.now())
                .build();
    }

    /**
     * 쿠폰 할인 적용 여부 확인
     */
    public boolean isCouponApplied() {
        return couponDiscountRate != null && couponDiscountRate > 0;
    }

    /**
     * 총 주문 금액 조회 (하위 호환성 메서드)
     * 기존 코드와의 호환성을 위해 최종 할인 적용 금액을 반환합니다.
     * @return 최종 주문 금액 (할인 적용 후)
     */
    public Long getTotalPrice() {
        return finalTotalPrice;
    }



    /**
     * 주문 아이템 개수 조회
     */
    public int getOrderItemCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    /**
     * 총 주문 수량 계산
     */
    public int getTotalQuantity() {
        return orderItems != null ?
                orderItems.stream().mapToInt(OrderItemInfo::quantity).sum() : 0;
    }

    /**
     * 첫 번째 상품명 조회 (알림 메시지용)
     * 다중 상품 주문 시 "상품명 외 N개" 형태로 사용할 수 있습니다.
     */
    public String getFirstProductName() {
        return orderItems != null && !orderItems.isEmpty() ?
                orderItems.get(0).productName() : "";
    }


}