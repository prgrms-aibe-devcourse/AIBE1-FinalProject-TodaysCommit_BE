package com.team5.catdogeats.orders.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.NoSuchElementException;

/**
 * 구매자 주문 관리 컨트롤러 (보안 개선 버전)
 *
 * JWT 인증을 통한 사용자 식별로 보안성을 강화했습니다.
 * ApiResponse 컨벤션을 적용하여 일관된 응답 형식을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/v1/buyers/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성 (구매자) - 보안 개선 버전
     *
     * @param userPrincipal JWT에서 추출된 인증된 사용자 정보
     * @param request 주문 생성 요청 정보
     * @return 생성된 주문 정보 (토스 페이먼츠 연동 정보 포함)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderCreateResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody OrderCreateRequest request) {

        try {
            // UserPrincipal에서 사용자 정보 추출 (provider + providerId로 사용자 식별)
            String userIdentifier = userPrincipal.provider() + ":" + userPrincipal.providerId();

            log.info("주문 생성 요청: userProvider={}, providerId={}, 상품 개수={}",
                    userPrincipal.provider(), userPrincipal.providerId(), request.getOrderItems().size());

            OrderCreateResponse response = orderService.createOrderByUserPrincipal(userPrincipal, request);

            log.info("주문 생성 성공 (재고 차감 완료): orderId={}, orderNumber={}",
                    response.getOrderId(), response.getOrderNumber());

            // 201 Created와 Location 헤더 설정
            return ResponseEntity
                    .created(URI.create("/v1/buyers/orders/" + response.getOrderNumber()))
                    .body(ApiResponse.success(ResponseCode.CREATED, response));

        } catch (NoSuchElementException e) {
            log.warn("주문 생성 실패 - 리소스를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("주문 생성 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("주문 생성 실패 - 재고 차감 실패: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));

        } catch (Exception e) {
            log.error("주문 생성 중 내부 오류 발생", e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "주문 생성 중 서버 오류가 발생했습니다."));
        }
    }
}