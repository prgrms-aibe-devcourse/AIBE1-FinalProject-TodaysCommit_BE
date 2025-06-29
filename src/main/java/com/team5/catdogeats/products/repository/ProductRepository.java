package com.team5.catdogeats.products.repository;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.dto.MyProductSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Products, String> {
    Optional<Products> findById(String productId);

    Optional<Products> findByProductNumber(Long productNumber);

    Boolean existsByProductNumber(Long productNumber);

    void deleteById(String productId);

    // 단순 스토어 상품 개수 조회
    @Query("SELECT COUNT(p) FROM Products p WHERE p.seller.userId = :sellerId")
    Long countSellerActiveProducts(@Param("sellerId") String sellerId);

    @Query("""
    select new com.team5.catdogeats.products.domain.dto.MyProductSummaryDto(
        p.id,
        p.title,
        coalesce((select count(r) from Reviews r where r.product.id = p.id), 0),
        coalesce((select avg(r.star) from Reviews r where r.product.id = p.id), 0.0)
    )
    from Products p
    where p.seller.userId = :sellerId
    """)
    Page<MyProductSummaryDto> findSummaryBySellerId(@Param("sellerId") String sellerId, Pageable pageable);

}
