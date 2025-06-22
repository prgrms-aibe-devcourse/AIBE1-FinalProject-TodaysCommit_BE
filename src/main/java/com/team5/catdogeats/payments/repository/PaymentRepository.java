package com.team5.catdogeats.payments.repository;

import com.team5.catdogeats.payments.domain.Payments;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Payments 엔티티 리포지토리
 *
 * 결제 정보 관리를 위한 데이터 접근 계층입니다.
 * OrderEventListener에서 결제 정보 생성 시 사용됩니다.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payments, UUID> {

    /**
     * 주문 ID로 결제 정보 조회
     *
     * @param ordersId 주문 ID
     * @return 결제 정보 (Optional)
     */
    Optional<Payments> findByOrdersId(UUID ordersId);

    /**
     * 토스 페이먼츠 키로 결제 정보 조회
     *
     * @param tossPaymentKey 토스 페이먼츠 키
     * @return 결제 정보 (Optional)
     */
    Optional<Payments> findByTossPaymentKey(String tossPaymentKey);

    /**
     * 구매자 ID로 결제 내역 조회 (수정된 부분)
     *
     * @param buyersUserId 구매자 ID (엔티티 필드명에 맞게 파라미터명도 수정)
     * @return 결제 내역 (Optional)
     */
    Optional<Payments> findByBuyersUserId(UUID buyersUserId);

    /**
     * 결제 상태별 결제 정보 존재 여부 확인
     *
     * @param ordersId 주문 ID
     * @param status 결제 상태
     * @return 존재 여부
     */
    boolean existsByOrdersIdAndStatus(UUID ordersId, PaymentStatus status);
}