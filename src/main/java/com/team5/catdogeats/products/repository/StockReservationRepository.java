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
 * 재고 예약 Repository
 *
 * 재고 예약 시스템의 데이터 접근 계층입니다.
 * 예약 생성, 조회, 상태 변경, 배치 처리를 위한 다양한 메서드를 제공합니다.
 */
@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    // === 기본 조회 메서드 ===

    /**
     * 주문 ID로 재고 예약 목록 조회
     * 하나의 주문에 속한 모든 상품의 예약 정보를 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 해당 주문의 재고 예약 목록
     */
    List<StockReservation> findByOrderId(UUID orderId);

    /**
     * 상품 ID로 재고 예약 목록 조회
     * 특정 상품에 대한 모든 예약 정보를 조회합니다.
     *
     * @param productId 상품 ID
     * @return 해당 상품의 재고 예약 목록
     */
    List<StockReservation> findByProductId(UUID productId);

    /**
     * 예약 상태별 조회
     * 특정 상태의 예약들을 조회합니다.
     *
     * @param status 예약 상태
     * @return 해당 상태의 예약 목록
     */
    List<StockReservation> findByReservationStatus(ReservationStatus status);

    /**
     * 주문 ID와 상품 ID로 예약 조회
     * 특정 주문의 특정 상품에 대한 예약을 조회합니다.
     *
     * @param orderId 주문 ID
     * @param productId 상품 ID
     * @return 해당 조건의 예약 정보 (Optional)
     */
    Optional<StockReservation> findByOrderIdAndProductId(UUID orderId, UUID productId);

    // === 활성 예약 관련 메서드 ===

    /**
     * 특정 상품의 활성 예약 목록 조회
     * RESERVED 상태인 예약만 조회합니다.
     *
     * @param productId 상품 ID
     * @return 활성 상태의 예약 목록
     */
    @Query("SELECT sr FROM StockReservation sr " +
            "WHERE sr.product.id = :productId AND sr.reservationStatus = 'RESERVED'")
    List<StockReservation> findActiveReservationsByProductId(@Param("productId") UUID productId);

    /**
     * 주문별 활성 예약 개수 조회
     * 주문의 모든 상품이 예약 상태인지 확인할 때 사용합니다.
     *
     * @param orderId 주문 ID
     * @return 활성 예약 개수
     */
    @Query("SELECT COUNT(sr) FROM StockReservation sr " +
            "WHERE sr.order.id = :orderId AND sr.reservationStatus = 'RESERVED'")
    long countActiveReservationsByOrderId(@Param("orderId") UUID orderId);

    // === 재고 수량 계산 메서드 ===

    /**
     * 특정 상품의 총 예약 수량 조회
     * 현재 예약된(RESERVED) 상태의 총 수량을 계산합니다.
     *
     * @param productId 상품 ID
     * @return 총 예약 수량
     */
    @Query("SELECT COALESCE(SUM(sr.reservedQuantity), 0) FROM StockReservation sr " +
            "WHERE sr.product.id = :productId AND sr.reservationStatus = 'RESERVED'")
    Integer getTotalReservedQuantityByProductId(@Param("productId") UUID productId);

    /**
     * 상품의 실제 가용 재고 계산 (실제 재고 - 예약 수량)
     * 새로운 주문 시 예약 가능한 수량을 확인할 때 사용합니다.
     *
     * @param productId 상품 ID
     * @return 가용 재고 수량
     */
    @Query("SELECT (p.stock - COALESCE(SUM(sr.reservedQuantity), 0)) " +
            "FROM Products p " +
            "LEFT JOIN StockReservation sr ON p.id = sr.product.id " +
            "AND sr.reservationStatus = 'RESERVED' " +
            "WHERE p.id = :productId " +
            "GROUP BY p.id, p.stock")
    Integer getAvailableStockByProductId(@Param("productId") UUID productId);

    // === 만료 처리 관련 메서드 ===

    /**
     * 만료된 예약 조회 (배치 처리용)
     * 현재 시간을 기준으로 만료 시간이 지난 RESERVED 상태의 예약을 조회합니다.
     *
     * @param currentTime 현재 시간
     * @return 만료된 예약 목록
     */
    @Query("SELECT sr FROM StockReservation sr " +
            "WHERE sr.reservationStatus = 'RESERVED' " +
            "AND sr.expiredAt <= :currentTime")
    List<StockReservation> findExpiredReservations(@Param("currentTime") ZonedDateTime currentTime);

    /**
     * 특정 시간 이전에 생성된 예약 조회
     * 오래된 예약 정리용 메서드입니다.
     *
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
     * 배치 처리에서 사용하는 벌크 업데이트 메서드입니다.
     *
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
     * 특정 주문의 모든 예약을 특정 상태로 변경
     * 주문 취소 시 사용합니다.
     *
     * @param orderId 주문 ID
     * @param status 변경할 상태
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE StockReservation sr " +
            "SET sr.reservationStatus = :status " +
            "WHERE sr.order.id = :orderId " +
            "AND sr.reservationStatus = 'RESERVED'")
    int bulkUpdateReservationStatus(@Param("orderId") UUID orderId,
                                    @Param("status") ReservationStatus status);

    // === 존재 여부 확인 메서드 ===

    /**
     * 특정 주문에 활성 예약이 있는지 확인
     *
     * @param orderId 주문 ID
     * @return 활성 예약 존재 여부
     */
    boolean existsByOrderIdAndReservationStatus(UUID orderId, ReservationStatus status);

    /**
     * 특정 상품에 활성 예약이 있는지 확인
     *
     * @param productId 상품 ID
     * @return 활성 예약 존재 여부
     */
    boolean existsByProductIdAndReservationStatus(UUID productId, ReservationStatus status);

    // === 통계 및 모니터링 메서드 ===

    /**
     * 상태별 예약 개수 조회
     * 관리자 대시보드에서 사용할 통계 정보입니다.
     *
     * @param status 예약 상태
     * @return 해당 상태의 예약 개수
     */
    long countByReservationStatus(ReservationStatus status);

    /**
     * 특정 기간 내 생성된 예약 개수 조회
     *
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 해당 기간의 예약 개수
     */
    @Query("SELECT COUNT(sr) FROM StockReservation sr " +
            "WHERE sr.reservedAt BETWEEN :startTime AND :endTime")
    long countReservationsBetween(@Param("startTime") ZonedDateTime startTime,
                                  @Param("endTime") ZonedDateTime endTime);
}