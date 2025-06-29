package com.team5.catdogeats.reviews.repository;

import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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

    // 전체 평균/갯수
    @Query("""
        select coalesce(avg(r.star),0.0), count(r)
        from Reviews r
        where r.product.seller.userId = :sellerId
    """)
    List<Object[]> findAvgAndCountBySellerId(@Param("sellerId") String sellerId);



    // 별점 구간별 개수 (0점대~5점)
    @Query("""
        select
            case 
                when floor(r.star) = 0 then 0
                when floor(r.star) = 1 then 1
                when floor(r.star) = 2 then 2
                when floor(r.star) = 3 then 3
                when floor(r.star) = 4 then 4
                when floor(r.star) >= 5 then 5
            end as groupStar,
            count(r)
        from Reviews r
        where r.product.seller.userId = :sellerId
        group by groupStar
    """)
    List<Object[]> findGroupStarCountBySellerId(@Param("sellerId") String sellerId);
}
