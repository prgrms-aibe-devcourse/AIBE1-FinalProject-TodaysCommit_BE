package com.team5.catdogeats.products.repository;

import com.team5.catdogeats.products.domain.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Products, String> {
    Optional<Products> findById(String productId);

    Boolean existsByProductNumber(Long productNumber);

    void deleteById(String productId);

    // 단순 스토어 상품 개수 조회
    @Query("SELECT COUNT(p) FROM Products p WHERE p.seller.userId = :sellerId")
    Long countSellerActiveProducts(@Param("sellerId") String sellerId);
}
