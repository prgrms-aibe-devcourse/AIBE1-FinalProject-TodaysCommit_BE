package com.team5.catdogeats.reviews.repository;

import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Reviews, String> {
    Page<Reviews> findByBuyer(Buyers buyer, Pageable pageable);

    Page<Reviews> findByProductId(String productId, Pageable pageable);

    void deleteById(String reviewId);
}
