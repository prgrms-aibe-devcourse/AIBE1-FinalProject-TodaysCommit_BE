package com.team5.catdogeats.orders.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "shipments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Shipments extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_shipments_order"))
    private Orders orders;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_shipments_seller"))
    private Sellers seller;

    @Column(length = 50, nullable = false)
    private String courier;

    @Column(name = "tracking_number", length = 100, nullable = false)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private ZonedDateTime shippedAt;

    @Column(name = "delivered_at")
    private ZonedDateTime deliveredAt;
}
