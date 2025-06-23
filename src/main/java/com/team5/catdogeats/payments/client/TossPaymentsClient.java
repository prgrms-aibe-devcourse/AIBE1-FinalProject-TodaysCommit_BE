package com.team5.catdogeats.payments.client;

import com.team5.catdogeats.payments.dto.request.TossPaymentConfirmRequest;
import com.team5.catdogeats.payments.dto.response.TossPaymentConfirmResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Toss Payments API 클라이언트
 * Spring Cloud OpenFeign을 사용하여 Toss Payments API와 통신합니다.
 * TossPaymentsClientConfig에서 인증 헤더와 기본 설정을 처리합니다.
 */
@FeignClient(
        name = "toss-payments-client",
        url = "${toss.payments.api.base-url}",
        configuration = TossPaymentsClientConfig.class
)
public interface TossPaymentsClient {

    /**
     * 결제 승인 API 호출
     * Toss Payments의 /v1/payments/confirm 엔드포인트를 호출하여
     * 최종 결제 승인을 요청합니다.
     *
     * @param request 결제 승인 요청 정보
     * @return 결제 승인 응답
     */
    @PostMapping(
            value = "/v1/payments/confirm",
            consumes = "application/json",
            produces = "application/json"
    )
    TossPaymentConfirmResponse confirmPayment(@RequestBody TossPaymentConfirmRequest request);
}