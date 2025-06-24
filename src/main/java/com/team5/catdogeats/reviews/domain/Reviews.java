package com.team5.catdogeats.reviews.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.reviews.domain.dto.ReviewCreateRequestDto;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Reviews extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_product_id"))
    private Products product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_buyer_id"))
    private Buyers buyer;

    @Column(nullable = false, columnDefinition = "DECIMAL(2,1)")
    private Double star;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contents;

    @Column(length = 100)
    private String summary;

    public static Reviews fromDto(ReviewCreateRequestDto dto, Buyers buyer, Products product) {
        return Reviews.builder()
                .buyer(buyer)
                .product(product)
                .star(dto.star())
                .contents(dto.contents())
                .summary(dto.summary())
                .build();
    }
}
