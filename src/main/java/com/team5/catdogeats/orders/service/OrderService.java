package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.orders.domain.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.domain.dto.response.OrderCreateResponse;

/**
 * 주문 관리 서비스 인터페이스
 *
 * 주문 생성, 조회, 관리 등의 비즈니스 로직을 정의합니다.
 */
public interface OrderService {

    /**
     * 주문을 생성합니다.
     *
     * 1. 주문 정보 검증 (상품 존재 여부, 재고 확인 등)
     * 2. 주문 엔티티 및 주문 아이템 생성
     * 3. 토스 페이먼츠를 위한 정보 생성
     * 4. 주문 응답 DTO 반환
     *
     * @param userId 주문을 생성하는 사용자 ID
     * @param request 주문 생성 요청 정보
     * @return 생성된 주문 정보 (토스 페이먼츠 연동 정보 포함)
     * @throws IllegalArgumentException 상품이 존재하지 않거나 재고가 부족한 경우
     * @throws RuntimeException 주문 생성 중 오류가 발생한 경우
     */
    OrderCreateResponse createOrder(String userId, OrderCreateRequest request);

    // TODO: 향후 추가될 메서드들
    // OrderDetailResponse getOrderDetail(String userId, String orderNumber);
    // PageResponse<OrderListResponse> getOrderList(String userId, int page);
    // void deleteOrder(String userId, String orderNumber);
}