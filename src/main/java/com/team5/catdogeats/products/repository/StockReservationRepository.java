package com.team5.catdogeats.products.repository;

import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

// 재고 예약 Repository (타입 수정됨)
// 재고 예약 시스템의 데이터 접근 계층입니다.
// Products와 Orders 엔티티의 ID 타입이 String으로 변경됨에 따라 관련 메서드들을 수정하였습니다.
public interface StockReservationRepository extends JpaRepository<StockReservation, String> {

    // === 기본 조회 메서드 (타입 수정) ===

    // 주문 ID로 재고 예약 목록 조회 (타입 수정: UUID → String)
    List<StockReservation> findByOrderId(String orderId);

    // 상품 ID로 재고 예약 목록 조회 (타입 수정: UUID → String)
    List<StockReservation> findByProductId(String productId);

    // 예약 상태별 조회
    List<StockReservation> findByReservationStatus(ReservationStatus status);

    // 주문 ID와 상품 ID로 예약 조회 (타입 수정)
    Optional<StockReservation> findByOrderIdAndProductId(String orderId, String productId);

    // === 활성 예약 관련 메서드 (타입 수정) ===

    // 특정 상품의 활성 예약 목록 조회 (타입 수정: UUID → String)
    @Query("SELECT sr FROM StockReservation sr " +
            "WHERE sr.product.id = :productId AND sr.reservationStatus = 'RESERVED'")
    List<StockReservation> findActiveReservationsByProductId(@Param("productId") String productId);

    // 주문별 활성 예약 개수 조회 (타입 수정: UUID → String)
    @Query("SELECT COUNT(sr) FROM StockReservation sr " +
            "WHERE sr.order.id = :orderId AND sr.reservationStatus = 'RESERVED'")
    long countActiveReservationsByOrderId(@Param("orderId") String orderId);

    // === 재고 수량 계산 메서드 (타입 수정) ===

    // 특정 상품의 총 예약 수량 조회 (타입 수정: UUID → String)
    @Query("SELECT COALESCE(SUM(sr.reservedQuantity), 0) FROM StockReservation sr " +
            "WHERE sr.product.id = :productId AND sr.reservationStatus = 'RESERVED'")
    Integer getTotalReservedQuantity(@Param("productId") String productId);

    // 특정 주문의 총 예약 수량 조회 (타입 수정: UUID → String)
    @Query("SELECT COALESCE(SUM(sr.reservedQuantity), 0) FROM StockReservation sr " +
            "WHERE sr.order.id = :orderId AND sr.reservationStatus = 'RESERVED'")
    Integer getTotalReservedQuantityByOrder(@Param("orderId") String orderId);

    // === 만료 관련 메서드 ===

    // 만료 시간이 지난 예약 목록 조회
    @Query("SELECT sr FROM StockReservation sr " +
            "WHERE sr.expiredAt <= :currentTime AND sr.reservationStatus = 'RESERVED'")
    List<StockReservation> findExpiredReservations(@Param("currentTime") ZonedDateTime currentTime);

    // 만료된 예약 일괄 상태 변경
    @Modifying
    @Query("UPDATE StockReservation sr " +
            "SET sr.reservationStatus = 'EXPIRED' " +
            "WHERE sr.expiredAt <= :currentTime AND sr.reservationStatus = 'RESERVED'")
    int bulkExpireReservations(@Param("currentTime") ZonedDateTime currentTime);

    // === 상태별 통계 메서드 (타입 수정) ===

    // 상품별 상태별 예약 개수 조회 (타입 수정: UUID → String)
    @Query("SELECT COUNT(sr) FROM StockReservation sr " +
            "WHERE sr.product.id = :productId AND sr.reservationStatus = :status")
    long countByProductIdAndStatus(@Param("productId") String productId,
                                   @Param("status") ReservationStatus status);

    // 주문별 상태별 예약 개수 조회 (타입 수정: UUID → String)
    @Query("SELECT COUNT(sr) FROM StockReservation sr " +
            "WHERE sr.order.id = :orderId AND sr.reservationStatus = :status")
    long countByOrderIdAndStatus(@Param("orderId") String orderId,
                                 @Param("status") ReservationStatus status);

    // === 시간 기반 조회 메서드 ===

    // 특정 시간 이후 생성된 예약 목록 조회
    @Query("SELECT sr FROM StockReservation sr " +
            "WHERE sr.reservedAt >= :fromTime")
    List<StockReservation> findReservationsAfter(@Param("fromTime") ZonedDateTime fromTime);

    // 곧 만료될 예약 목록 조회 (예: 5분 이내)
    @Query("SELECT sr FROM StockReservation sr " +
            "WHERE sr.expiredAt BETWEEN :now AND :warningTime " +
            "AND sr.reservationStatus = 'RESERVED'")
    List<StockReservation> findSoonExpiringReservations(@Param("now") ZonedDateTime now,
                                                        @Param("warningTime") ZonedDateTime warningTime);
}