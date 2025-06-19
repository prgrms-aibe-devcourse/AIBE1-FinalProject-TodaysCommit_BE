package com.team5.catdogeats.orders.controller;

import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

/**
 * 구매자 주문 관리 컨트롤러 (1단계: 재고 차감 포함)
 *
 * 구매자의 주문 생성 API를 제공합니다.
 * API 명세서의 "주문/배송 관리" 영역 중 "주문(구매자)" 기능에 해당합니다.
 */
@Slf4j
@RestController
@RequestMapping("/v1/buyers/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성 (구매자) - 1단계: 재고 차감 포함
     * <p>
     * API: POST /v1/buyers/orders
     * <p>
     * 처리 과정:
     * 1. 상품 검증 및 재고 확인
     * 2. 재고 차감 (동시성 제어)
     * 3. 주문 생성 (PENDING 상태)
     * 4. 토스 페이먼츠 정보 응답
     *
     * 주문 완료 후 사용자는 응답받은 토스 정보로 결제창에 접속합니다.
     * 실제 결제 완료는 2단계 "주문 결제 성공(callback)"에서 처리됩니다.
     *
     * @param request 주문 생성 요청 정보
     * @return 생성된 주문 정보 (토스 페이먼츠 연동 정보 포함)
     */
    @PostMapping
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody OrderCreateRequest request) {

        try {
            String userId = getCurrentUserId();
            log.info("주문 생성 요청: userId={}, 상품 개수={}", userId, request.getOrderItems().size());

            OrderCreateResponse response = orderService.createOrder(userId, request);
            log.info("주문 생성 성공 (재고 차감 완료): orderId={}, orderNumber={}",
                    response.getOrderId(), response.getOrderNumber());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (NoSuchElementException e) {
            log.warn("주문 생성 실패 - 리소스를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("주문 생성 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("주문 생성 실패 - 재고 차감 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("주문 생성 중 내부 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("주문 생성 중 서버 오류가 발생했습니다.");
        }
    }

    /**
     * 현재 인증된 사용자의 ID를 추출합니다.
     * <p>
     * Spring Security의 Authentication에서 사용자 정보를 가져옵니다.
     * 실제 프로젝트에서는 JWT 토큰이나 세션에서 사용자 ID를 추출하는
     * 로직으로 대체될 수 있습니다.
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        // 실제 환경에서는 JWT에서 사용자 ID 추출 또는 UserDetails에서 가져오기
        return authentication.getName(); // 임시로 name 사용
    }
}