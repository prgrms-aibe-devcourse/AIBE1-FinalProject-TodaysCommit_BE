package com.team5.catdogeats.storage.repository;

import com.team5.catdogeats.storage.domain.mapping.ReviewsImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewImageRepository extends JpaRepository<ReviewsImages, String> {
    Optional<ReviewsImages> findByReviewsIdAndImagesId(String reviewId, String imageId);

    List<ReviewsImages> findAllByReviewsId(String reviewId);

    @Query("""
        select ri
        from ReviewsImages ri
        join fetch ri.images
        where ri.reviews.id = :reviewId
    """)
    List<ReviewsImages> findAllByReviewsIdWithImages(@Param("reviewId") String reviewId);

}
