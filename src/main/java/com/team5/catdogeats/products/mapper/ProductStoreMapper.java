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
        ),
        order_stats AS (
            SELECT 
                oi.product_id,
                COALESCE(SUM(oi.quantity), 0) as total_sales_quantity,
                COALESCE(SUM(oi.quantity * oi.price), 0) as total_revenue,
                COALESCE(COUNT(DISTINCT CASE 
                    WHEN o.created_at >= NOW() - INTERVAL '30 days' 
                    THEN o.id 
                    ELSE NULL 
                END), 0) as recent_order_count
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.order_status IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED')
            GROUP BY oi.product_id
        ),
        best_scores AS (
            SELECT 
                p.id as product_id,
                ROUND(
                    (COALESCE(os.total_sales_quantity, 0) * 0.4) +
                    (COALESCE(os.total_revenue / 1000.0, 0) * 0.3) +
                    (COALESCE(rs.avg_rating, 0) * 0.15) +
                    (COALESCE(rs.review_count, 0) * 0.1) +
                    (COALESCE(os.recent_order_count, 0) * 0.05), 2
                ) as best_score
            FROM products p
            LEFT JOIN order_stats os ON p.id = os.product_id
            LEFT JOIN review_stats rs ON p.id = rs.product_id
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
            COALESCE(bs.best_score, 0.0) as bestScore
        FROM products p
        LEFT JOIN first_images fi ON fi.product_id = p.id
        LEFT JOIN review_stats rs ON rs.product_id = p.id
        LEFT JOIN best_scores bs ON bs.product_id = p.id
        WHERE p.seller_id = #{sellerId}::uuid
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
        <choose>
            <when test="filter == 'best'">
                ORDER BY bs.best_score DESC NULLS LAST, p.created_at DESC
                LIMIT 10
            </when>
            <when test="filter == 'discount'">
                ORDER BY p.discount_rate DESC NULLS LAST, p.created_at DESC
                LIMIT #{limit} OFFSET #{offset}
            </when>
            <when test="filter == 'new'">
                ORDER BY p.created_at DESC
                LIMIT #{limit} OFFSET #{offset}
            </when>
            <otherwise>
                ORDER BY p.created_at DESC
                LIMIT #{limit} OFFSET #{offset}
            </otherwise>
        </choose>
        </script>
        """)
    List<ProductStoreInfo> findSellerProductsForStore(
            @Param("sellerId") UUID sellerId,
            @Param("category") String category,
            @Param("filter") String filter,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM products p
        WHERE p.seller_id = #{sellerId}::uuid
        <choose>
            <when test="filter == 'exclude_sold_out'">
                AND p.stock_status != 'OUT_OF_STOCK'
            </when>
            <otherwise>
                AND p.stock_status IN ('IN_STOCK', 'LOW_STOCK')
            </otherwise>
        </choose>
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
            @Param("sellerId") UUID sellerId,
            @Param("category") String category,
            @Param("filter") String filter
    );
}