package com.team5.catdogeats.products.domain.mapping;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.enums.AdjustmentType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_adjustments")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryAdjustments extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            referencedColumnName = "id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_inventory_adjustments_product")
    )
    private Products products;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false)
    private AdjustmentType adjustmentType;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 255)
    private String note;
}
