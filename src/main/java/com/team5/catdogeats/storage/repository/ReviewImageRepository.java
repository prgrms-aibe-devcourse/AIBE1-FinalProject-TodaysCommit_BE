package com.team5.catdogeats.storage.repository;

import com.team5.catdogeats.storage.domain.mapping.ReviewsImages;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewImageRepository extends JpaRepository<ReviewsImages, String> {
}
