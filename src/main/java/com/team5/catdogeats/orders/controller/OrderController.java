package com.team5.catdogeats.orders.controller;

import com.team5.catdogeats.orders.domain.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.domain.dto.response.OrderCreateResponse;
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

/**
 * 구매자 주문 관리 컨트롤러
 *
 * 구매자의 주문 생성, 조회, 관리 등의 API를 제공합니다.
 * API 명세서의 "주문/배송 관리" 영역에 해당합니다.
 */
@Slf4j
@RestController
@RequestMapping("/v1/buyers/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성 (구매자)
     *
     * API: POST /v1/buyers/orders
     *
     * 구매자가 장바구니의 상품들을 주문하고, 토스 페이먼츠 결제를 위한
     * 정보를 생성하여 반환합니다.
     *
     * @param request 주문 생성 요청 정보
     * @return 생성된 주문 정보 (토스 페이먼츠 연동 정보 포함)
     */
    @PostMapping
    public ResponseEntity<OrderCreateResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request) {

        try {
            // 현재 인증된 사용자 ID 추출
            String userId = getCurrentUserId();

            log.info("주문 생성 요청: userId={}, 상품 개수={}",
                    userId, request.getOrderItems().size());

            // 주문 생성 서비스 호출
            OrderCreateResponse response = orderService.createOrder(userId, request);

            log.info("주문 생성 성공: orderId={}, orderNumber={}",
                    response.getOrderId(), response.getOrderNumber());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("주문 생성 실패 - 잘못된 요청: {}", e.getMessage());
            throw e; // 글로벌 예외 핸들러에서 처리

        } catch (Exception e) {
            log.error("주문 생성 중 오류 발생", e);
            throw new RuntimeException("주문 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 현재 인증된 사용자의 ID를 추출합니다.
     *
     * Spring Security의 Authentication에서 사용자 정보를 가져옵니다.
     * 실제 프로젝트에서는 JWT 토큰이나 세션에서 사용자 ID를 추출하는
     * 로직으로 대체될 수 있습니다.
     *
     * @return 현재 사용자 ID
     * @throws IllegalStateException 인증되지 않은 사용자인 경우
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        // TODO: 실제 사용자 ID 추출 로직 구현
        // 현재는 예시로 principal에서 가져오는 것으로 가정
        // 실제로는 JWT의 subject나 UserDetails의 username 등을 사용
        String userId = authentication.getName();

        if (userId == null || userId.isEmpty()) {
            throw new IllegalStateException("사용자 ID를 추출할 수 없습니다.");
        }

        return userId;
    }

    // TODO: 향후 추가될 주문 관련 엔드포인트들
    /*
    @GetMapping
    public ResponseEntity<PageResponse<OrderListResponse>> getOrderList(
            @RequestParam(defaultValue = "0") int page) {
        // 주문 목록 조회 로직
    }

    @GetMapping("/{order-number}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(
            @PathVariable("order-number") Long orderNumber) {
        // 주문 상세 조회 로직
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteOrder(
            @RequestParam("orderNumber") Long orderNumber) {
        // 주문 내역 삭제 로직
    }
    */
}