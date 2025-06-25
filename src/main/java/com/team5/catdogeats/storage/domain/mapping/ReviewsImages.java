package com.team5.catdogeats.storage.domain.mapping;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.storage.domain.Images;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ReviewsImages extends BaseEntity {
    @Id
    @Column(length = 36)
    private String id;  // 이 id는 reviews 테이블의 id를 FK로 참조

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "review_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_images_review_id"))
    private Reviews reviews;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_image_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_images_image_id"))
    private Images images;
}
