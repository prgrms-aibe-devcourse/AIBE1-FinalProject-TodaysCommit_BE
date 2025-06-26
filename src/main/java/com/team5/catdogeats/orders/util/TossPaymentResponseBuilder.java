package com.team5.catdogeats.orders.util;

import com.team5.catdogeats.global.config.TossPaymentsConfig;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 토스 페이먼츠 응답 생성 유틸리티 클래스
 * 토스 페이먼츠 API 응답 포맷 생성 책임을 담당합니다.
 * 주요 기능:
 * - 토스 페이먼츠 결제창 연동을 위한 응답 객체 생성
 * - 결제 관련 설정값 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentResponseBuilder {

    private final TossPaymentsConfig.TossPaymentsProperties tossPaymentsProperties;

    /**
     * 토스 페이먼츠 응답 객체를 생성합니다.
     *
     * @param order 주문 정보
     * @param paymentInfo 결제 정보 요청 데이터
     * @param orderName 주문명 (외부에서 생성된 주문명)
     * @return 토스 페이먼츠 연동을 위한 응답 객체
     */
    public OrderCreateResponse buildTossPaymentResponse(Orders order, OrderCreateRequest.PaymentInfoRequest paymentInfo, String orderName) {
        log.debug("토스 페이먼츠 응답 생성 시작: orderId={}, orderNumber={}",
                order.getId(), order.getOrderNumber());

        // TossPaymentInfo 생성
        OrderCreateResponse.TossPaymentInfo tossPaymentInfo = OrderCreateResponse.TossPaymentInfo.builder()
                .tossOrderId(order.getId())
                .orderName(orderName)
                .amount(order.getTotalPrice())
                .customerName(paymentInfo.getCustomerName())
                .customerEmail(paymentInfo.getCustomerEmail())
                .successUrl(paymentInfo.getSuccessUrl() != null ?
                        paymentInfo.getSuccessUrl() : tossPaymentsProperties.getSuccessUrl())
                .failUrl(paymentInfo.getFailUrl() != null ?
                        paymentInfo.getFailUrl() : tossPaymentsProperties.getFailUrl())
                .clientKey(tossPaymentsProperties.getClientKey())
                .build();

        OrderCreateResponse response = OrderCreateResponse.builder()
                .orderNumber(order.getOrderNumber())
                .totalPrice(order.getTotalPrice())
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus())
                .createdAt(order.getCreatedAt())
                .tossPaymentInfo(tossPaymentInfo)
                .build();

        log.debug("토스 페이먼츠 응답 생성 완료: orderName={}, amount={}",
                orderName, tossPaymentInfo.getAmount());

        return response;
    }
}