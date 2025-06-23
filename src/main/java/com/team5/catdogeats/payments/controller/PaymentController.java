package com.team5.catdogeats.payments.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.payments.dto.response.PaymentConfirmResponse;
import com.team5.catdogeats.payments.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 결제 처리 컨트롤러
 *
 * Toss Payments 콜백 및 결제 관련 API를 처리합니다.
 * 주요 기능:
 * - 결제 성공 콜백 처리
 * - 결제 실패 콜백 처리
 * - 결제 정보 조회
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/buyers/payments")
@Tag(name = "Payment", description = "결제 관련 API")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 성공 콜백 처리
     *
     * Toss Payments에서 결제 성공 시 리디렉션되는 엔드포인트입니다.
     * URL 쿼리 파라미터로 paymentKey, orderId, amount를 전달받아
     * 최종 결제 승인을 진행합니다.
     *
     * @param paymentKey Toss Payments에서 발급한 결제 키
     * @param orderId 주문 ID (UUID 형태)
     * @param amount 결제 금액
     * @return 결제 승인 결과
     */
    @GetMapping("/success")
    @Operation(
            summary = "결제 성공 콜백",
            description = "Toss Payments 결제 성공 시 호출되는 콜백 엔드포인트"
    )
    public ResponseEntity<ApiResponse<PaymentConfirmResponse>> handlePaymentSuccess(
            @Parameter(description = "Toss Payments 결제 키", required = true)
            @RequestParam("paymentKey") String paymentKey,

            @Parameter(description = "주문 ID", required = true)
            @RequestParam("orderId") String orderId,

            @Parameter(description = "결제 금액", required = true)
            @RequestParam("amount") Long amount) {

        log.info("결제 성공 콜백 수신: paymentKey={}, orderId={}, amount={}",
                paymentKey, orderId, amount);

        try {
            // 결제 승인 처리
            PaymentConfirmResponse response = paymentService.confirmPayment(paymentKey, orderId, amount);

            log.info("결제 승인 완료: orderId={}, paymentId={}", orderId, response.getPaymentId());

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SUCCESS, response)
            );

        } catch (IllegalArgumentException e) {
            log.error("결제 승인 실패 (잘못된 요청): {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(ResponseCode.BAD_REQUEST, e.getMessage())
            );

        } catch (Exception e) {
            log.error("결제 승인 중 오류 발생: orderId={}, error={}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "결제 처리 중 오류가 발생했습니다.")
            );
        }
    }

    /**
     * 결제 실패 콜백 처리
     *
     * Toss Payments에서 결제 실패 시 리디렉션되는 엔드포인트입니다.
     *
     * @param code 실패 코드
     * @param message 실패 메시지
     * @param orderId 주문 ID
     * @return 결제 실패 처리 결과
     */
    @GetMapping("/fail")
    @Operation(
            summary = "결제 실패 콜백",
            description = "Toss Payments 결제 실패 시 호출되는 콜백 엔드포인트"
    )
    public ResponseEntity<ApiResponse<String>> handlePaymentFailure(
            @Parameter(description = "실패 코드")
            @RequestParam(value = "code", required = false) String code,

            @Parameter(description = "실패 메시지")
            @RequestParam(value = "message", required = false) String message,

            @Parameter(description = "주문 ID")
            @RequestParam(value = "orderId", required = false) String orderId) {

        log.warn("결제 실패 콜백 수신: code={}, message={}, orderId={}", code, message, orderId);

        try {
            // 결제 실패 처리
            paymentService.handlePaymentFailure(orderId, code, message);

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SUCCESS, "결제 실패 처리가 완료되었습니다.")
            );

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류 발생: orderId={}, error={}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "결제 실패 처리 중 오류가 발생했습니다.")
            );
        }
    }
}