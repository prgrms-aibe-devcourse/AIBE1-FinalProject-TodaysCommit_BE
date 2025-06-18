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

import java.util.NoSuchElementException;

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
     * <p>
     * API: POST /v1/buyers/orders
     * <p>
     * 구매자가 장바구니의 상품들을 주문하고, 토스 페이먼츠 결제를 위한
     * 정보를 생성하여 반환합니다.
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
            log.info("주문 생성 성공: orderId={}, orderNumber={}", response.getOrderId(), response.getOrderNumber());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

            // 특정 예외를 잡아서 명확한 HTTP 상태 코드로 응답합니다.
        } catch (NoSuchElementException e) {
            log.warn("주문 생성 실패 - 리소스를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("주문 생성 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
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
     * <p>
     * /*
     * Spring Security Context에서 현재 인증된 사용자의 ID를 가져옵니다.
     *
     * @return 사용자 ID (UUID)
     * @throws IllegalStateException 인증 정보가 없거나 유효하지 않은 경우
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("인증되지 않은 사용자입니다. API 요청에 유효한 인증 정보가 필요합니다.");
        }

        // Principal에서 사용자 이름을 가져옴 (테스트에서는 여기에 UUID를 설정)
        String userId = authentication.getName();

        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalStateException("유효하지 않은 사용자 ID입니다.");
        }

        return userId;
    }
}