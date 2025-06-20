package com.team5.catdogeats.products.repository;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductsRepository extends JpaRepository<Products, UUID> {

    /**
     * 판매자 스토어 페이지용 상품 목록 조회 (카테고리 필터링 포함)
     * 서비스에서 DTO 변환 처리
     */
    @Query("""
        SELECT p
        FROM Products p
        WHERE p.seller.userId = :sellerId
        AND (:category IS NULL OR p.petCategory = :category)
        AND p.stockStatus = 'IN_STOCK'
        ORDER BY p.createdAt DESC
        """)
    Page<Products> findSellerProductsForStore(
            @Param("sellerId") UUID sellerId,
            @Param("category") PetCategory category,
            Pageable pageable
    );

    /**
     * 판매자의 활성 상품 총 개수 조회
     */
    @Query("""
        SELECT COUNT(p)
        FROM Products p
        WHERE p.seller.userId = :sellerId
        AND p.stockStatus = 'IN_STOCK'
        """)
    Long countSellerActiveProducts(@Param("sellerId") UUID sellerId);

    /**
     * 상품의 첫 번째 이미지 URL 조회
     */
    @Query(value = """
        SELECT i.image_url
        FROM products_images pi
        JOIN images i ON pi.product_image_id = i.id
        WHERE pi.product_id = :productId
        ORDER BY pi.created_at ASC
        LIMIT 1
        """, nativeQuery = true)
    String findFirstImageUrlByProductId(@Param("productId") UUID productId);

    /**
     * 상품의 평균 평점 조회
     */
    @Query("""
        SELECT COALESCE(AVG(r.star), 0.0)
        FROM Reviews r
        WHERE r.product.id = :productId
        """)
    Double findAvgRatingByProductId(@Param("productId") UUID productId);

    /**
     * 상품의 리뷰 개수 조회
     */
    @Query("""
        SELECT COUNT(r)
        FROM Reviews r
        WHERE r.product.id = :productId
        """)
    Long findReviewCountByProductId(@Param("productId") UUID productId);
}