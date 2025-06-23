package com.team5.catdogeats.products.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.products.domain.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 재고 예약 엔티티
 * 안전한 재고 관리를 위한 예약 시스템의 핵심 엔티티입니다.
 * 주문 생성 시 재고를 즉시 차감하지 않고 예약하여,
 * 결제 성공 시에만 확정 차감하는 안전한 재고 관리를 제공합니다.
 * 주요 기능:
 * - 재고 예약/해제/확정 상태 관리
 * - RabbitMQ를 통한 자동 만료 처리 지원
 * - 동시성 제어를 위한 Version 관리
 */
@Entity
@Table(name = "stock_reservations",
        indexes = {
                @Index(name = "idx_stock_reservation_order_id", columnList = "order_id"),
                @Index(name = "idx_stock_reservation_product_id", columnList = "product_id"),
                @Index(name = "idx_stock_reservation_status", columnList = "reservation_status"),
                @Index(name = "idx_stock_reservation_expired_at", columnList = "expired_at")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private UUID id;

    /**
     * 주문 정보 (N:1 관계)
     * 하나의 주문이 여러 상품에 대한 재고 예약을 가질 수 있습니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_reservation_order_id"))
    private Orders order;

    /**
     * 상품 정보 (N:1 관계)
     * 하나의 상품이 여러 예약을 가질 수 있습니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_reservation_product_id"))
    private Products product;

    /**
     * 예약 수량
     * 실제 차감될 재고 수량입니다.
     */
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    /**
     * 예약 상태
     * RESERVED → CONFIRMED/CANCELLED/EXPIRED 순서로 상태가 변경됩니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false, length = 20)
    private ReservationStatus reservationStatus;

    /**
     * 예약 생성 시간
     * 주문 생성과 동시에 설정됩니다.
     */
    @Column(name = "reserved_at", nullable = false)
    private ZonedDateTime reservedAt;

    /**
     * 예약 확정 시간
     * 결제 성공 시에 설정됩니다.
     */
    @Column(name = "confirmed_at")
    private ZonedDateTime confirmedAt;

    /**
     * 예약 만료 시간
     * RabbitMQ 지연 메시지 처리를 위한 만료 시간입니다.
     * 기본값: 예약 시간 + 30분
     */
    @Column(name = "expired_at", nullable = false)
    private ZonedDateTime expiredAt;

    /**
     * 동시성 제어를 위한 버전 관리
     * 여러 프로세스가 동시에 같은 예약을 처리하는 것을 방지합니다.
     */
    @Version
    private Long version;

    // === 비즈니스 메서드 ===

    /**
     * 예약 확정 처리
     * 결제 성공 시 호출되어 예약을 확정 상태로 변경합니다.
     */
    public void confirm() {
        if (this.reservationStatus != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    String.format("예약 확정 불가: 현재 상태=%s, 예약 ID=%s",
                            this.reservationStatus, this.id));
        }

        this.reservationStatus = ReservationStatus.CONFIRMED;
        this.confirmedAt = ZonedDateTime.now();
    }

    /**
     * 예약 취소 처리
     * 사용자가 결제 전에 주문을 취소할 때 호출됩니다.
     */
    public void cancel() {
        if (this.reservationStatus != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    String.format("예약 취소 불가: 현재 상태=%s, 예약 ID=%s",
                            this.reservationStatus, this.id));
        }

        this.reservationStatus = ReservationStatus.CANCELLED;
    }

    /**
     * 예약 만료 처리
     * RabbitMQ 지연 메시지를 통해 자동으로 만료 처리할 때 호출됩니다.
     */
    public void expire() {
        if (this.reservationStatus != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    String.format("예약 만료 불가: 현재 상태=%s, 예약 ID=%s",
                            this.reservationStatus, this.id));
        }

        this.reservationStatus = ReservationStatus.EXPIRED;
    }

    /**
     * 예약이 활성 상태인지 확인
     * RESERVED 상태만 활성 상태로 간주합니다.
     */
    public boolean isActive() {
        return this.reservationStatus == ReservationStatus.RESERVED;
    }

    /**
     * 예약이 만료되었는지 확인
     * 현재 시간이 만료 시간을 초과했는지 검사합니다.
     */
    public boolean isExpired() {
        return ZonedDateTime.now().isAfter(this.expiredAt);
    }

    /**
     * 정적 팩토리 메서드: 새로운 재고 예약 생성
     */
    public static StockReservation createReservation(Orders order, Products product,
                                                     Integer quantity, int expirationMinutes) {
        ZonedDateTime now = ZonedDateTime.now();

        return StockReservation.builder()
                .order(order)
                .product(product)
                .reservedQuantity(quantity)
                .reservationStatus(ReservationStatus.RESERVED)
                .reservedAt(now)
                .expiredAt(now.plusMinutes(expirationMinutes))
                .build();
    }
}