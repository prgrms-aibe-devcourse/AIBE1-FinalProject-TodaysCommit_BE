package com.team5.catdogeats.payments.dto.response;

import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * 결제 승인 응답 DTO
 * 클라이언트에게 반환하는 결제 승인 완료 정보입니다.
 * Toss Payments API 응답을 가공하여 필요한 정보만 포함합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentConfirmResponse {

    /**
     * 결제 ID
     * 시스템 내부의 결제 고유 식별자 (UUID)
     */
    private String paymentId;

    /**
     * 주문 ID
     * 주문의 고유 식별자 (UUID)
     */
    private String orderId;

    /**
     * 주문 번호
     * 사용자에게 표시되는 주문 번호
     */
    private Long orderNumber;

    /**
     * 결제 금액
     * 실제 결제된 금액
     */
    private Long amount;

    /**
     * 결제 상태
     * SUCCESS, FAILED, CANCELLED 등
     */
    private PaymentStatus status;

    /**
     * 결제 완료 시간
     */
    private ZonedDateTime paidAt;

    /**
     * Toss Payments 결제 키
     * 추후 환불 등에 사용할 수 있는 식별자
     */
    private String tossPaymentKey;

    /**
     * 성공 메시지
     */
    private String message;

    /**
     * 성공 응답 생성을 위한 정적 팩토리 메서드
     */
    public static PaymentConfirmResponse success(String paymentId, String orderId, Long orderNumber,
                                                 Long amount, String tossPaymentKey) {
        return PaymentConfirmResponse.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .orderNumber(orderNumber)
                .amount(amount)
                .status(PaymentStatus.SUCCESS)
                .paidAt(ZonedDateTime.now())
                .tossPaymentKey(tossPaymentKey)
                .message("결제가 성공적으로 완료되었습니다.")
                .build();
    }
}