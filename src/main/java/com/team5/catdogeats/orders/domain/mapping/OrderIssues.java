package com.team5.catdogeats.orders.domain.mapping;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.IssueStatus;
import com.team5.catdogeats.orders.domain.enums.IssueType;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "order_issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderIssues extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_issues_order"))
    private Orders orders;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_issues_buyer"))
    private Buyers buyers;

    @Column(name = "issue_request_number", length = 50)
    private String issueRequestNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 10)
    private IssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_status", nullable = false, length = 10)
    @Builder.Default
    private IssueStatus issueStatus = IssueStatus.REQUESTED;

    @Column(name = "issue_request_date", nullable = false)
    private Date issueRequestDate;

    @Column(name = "issue_complete_date")
    private Date issueCompleteDate;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "delivery_fee", nullable = false)
    @Builder.Default
    private Long deliveryFee = 0L;

    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    private Long discountAmount = 0L;

    @OneToMany(mappedBy = "orderIssues", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderIssueItems> orderIssueItems;

}