package com.team5.catdogeats.products.mapper;

import com.team5.catdogeats.products.domain.dto.ProductStoreInfo;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ProductStoreMapper {

    /**
     * 특정 상품 ID들로 상품 기본 정보 조회 (베스트 상품용)
     */
    @Select("""
        <script>
        WITH first_images AS (
            SELECT DISTINCT ON (pi.product_id) 
                   pi.product_id, 
                   i.image_url
            FROM products_images pi
            JOIN images i ON pi.product_image_id = i.id
            ORDER BY pi.product_id, pi.created_at ASC
        ),
        review_stats AS (
            SELECT 
                r.product_id,
                ROUND(AVG(r.star), 1) as avg_rating,
                COUNT(r.id) as review_count
            FROM reviews r
            WHERE r.product_id IN 
            <foreach collection="productIds" item="productId" open="(" separator="," close=")">
                #{productId}
            </foreach>
            GROUP BY r.product_id
        )
        SELECT 
            p.id as productId,
            p.product_number as productNumber,
            p.title,
            p.price,
            p.is_discounted as isDiscounted,
            p.discount_rate as discountRate,
            COALESCE(fi.image_url, '') as mainImageUrl,
            p.petcategory as petCategory,
            p.stock_status as stockStatus,
            COALESCE(rs.avg_rating, 0.0) as avgRating,
            COALESCE(rs.review_count, 0) as reviewCount,
            0.0 as bestScore
        FROM products p
        LEFT JOIN first_images fi ON fi.product_id = p.id
        LEFT JOIN review_stats rs ON rs.product_id = p.id
        WHERE p.id IN 
        <foreach collection="productIds" item="productId" open="(" separator="," close=")">
            #{productId}
        </foreach>
        <if test="category != null and category != '' and category != 'ALL'">
            AND p.petcategory = #{category}
        </if>
        ORDER BY p.created_at DESC
        </script>
        """)
    List<ProductStoreInfo> findProductsByIds(
            @Param("productIds") List<String> productIds,
            @Param("category") String category
    );

    /**
     * 판매자 상품 정보 조회 (일반 상품용, String ID 사용)
     */
    @Select("""
        <script>
        WITH first_images AS (
            SELECT DISTINCT ON (pi.product_id) 
                   pi.product_id, 
                   i.image_url
            FROM products_images pi
            JOIN images i ON pi.product_image_id = i.id
            ORDER BY pi.product_id, pi.created_at ASC
        ),
        review_stats AS (
            SELECT 
                r.product_id,
                ROUND(AVG(r.star), 1) as avg_rating,
                COUNT(r.id) as review_count
            FROM reviews r
            GROUP BY r.product_id
        )
        SELECT 
            p.id as productId,
            p.product_number as productNumber,
            p.title,
            p.price,
            p.is_discounted as isDiscounted,
            p.discount_rate as discountRate,
            COALESCE(fi.image_url, '') as mainImageUrl,
            p.petcategory as petCategory,
            p.stock_status as stockStatus,
            COALESCE(rs.avg_rating, 0.0) as avgRating,
            COALESCE(rs.review_count, 0) as reviewCount,
            0.0 as bestScore
        FROM products p
        LEFT JOIN first_images fi ON fi.product_id = p.id
        LEFT JOIN review_stats rs ON rs.product_id = p.id
        WHERE p.seller_id = #{sellerId}
        <if test="filter == 'exclude_sold_out'">
            AND p.stock_status != 'OUT_OF_STOCK'
        </if>
        <if test="category != null and category != '' and category != 'ALL'">
            AND p.petcategory = #{category}
        </if>
        <if test="filter == 'discount'">
            AND p.is_discounted = true
        </if>
        <if test="filter == 'new'">
            AND p.created_at >= NOW() - INTERVAL '30 days'
        </if>
        ORDER BY p.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<ProductStoreInfo> findSellerProductsBaseInfo(
            @Param("sellerId") String sellerId,
            @Param("category") String category,
            @Param("filter") String filter,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 판매자 상품 개수 조회 (필터별,카레고리별,페이징을 위한 스토어의 상품 개수 조회)
     */
    @Select("""
        <script>
        SELECT COUNT(*)
        FROM products p
        WHERE p.seller_id = #{sellerId}
        <if test="filter == 'exclude_sold_out'">
            AND p.stock_status != 'OUT_OF_STOCK'
        </if>
        <if test="category != null and category != '' and category != 'ALL'">
            AND p.petcategory = #{category}
        </if>
        <if test="filter == 'discount'">
            AND p.is_discounted = true
        </if>
        <if test="filter == 'new'">
            AND p.created_at >= NOW() - INTERVAL '30 days'
        </if>
        </script>
        """)
    Long countSellerProductsForStore(
            @Param("sellerId") String sellerId,
            @Param("category") String category,
            @Param("filter") String filter
    );
}