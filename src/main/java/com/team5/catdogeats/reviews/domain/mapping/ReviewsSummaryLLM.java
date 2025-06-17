package com.team5.catdogeats.reviews.domain.mapping;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.products.domain.Products;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "reviews_summary_llm")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewsSummaryLLM extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_summary_llm_product_id"))
    private Products product;

    @Column(name = "positive_review", nullable = false, columnDefinition = "TEXT")
    private String positiveReview;

    @Column(name = "negative_review", nullable = false, columnDefinition = "TEXT")
    private String negativeReview;
}
