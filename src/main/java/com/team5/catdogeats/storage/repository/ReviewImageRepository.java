package com.team5.catdogeats.storage.repository;

import com.team5.catdogeats.storage.domain.mapping.ReviewsImages;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewImageRepository extends JpaRepository<ReviewsImages, String> {
    Optional<ReviewsImages> findByReviewsIdAndImagesId(String reviewId, String imageId);
    List<ReviewsImages> findAllByReviewsId(String reviewId);
}
