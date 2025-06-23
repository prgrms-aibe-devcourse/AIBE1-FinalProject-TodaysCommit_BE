package com.team5.catdogeats.payments.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Toss Payments 결제 승인 요청 DTO
 *
 * Toss Payments의 /v1/payments/confirm API에 전송하는 요청 객체입니다.
 * 결제 승인을 위한 필수 정보들을 포함합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TossPaymentConfirmRequest {

    /**
     * 결제 키
     *
     * 결제 승인에 필요한 고유 키입니다.
     * 클라이언트에서 결제 완료 후 받은 paymentKey를 그대로 전달합니다.
     * 최대 200자이며, 10분 내에 사용해야 합니다.
     */
    @JsonProperty("paymentKey")
    private String paymentKey;

    /**
     * 주문 ID
     *
     * 가맹점에서 생성한 고유 주문 번호입니다.
     * 주문 생성 시 생성한 UUID 값을 문자열로 전달합니다.
     */
    @JsonProperty("orderId")
    private String orderId;

    /**
     * 결제 금액
     *
     * 실제로 결제할 금액입니다.
     * 반드시 서버에서 검증된 정확한 금액이어야 합니다.
     * 클라이언트에서 전달받은 값이 아닌, DB에 저장된 주문 금액을 사용해야 합니다.
     */
    @JsonProperty("amount")
    private Long amount;
}