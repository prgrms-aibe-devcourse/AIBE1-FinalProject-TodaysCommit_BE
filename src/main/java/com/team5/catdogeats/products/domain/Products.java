package com.team5.catdogeats.products.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.domain.enums.StockStatus;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Products extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private UUID id;

    @Column(name = "product_number", nullable = false, unique = true)
    private Long productNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", referencedColumnName = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_products_seller_id"))
    private Sellers seller;

    @Column(length = 50, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contents;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PetCategory petCategory;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProductCategory productCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status", length = 15)
    private StockStatus stockStatus;

    @Column(name = "is_discounted")
    private boolean isDiscounted = false;

    @Column(name = "discount_rate", columnDefinition = "DECIMAL(10,2)")
    private Double discountRate;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "lead_time", nullable = false)
    private Short leadTime;
}
