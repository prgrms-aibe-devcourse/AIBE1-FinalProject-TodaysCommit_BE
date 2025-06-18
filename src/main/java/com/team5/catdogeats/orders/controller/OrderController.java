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

        // 인증된 사용자의 principal에서 사용자 ID 추출
        Object principal = authentication.getPrincipal();

        if (principal == null) {
            throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");
        }

        // 실제 OAuth2 로그인의 경우, CustomOAuth2User나 UserPrincipal에서 ID 추출
        // 현재는 임시로 principal의 toString()을 사용하거나 테스트용 ID 반환

        // TODO: 실제 OAuth2 사용자 정보 구조에 맞게 수정 필요
        if (principal instanceof String) {
            return (String) principal;
        }

        // OAuth2User 인터페이스를 구현한 사용자 정보에서 ID 추출하는 경우
        if (principal.toString().equals("anonymousUser")) {
            // 개발/테스트 환경에서 임시 사용자 ID 반환
            log.warn("익명 사용자 접근 - 테스트용 사용자 ID 반환");
            return "test-user-id-123"; // 개발용 임시 ID
        }

        // 사용자 정보에서 실제 사용자 ID 추출
        // 실제 구현에서는 CustomOAuth2User.getId() 등을 사용
        String userId = extractUserIdFromPrincipal(principal);

        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalStateException("유효하지 않은 사용자 ID입니다.");
        }

        return userId;
    }

    /**
     * Principal 객체에서 사용자 ID를 추출합니다.
     *
     * 실제 OAuth2 구현에 따라 수정이 필요한 메서드입니다.
     *
     * @param principal 인증된 사용자 정보
     * @return 사용자 ID
     */
    private String extractUserIdFromPrincipal(Object principal) {
        // TODO: 실제 OAuth2 구현에 맞게 수정

        // 예시 1: CustomOAuth2User를 사용하는 경우
        // if (principal instanceof CustomOAuth2User customUser) {
        //     return customUser.getId();
        // }

        // 예시 2: OAuth2User를 직접 사용하는 경우
        // if (principal instanceof OAuth2User oAuth2User) {
        //     return (String) oAuth2User.getAttribute("id");
        // }

        // 예시 3: JWT 토큰에서 추출하는 경우
        // if (principal instanceof JwtUser jwtUser) {
        //     return jwtUser.getUserId();
        // }

        // 현재는 principal의 문자열 표현을 반환 (개발/테스트용)
        String principalStr = principal.toString();

        // 테스트용: 특정 패턴의 사용자 ID 추출
        if (principalStr.contains("userId=")) {
            // "userId=uuid-123" 형태에서 ID 추출
            return principalStr.split("userId=")[1].split(",")[0];
        }

        // 기본값: principal 문자열 자체를 ID로 사용 (개발용)
        return principalStr;
    }

    // TODO: 향후 추가될 주문 관련 API 메서드들
    // @GetMapping
    // public ResponseEntity<PageResponse<OrderListResponse>> getOrderList(...)

    // @GetMapping("/{order-number}")
    // public ResponseEntity<OrderDetailResponse> getOrderDetail(...)

    // @DeleteMapping
    // public ResponseEntity<Void> deleteOrder(...)
}