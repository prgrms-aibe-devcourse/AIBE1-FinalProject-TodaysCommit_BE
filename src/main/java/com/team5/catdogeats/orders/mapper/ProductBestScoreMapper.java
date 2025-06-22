package com.team5.catdogeats.orders.mapper;

import com.team5.catdogeats.products.domain.dto.ProductBestScoreData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductBestScoreMapper {

    /**
     * 판매자의 모든 상품에 대한 베스트 점수 계산 데이터 조회
     */
    @Select("""
        WITH sales_data AS (
            SELECT 
                p.id as product_id,
                COALESCE(SUM(oi.quantity), 0) as sales_quantity,
                COALESCE(SUM(oi.price * oi.quantity), 0) as total_revenue
            FROM products p
            LEFT JOIN order_items oi ON p.id = oi.product_id
            LEFT JOIN orders o ON oi.order_id = o.id
            WHERE p.seller_id = #{sellerId}
            AND (o.order_status IS NULL OR o.order_status IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED'))
            GROUP BY p.id
        ),
        review_data AS (
            SELECT 
                p.id as product_id,
                COALESCE(AVG(r.star), 0.0) as avg_rating,
                COALESCE(COUNT(r.id), 0) as review_count
            FROM products p
            LEFT JOIN reviews r ON p.id = r.product_id
            WHERE p.seller_id = #{sellerId}
            GROUP BY p.id
        ),
        recent_orders AS (
            SELECT 
                p.id as product_id,
                COALESCE(COUNT(DISTINCT o.id), 0) as recent_order_count
            FROM products p
            LEFT JOIN order_items oi ON p.id = oi.product_id
            LEFT JOIN orders o ON oi.order_id = o.id
            WHERE p.seller_id = #{sellerId}
            AND o.created_at >= NOW() - INTERVAL '30 days'
            AND o.order_status IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED')
            GROUP BY p.id
        )
        SELECT 
            p.id as productId,
            COALESCE(sd.sales_quantity, 0) as salesQuantity,
            COALESCE(sd.total_revenue, 0) as totalRevenue,
            COALESCE(rd.avg_rating, 0.0) as avgRating,
            COALESCE(rd.review_count, 0) as reviewCount,
            COALESCE(ro.recent_order_count, 0) as recentOrderCount
        FROM products p
        LEFT JOIN sales_data sd ON p.id = sd.product_id
        LEFT JOIN review_data rd ON p.id = rd.product_id
        LEFT JOIN recent_orders ro ON p.id = ro.product_id
        WHERE p.seller_id = #{sellerId}
        ORDER BY p.id
        """)
    List<ProductBestScoreData> getProductBestScoreDataBySeller(@Param("sellerId") String sellerId);
}