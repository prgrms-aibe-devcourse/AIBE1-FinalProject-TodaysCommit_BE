package com.team5.catdogeats.products.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.ProductCreateRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductUpdateRequestDto;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.domain.enums.StockStatus;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Products extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

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

    @Column(name = "lead_time", nullable = false)
    private Short leadTime;

    @Column(nullable = false)
    private Integer stock;

    @Version // 동시성 제어
    private Long version;

    public void decreaseStock(int qty) {
        if (this.stock < qty) throw new IllegalArgumentException("재고 부족");
        this.stock -= qty;
    }

    public static Products fromDto(ProductCreateRequestDto dto, Sellers seller, Long productNumber) {
        return Products.builder()
                .productNumber(productNumber)
                .seller(seller)
                .title(dto.title())
                .contents(dto.contents())
                .petCategory(dto.petCategory())
                .productCategory(dto.productCategory())
                .stockStatus(dto.stockStatus())
                .isDiscounted(dto.isDiscounted())
                .discountRate(dto.discountRate())
                .price(dto.price())
                .leadTime(dto.leadTime())
                .stock(dto.stock())
                .build();
    }

    public void updateFromDto(ProductUpdateRequestDto dto) {
        if (dto.title() != null) this.title = dto.title();
        if (dto.contents() != null) this.contents = dto.contents();
        if (dto.petCategory() != null) this.petCategory = dto.petCategory();
        if (dto.productCategory() != null) this.productCategory = dto.productCategory();
        if (dto.stockStatus() != null) this.stockStatus = dto.stockStatus();
        if (dto.isDiscounted() != null) this.isDiscounted = dto.isDiscounted();
        if (dto.discountRate() != null) this.discountRate = dto.discountRate();
        if (dto.stockStatus() != null) this.stockStatus = dto.stockStatus();
        if (dto.price() != null) this.price = dto.price();
        if (dto.leadTime() != null) this.leadTime = dto.leadTime();
        if (dto.stock() != null) this.stock = dto.stock();
    }
}
