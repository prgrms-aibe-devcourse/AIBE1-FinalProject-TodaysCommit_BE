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
import org.springframework.web.bind.annotation.*;

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
     * 주문 생성 (구매자)
     * @param request 주문 생성 요청 정보
     * @param userId (테스트용) Swagger UI에서 사용자 ID를 직접 입력받기 위한 파라미터
     * @return 생성된 주문 정보 (토스 페이먼츠 연동 정보 포함)
     */
    @PostMapping
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            @RequestParam("userId") String userId) { // ⭐️ @RequestParam은 이 위치에 있어야 합니다.

        try {
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
}