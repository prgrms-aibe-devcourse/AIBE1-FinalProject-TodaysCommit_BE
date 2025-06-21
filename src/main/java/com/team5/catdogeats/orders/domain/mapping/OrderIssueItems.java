package com.team5.catdogeats.orders.domain.mapping;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.orders.domain.enums.IssueStatus;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_issue_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderIssueItems extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_issue_items_seller_id"))
    private Sellers sellers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_issue_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_issue_items_issue"))
    private OrderIssues orderIssues;

    @Column(name = "order_item_id", nullable = false, length = 36)
    private String orderItemId;  // 주문 아이템과 연동 필요하면 ManyToOne 매핑 가능

    @Column(nullable = false)
    private int quantity;

    @Column(name = "item_price", nullable = false)
    private Long itemPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private IssueStatus issueStatus = IssueStatus.REQUESTED;

    @Column(name = "issue_reason")
    private String issueReason;

    @Column(name = "refund_method", length = 50)
    private String refundMethod;

    @Column(name = "refund_amount", nullable = false)
    @Builder.Default
    private Long refundAmount = 0L;

    @Column(name = "delivery_fee", nullable = false)
    @Builder.Default
    private Long deliveryFee = 0L;

    @Column(name = "return_fee", nullable = false)
    @Builder.Default
    private Long returnFee = 0L;

}
