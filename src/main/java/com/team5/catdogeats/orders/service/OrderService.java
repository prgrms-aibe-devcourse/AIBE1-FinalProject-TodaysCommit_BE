package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;

import java.util.NoSuchElementException;

/**
 * 주문 관리 서비스 인터페이스 (보안 개선 버전)
 * JWT 인증을 통한 사용자 식별로 보안성을 강화했습니다.
 */
public interface OrderService {

    /**
     * UserPrincipal을 사용한 안전한 주문 생성 (보안 개선 버전)
     * 처리 과정:
     * 1. UserPrincipal로 사용자 인증 및 조회
     * 2. 구매자 권한 검증
     * 3. 주문 정보 검증 (상품 존재 여부, 재고 확인)
     * 4. 재고 차감 (동시성 제어)
     * 5. 주문 엔티티 및 주문 아이템 생성 (PENDING 상태)
     * 6. 토스 페이먼츠를 위한 정보 생성
     * 7. 주문 응답 DTO 반환
     * 모든 작업이 하나의 트랜잭션에서 수행되어 재고 차감 실패 시 주문도 롤백됩니다.
     * @param userPrincipal JWT에서 추출된 인증된 사용자 정보
     * @param request 주문 생성 요청 정보
     * @return 생성된 주문 정보 (토스 페이먼츠 연동 정보 포함)
     * @throws IllegalArgumentException 상품이 존재하지 않거나 재고가 부족한 경우, 구매자 권한이 없는 경우
     * @throws IllegalStateException 재고 차감 실패 (동시성 충돌) 시
     * @throws NoSuchElementException 사용자를 찾을 수 없는 경우
     */
    OrderCreateResponse createOrderByUserPrincipal(UserPrincipal userPrincipal, OrderCreateRequest request);

    /*
     * 주문을 생성합니다. (1단계: 재고 차감 포함
     * 1. 주문 정보 검증 (상품 존재 여부, 재고 확인)
     * 2. 재고 차감 (동시성 제어)
     * 3. 주문 엔티티 및 주문 아이템 생성 (PENDING 상태)
     * 4. 토스 페이먼츠를 위한 정보 생성
     * 5. 주문 응답 DTO 반환
     * 모든 작업이 하나의 트랜잭션에서 수행되어 재고 차감 실패 시 주문도 롤백됩니다.
     * 주문 완료 후 사용자는 토스 페이먼츠 결제창으로 이동하게 됩니다.
     * 실제 결제 완료는 2단계 "주문 결제 성공(callback)"에서 처리됩니다.
     *
     * @param userId 주문을 생성하는 사용자 ID (문자열 형태의 UUID)
     * @param request 주문 생성 요청 정보
     * @return 생성된 주문 정보 (토스 페이먼츠 연동 정보 포함)
     * @throws IllegalArgumentException 상품이 존재하지 않거나 재고가 부족한 경우
     * @throws IllegalStateException 재고 차감 실패 (동시성 충돌) 시
     * @throws RuntimeException 주문 생성 중 기타 오류가 발생한 경우
     * @deprecated 보안상 위험하므로 {@link #createOrderByUserPrincipal(UserPrincipal, OrderCreateRequest)} 사용을 권장합니다.
     *             이 메서드는 향후 버전에서 제거될 예정입니다.
     */

}