package com.team5.catdogeats.orders.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 주문 생성 요청 DTO
 * API: POST /v1/buyers/orders
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateRequest {

    /**
     * 주문할 상품들의 목록
     */
    @NotEmpty(message = "주문 상품 목록은 비어있을 수 없습니다")
    @Valid
    private List<OrderItemRequest> orderItems;

    /**
     * 배송 주소 정보
     */
    @NotNull(message = "배송 주소 정보는 필수입니다")
    @Valid
    private ShippingAddressRequest shippingAddress;

    /**
     * 결제 관련 정보
     */
    @NotNull(message = "결제 정보는 필수입니다")
    @Valid
    private PaymentInfoRequest paymentInfo;

    /**
     * 주문 요청 아이템 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemRequest {

        /**
         * 상품 ID
         */
        @NotBlank(message = "상품 ID는 필수입니다")
        private String productId;

        /**
         * 주문 수량
         */
        @NotNull(message = "주문 수량은 필수입니다")
        private Integer quantity;
    }

    /**
     * 배송 주소 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShippingAddressRequest {

        /**
         * 받는 사람 이름
         */
        @NotBlank(message = "받는 사람 이름은 필수입니다")
        private String recipientName;

        /**
         * 받는 사람 연락처
         */
        @NotBlank(message = "받는 사람 연락처는 필수입니다")
        private String recipientPhone;

        /**
         * 우편번호
         */
        @NotBlank(message = "우편번호는 필수입니다")
        private String postalCode;

        /**
         * 기본 주소
         */
        @NotBlank(message = "기본 주소는 필수입니다")
        private String streetAddress;

        /**
         * 상세 주소
         */
        private String detailAddress;

        /**
         * 배송 요청사항
         */
        private String deliveryNote;
    }

    /**
     * 결제 정보
     * 토스 페이먼츠 연동을 위한 정보 포함
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentInfoRequest {

        /**
         * 주문명 (토스 페이먼츠에 표시될 상품명)
         * 예: "강아지 사료 외 2건"
         */
        @NotBlank(message = "주문명은 필수입니다")
        private String orderName;

        /**
         * 구매자 이메일 (토스 페이먼츠 결제창에 미리 입력)
         */
        private String customerEmail;

        /**
         * 구매자 이름 (토스 페이먼츠 결제창에 미리 입력)
         */
        private String customerName;

        /**
         * 성공 시 리디렉션 URL (선택사항 - 기본값 사용 가능)
         */
        private String successUrl;

        /**
         * 실패 시 리디렉션 URL (선택사항 - 기본값 사용 가능)
         */
        private String failUrl;
    }
}