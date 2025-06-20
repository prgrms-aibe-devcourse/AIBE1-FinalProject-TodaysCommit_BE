package com.team5.catdogeats.products.mapper;

import com.team5.catdogeats.products.domain.dto.ProductStoreInfo;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.UUID;


@Mapper
public interface ProductStoreMapper {

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
            COALESCE(rs.review_count, 0) as reviewCount
        FROM products p
        LEFT JOIN first_images fi ON fi.product_id = p.id
        LEFT JOIN review_stats rs ON rs.product_id = p.id
        WHERE p.seller_id = #{sellerId}::uuid
        AND p.stock_status = 'IN_STOCK'
        <if test="category != null and category != ''">
            AND p.petcategory = #{category}
        </if>
        ORDER BY p.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<ProductStoreInfo> findSellerProductsForStore(
            @Param("sellerId") UUID sellerId,
            @Param("category") String category,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM products p
        WHERE p.seller_id = #{sellerId}::uuid
        AND p.stock_status = 'IN_STOCK'
        <if test="category != null and category != ''">
            AND p.petcategory = #{category}
        </if>
        </script>
        """)
    Long countSellerProductsForStore(
            @Param("sellerId") UUID sellerId,
            @Param("category") String category
    );
}