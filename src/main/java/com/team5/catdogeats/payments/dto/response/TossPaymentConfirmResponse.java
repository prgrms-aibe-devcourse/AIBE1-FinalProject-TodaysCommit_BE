package com.team5.catdogeats.payments.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Toss Payments 결제 승인 응답 DTO
 *
 * Toss Payments의 /v1/payments/confirm API로부터 받는 응답 객체입니다.
 * 결제 승인 완료 후 필요한 정보들을 포함합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TossPaymentConfirmResponse {

    /**
     * 결제 키
     * Toss Payments에서 발급한 결제의 고유 식별자
     */
    @JsonProperty("paymentKey")
    private String paymentKey;

    /**
     * 결제 타입
     * 예: "NORMAL" (일반 결제)
     */
    @JsonProperty("type")
    private String type;

    /**
     * 주문 ID
     * 가맹점에서 생성한 주문 번호
     */
    @JsonProperty("orderId")
    private String orderId;

    /**
     * 주문명
     * 사용자에게 보여지는 주문 상품명
     */
    @JsonProperty("orderName")
    private String orderName;

    /**
     * 결제 상태
     * "DONE": 결제 완료
     * "CANCELED": 결제 취소
     * "FAILED": 결제 실패
     */
    @JsonProperty("status")
    private String status;

    /**
     * 결제 승인 시간
     * ISO 8601 형식의 시간 정보
     */
    @JsonProperty("approvedAt")
    private ZonedDateTime approvedAt;

    /**
     * 총 결제 금액
     */
    @JsonProperty("totalAmount")
    private Long totalAmount;

    /**
     * 잔액 (할인 전 금액)
     */
    @JsonProperty("balanceAmount")
    private Long balanceAmount;

    /**
     * 공급가액
     */
    @JsonProperty("suppliedAmount")
    private Long suppliedAmount;

    /**
     * 부가세
     */
    @JsonProperty("vat")
    private Long vat;

    /**
     * 결제 수단 정보
     */
    @JsonProperty("method")
    private String method;

    /**
     * 결제 수단별 상세 정보
     * 카드 결제 시 카드 정보, 계좌이체 시 은행 정보 등
     */
    @JsonProperty("card")
    private CardInfo card;

    /**
     * 카드 결제 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardInfo {

        /**
         * 카드 회사
         */
        @JsonProperty("company")
        private String company;

        /**
         * 카드 번호 (마스킹됨)
         */
        @JsonProperty("number")
        private String number;

        /**
         * 카드 타입
         * "신용", "체크", "기프트" 등
         */
        @JsonProperty("cardType")
        private String cardType;

        /**
         * 소유자 타입
         * "개인", "법인"
         */
        @JsonProperty("ownerType")
        private String ownerType;

        /**
         * 승인 번호
         */
        @JsonProperty("approveNo")
        private String approveNo;

        /**
         * 할부 개월 수
         * 0이면 일시불
         */
        @JsonProperty("installmentPlanMonths")
        private Integer installmentPlanMonths;
    }
}