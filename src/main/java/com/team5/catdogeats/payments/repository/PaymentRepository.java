package com.team5.catdogeats.payments.repository;

import com.team5.catdogeats.payments.domain.Payments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Payments 엔티티 리포지토리 (타입 수정됨)
 * 결제 정보 관리를 위한 데이터 접근 계층입니다.
 * OrderEventListener에서 결제 정보 생성 시 사용됩니다.
 * Orders 엔티티의 ID 타입이 String으로 수정됨에 따라 관련 메서드들도 수정되었습니다.
 */
public interface PaymentRepository extends JpaRepository<Payments, String> {

    /**
     * 주문 ID로 결제 정보 조회 (타입 수정: UUID → String)
     * @param ordersId 주문 ID (String 타입)
     * @return 결제 정보 (Optional)
     */
    Optional<Payments> findByOrdersId(String ordersId);

    /*
     * 토스 페이먼츠 키로 결제 정보 조회
     * @param tossPaymentKey 토스 페이먼츠 키
     * @return 결제 정보 (Optional)
     */
    //Optional<Payments> findByTossPaymentKey(String tossPaymentKey);

    /*
     * 구매자 ID로 결제 내역 조회
     * @param buyersUserId 구매자 ID (String 타입)
     * @return 결제 내역 (Optional)
     */
    //Optional<Payments> findByBuyersUserId(String buyersUserId);

    /*
     * 결제 상태별 결제 정보 존재 여부 확인 (타입 수정: UUID → String)
     * @param ordersId 주문 ID (String 타입)
     * @param status 결제 상태
     * @return 존재 여부
     */
    //boolean existsByOrdersIdAndStatus(String ordersId, PaymentStatus status);
}