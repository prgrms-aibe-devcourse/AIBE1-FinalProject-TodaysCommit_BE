package com.team5.catdogeats.payments.service;

import com.team5.catdogeats.payments.dto.response.PaymentConfirmResponse;

// 결제 처리 서비스 인터페이스
// Toss Payments API와 연동하여 결제 승인, 검증 및 상태 관리를 담당합니다.
// 프로젝트 컨벤션에 따라 인터페이스 + 구현체 패턴을 적용했습니다.
public interface PaymentService {

    // 결제 승인 처리
    PaymentConfirmResponse confirmPayment(String paymentKey, String orderId, Long amount);

    // 결제 실패 처리
    void handlePaymentFailure(String orderId, String code, String message);
}