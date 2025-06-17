package com.team5.catdogeats.orders.domain.dto.response;

import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 주문 생성 응답 DTO
 * API: POST /v1/buyers/orders
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateResponse {

    /**
     * 생성된 주문의 고유 ID
     */
    private String orderId;

    /**
     * 주문 번호 (사용자에게 표시되는 번호)
     */
    private Long orderNumber;

    /**
     * 주문 상태
     */
    private OrderStatus orderStatus;

    /**
     * 총 주문 금액
     */
    private Long totalPrice;

    /**
     * 주문 생성 시간
     */
    private ZonedDateTime createdAt;

    /**
     * 주문 상품 목록
     */
    private List<OrderItemResponse> orderItems;

    /**
     * 토스 페이먼츠 결제를 위한 정보
     */
    private TossPaymentInfo tossPaymentInfo;

    /**
     * 주문 상품 응답 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemResponse {

        /**
         * 주문 아이템 ID
         */
        private String orderItemId;

        /**
         * 상품 ID
         */
        private String productId;

        /**
         * 상품명
         */
        private String productName;

        /**
         * 주문 수량
         */
        private Integer quantity;

        /**
         * 주문 시점 상품 가격 (단가)
         */
        private Long unitPrice;

        /**
         * 해당 상품의 총 가격 (단가 × 수량)
         */
        private Long totalPrice;
    }

    /**
     * 토스 페이먼츠 결제 진행을 위한 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TossPaymentInfo {

        /**
         * 토스 페이먼츠에 전달할 주문 ID
         * (UUID 형태의 고유값)
         */
        private String tossOrderId;

        /**
         * 토스 페이먼츠에 표시될 주문명
         * 예: "강아지 사료 외 2건"
         */
        private String orderName;

        /**
         * 결제할 총 금액
         */
        private Long amount;

        /**
         * 구매자 정보
         */
        private String customerName;
        private String customerEmail;

        /**
         * 결제 성공 시 리디렉션 URL
         */
        private String successUrl;

        /**
         * 결제 실패 시 리디렉션 URL
         */
        private String failUrl;

        /**
         * 토스 페이먼츠 클라이언트 키
         * (클라이언트에서 SDK 초기화에 사용)
         */
        private String clientKey;
    }
}