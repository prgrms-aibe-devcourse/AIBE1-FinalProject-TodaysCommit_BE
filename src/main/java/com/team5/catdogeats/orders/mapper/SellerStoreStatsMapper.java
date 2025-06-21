package com.team5.catdogeats.orders.mapper;

import com.team5.catdogeats.orders.domain.dto.SellerStoreStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SellerStoreStatsMapper {

    /**
     * 판매자 상점 집계 정보 조회
     */
    @Select("""
        WITH sales_data AS (
            SELECT 
                COALESCE(SUM(oi.quantity), 0) as total_sales_count
            FROM products p
            LEFT JOIN order_items oi ON p.id = oi.product_id
            LEFT JOIN orders o ON oi.order_id = o.id
            WHERE p.seller_id = #{sellerId}
            AND (o.order_status IS NULL OR o.order_status IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED'))
        ),
        delivery_data AS (
            SELECT 
                COALESCE(AVG(EXTRACT(EPOCH FROM (s.delivered_at - o.created_at)) / 86400), 0) as avg_delivery_days
            FROM shipments s
            JOIN orders o ON s.order_id = o.id
            WHERE s.seller_id = #{sellerId}
            AND s.delivered_at IS NOT NULL
            AND o.created_at IS NOT NULL
            AND s.delivered_at > o.created_at
            AND s.delivered_at >= NOW() - INTERVAL '6 months'
        ),
        review_data AS (
            SELECT 
                COALESCE(COUNT(r.id), 0) as total_reviews
            FROM products p
            LEFT JOIN reviews r ON p.id = r.product_id
            WHERE p.seller_id = #{sellerId}
        )
        SELECT 
            sd.total_sales_count as totalSalesCount,
            dd.avg_delivery_days as avgDeliveryDays,
            rd.total_reviews as totalReviews
        FROM sales_data sd
        CROSS JOIN delivery_data dd
        CROSS JOIN review_data rd
        """)
    SellerStoreStats getSellerStoreStats(@Param("sellerId") String sellerId);
}