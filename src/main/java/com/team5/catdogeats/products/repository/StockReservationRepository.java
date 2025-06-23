package com.team5.catdogeats.products.repository;

import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 재고 예약 Repository (타입 수정됨)
 * 재고 예약 시스템의 데이터 접근 계층입니다.
 * Products와 Orders 엔티티의 ID 타입이 String으로 변경됨에 따라 관련 메서드들을 수정하였습니다.
 */
@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    // === 기본 조회 메서드 (타입 수정) ===

    /**
     * 주문 ID로 재고 예약 목록 조회 (타입 수정: UUID → String)
     * @param orderId 주문 ID (String 타입)
     * @return 해당 주문의 재고 예약 목록
     */
    List<StockReservation> findByOrderId(String orderId);

    /**
     * 상품 ID로 재고 예약 목록 조회 (타입 수정: UUID → String)
     * @param productId 상품 ID (String 타입)
     * @return 해당 상품의 재고 예약 목록
     */
    List<StockReservation> findByProductId(String productId);

    /**
     * 예약 상태별 조회
     * @param status 예약 상태
     * @return 해당 상태의 예약 목록
     */
    List<StockReservation> findByReservationStatus(ReservationStatus status);

    /**
     * 주문 ID와 상품 ID로 예약 조회 (타입 수정)
     * @param orderId 주문 ID (String 타입)
     * @param productId 상품 ID (String 타입)
     * @return 해당 조건의 예약 정보 (Optional)
     */
    Optional<StockReservation> findByOrderIdAndProductId(String orderId, String productId);

    // === 활성 예약 관련 메서드 (타입 수정) ===

    /**
     * 특정 상품의 활성 예약 목록 조회 (타입 수정: UUID → String)
     * @param productId 상품 ID (String 타입)
     * @return 활성 상태의 예약 목록
     */
    @Query("SELECT sr FROM StockReservation sr " +
            "WHERE sr.product.id = :productId AND sr.reservationStatus = 'RESERVED'")
    List<StockReservation> findActiveReservationsByProductId(@Param("productId") String productId);

    /**
     * 주문별 활성 예약 개수 조회 (타입 수정: UUID → String)
     * @param orderId 주문 ID (String 타입)
     * @return 활성 예약 개수
     */
    @Query("SELECT COUNT(sr) FROM StockReservation sr " +
            "WHERE sr.order.id = :orderId AND sr.reservationStatus = 'RESERVED'")
    long countActiveReservationsByOrderId(@Param("orderId") String orderId);

    // === 재고 수량 계산 메서드 (타입 수정) ===

    /**
     * 특정 상품의 총 예약 수량 조회 (타입 수정: UUID → String)
     * @param productId 상품 ID (String 타입)
     * @return 총 예약 수량
     */
    @Query("SELECT COALESCE(SUM(sr.reservedQuantity), 0) FROM StockReservation sr " +
            "WHERE sr.product.id = :productId AND sr.reservationStatus = 'RESERVED'")
    Integer getTotalReservedQuantityByProductId(@Param("productId") String productId);

    /**
     * 상품의 실제 가용 재고 계산 (타입 수정: UUID → String)
     * @param productId 상품 ID (String 타입)
     * @return 가용 재고 수량
     */
    @Query("SELECT (p.stock - COALESCE(SUM(sr.reservedQuantity), 0)) " +
            "FROM Products p " +
            "LEFT JOIN StockReservation sr ON p.id = sr.product.id " +
            "AND sr.reservationStatus = 'RESERVED' " +
            "WHERE p.id = :productId " +
            "GROUP BY p.id, p.stock")
    Integer getAvailableStockByProductId(@Param("productId") String productId);

    // === 만료 처리 관련 메서드 ===

    /**
     * 만료된 예약 조회 (배치 처리용)
     * @param currentTime 현재 시간
     * @return 만료된 예약 목록
     */
    @Query("SELECT sr FROM StockReservation sr " +
            "WHERE sr.reservationStatus = 'RESERVED' " +
            "AND sr.expiredAt <= :currentTime")
    List<StockReservation> findExpiredReservations(@Param("currentTime") ZonedDateTime currentTime);

    /**
     * 특정 시간 이전에 생성된 예약 조회
     * @param beforeTime 기준 시간
     * @param statuses 조회할 상태 목록
     * @return 오래된 예약 목록
     */
    @Query("SELECT sr FROM StockReservation sr " +
            "WHERE sr.reservedAt <= :beforeTime " +
            "AND sr.reservationStatus IN :statuses")
    List<StockReservation> findOldReservations(@Param("beforeTime") ZonedDateTime beforeTime,
                                               @Param("statuses") List<ReservationStatus> statuses);

    // === 벌크 업데이트 메서드 ===

    /**
     * 만료된 예약들을 일괄 EXPIRED 상태로 변경
     * @param currentTime 현재 시간
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE StockReservation sr " +
            "SET sr.reservationStatus = 'EXPIRED' " +
            "WHERE sr.reservationStatus = 'RESERVED' " +
            "AND sr.expiredAt <= :currentTime")
    int bulkExpireReservations(@Param("currentTime") ZonedDateTime currentTime);

    /**
     * 특정 주문의 모든 예약을 특정 상태로 변경 (타입 수정: UUID → String)
     * @param orderId 주문 ID (String 타입)
     * @param status 변경할 상태
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE StockReservation sr " +
            "SET sr.reservationStatus = :status " +
            "WHERE sr.order.id = :orderId " +
            "AND sr.reservationStatus = 'RESERVED'")
    int bulkUpdateReservationStatus(@Param("orderId") String orderId,
                                    @Param("status") ReservationStatus status);

    // === 존재 여부 확인 메서드 (타입 수정) ===

    /**
     * 특정 주문에 활성 예약이 있는지 확인 (타입 수정: UUID → String)
     * @param orderId 주문 ID (String 타입)
     * @param status 예약 상태
     * @return 활성 예약 존재 여부
     */
    boolean existsByOrderIdAndReservationStatus(String orderId, ReservationStatus status);

    /**
     * 특정 상품에 활성 예약이 있는지 확인 (타입 수정: UUID → String)
     * @param productId 상품 ID (String 타입)
     * @param status 예약 상태
     * @return 활성 예약 존재 여부
     */
    boolean existsByProductIdAndReservationStatus(String productId, ReservationStatus status);

    // === 통계 및 모니터링 메서드 ===

    /**
     * 상태별 예약 개수 조회
     * @param status 예약 상태
     * @return 해당 상태의 예약 개수
     */
    long countByReservationStatus(ReservationStatus status);

    /**
     * 특정 기간 내 생성된 예약 개수 조회
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 해당 기간의 예약 개수
     */
    @Query("SELECT COUNT(sr) FROM StockReservation sr " +
            "WHERE sr.reservedAt BETWEEN :startTime AND :endTime")
    long countReservationsBetween(@Param("startTime") ZonedDateTime startTime,
                                  @Param("endTime") ZonedDateTime endTime);
}