package com.team5.catdogeats.orders.domain.enums;

/**
 * 주문 상태 열거형
 *
 * 이커머스 주문의 전체 생명주기를 나타냅니다.
 */
public enum OrderStatus {

    /**
     * 결제 대기
     * 주문이 생성되었지만 아직 결제가 완료되지 않은 상태
     */
    PAYMENT_PENDING,

    /**
     * 결제 완료
     * 결제가 성공적으로 완료된 상태
     */
    PAYMENT_COMPLETED,

    /**
     * 상품 준비 중
     * 결제 완료 후 판매자가 상품을 준비하는 상태
     */
    PREPARING,

    /**
     * 배송 준비 완료
     * 상품 준비가 완료되어 배송을 시작할 수 있는 상태
     */
    READY_FOR_SHIPMENT,

    /**
     * 배송 중
     * 상품이 배송 중인 상태
     */
    IN_DELIVERY,

    /**
     * 배송 완료
     * 고객에게 상품이 성공적으로 배송된 상태
     */
    DELIVERED,

    /**
     * 주문 취소
     * 결제 전 또는 결제 후 주문이 취소된 상태
     */
    CANCELLED,

    /**
     * 환불 처리 중
     * 환불이 요청되어 처리 중인 상태
     */
    REFUND_PROCESSING,

    /**
     * 환불 완료
     * 환불이 완료된 상태
     */
    REFUNDED
}