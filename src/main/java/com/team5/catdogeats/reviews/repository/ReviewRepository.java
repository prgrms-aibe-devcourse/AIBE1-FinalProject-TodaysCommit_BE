package com.team5.catdogeats.reviews.repository;

import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Reviews, String> {
    Page<Reviews> findByBuyer(Buyers buyer, Pageable pageable);

    @Query("""
        select r
        from Reviews r
        join r.product p
        where p.productNumber = :productNumber
    """)
    Page<Reviews> findByProductNumber(@Param("productNumber") Long productNumber, Pageable pageable);

    void deleteById(String reviewId);

    int countByProductId(String productId);

    @Query("SELECT COALESCE(AVG(r.star), 0.0) FROM Reviews r WHERE r.product.id = :productId")
    Double avgStarByProductId(@Param("productId") String productId);
}
