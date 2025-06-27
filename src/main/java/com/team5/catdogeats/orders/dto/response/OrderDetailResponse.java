package com.team5.catdogeats.orders.dto.response;

import com.team5.catdogeats.orders.domain.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 상세 조회 응답 DTO (record 타입)
 * API: GET /v1/buyers/orders/{order-number}
 */
public record OrderDetailResponse(
        String orderId,
        Long orderNumber,
        LocalDateTime orderDate,
        OrderStatus orderStatus,
        RecipientInfo recipientInfo,
        PaymentInfo paymentInfo,
        List<OrderItemDetail> orderItems
) {

    /**
     * 받는 사람 정보
     */
    public record RecipientInfo(
            String recipientName,
            String recipientPhone,
            String fullAddress,
            String deliveryRequest
    ) {
        /**
         * 주소 정보를 포맷팅하여 전체 주소 생성
         */
        public static RecipientInfo of(String recipientName, String recipientPhone,
                                       String city, String district, String neighborhood,
                                       String streetAddress, String detailAddress,
                                       String deliveryRequest) {
            String fullAddress = String.format("%s %s %s %s %s",
                    city, district, neighborhood, streetAddress,
                    detailAddress != null ? detailAddress : "").trim();

            return new RecipientInfo(recipientName, recipientPhone, fullAddress, deliveryRequest);
        }
    }

    /**
     * 결제 정보
     */
    public record PaymentInfo(
            Long totalProductPrice,   // 총 상품 가격
            Long discountAmount,      // 할인 금액
            Long deliveryFee,         // 배송비
            Long totalPaymentAmount   // 총 결제 금액
    ) {
        /**
         * 결제 정보 생성 (할인과 배송비 계산)
         */
        public static PaymentInfo of(Long totalProductPrice, Long discountAmount, Long deliveryFee) {
            Long totalPaymentAmount = totalProductPrice - discountAmount + deliveryFee;
            return new PaymentInfo(totalProductPrice, discountAmount, deliveryFee, totalPaymentAmount);
        }
    }

    /**
     * 주문 상품 상세 정보
     */
    public record OrderItemDetail(
            String orderItemId,
            String productId,
            String productName,
            Integer quantity,
            Long unitPrice,
            Long totalPrice
    ) {}
}