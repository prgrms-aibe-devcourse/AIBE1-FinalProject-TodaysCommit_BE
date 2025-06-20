package com.team5.catdogeats.payments.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.orders.domain.mapping.OrderIssues;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "refunds")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Refunds extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_refunds_payment_id"))
    private Payments payments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_issue_id", foreignKey = @ForeignKey(name = "fk_refunds_order_issue"))
    private OrderIssues orderIssues;  // order_issues 테이블에 대응하는 엔티티

    @Column(name = "buyer_id", nullable = false, length = 36)
    private String buyerId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "refunded_at")
    private ZonedDateTime refundedAt;
}
