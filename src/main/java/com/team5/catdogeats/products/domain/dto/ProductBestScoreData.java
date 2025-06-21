package com.team5.catdogeats.products.domain.dto;

/**
 * 베스트 점수 계산을 위한 상품별 통계 데이터 DTO
 * Orders 도메인에서 Products 도메인으로 전달되는 데이터
 */
public record ProductBestScoreData(
        String productId,
        Long salesQuantity,      // 판매량
        Long totalRevenue,       // 매출액
        Double avgRating,        // 평균 평점
        Long reviewCount,        // 리뷰 수
        Long recentOrderCount    // 최근 30일 주문 수
) {

    /**
     * 베스트 점수 계산
     * 베스트 상품 점수 = (판매량 * 0.4) + (매출액 * 0.3) + (고객평점 * 0.15) + (리뷰수 * 0.1) + (최근주문수 * 0.05)
     */
    public Double calculateBestScore() {
        // 각 지표를 0-100 점수로 정규화
        double normalizedSales = normalizeValue(salesQuantity != null ? salesQuantity : 0L, 100L);
        double normalizedRevenue = normalizeValue(totalRevenue != null ? totalRevenue : 0L, 1000000L); // 100만원 기준
        double normalizedRating = normalizeRating(avgRating != null ? avgRating : 0.0);
        double normalizedReviews = normalizeValue(reviewCount != null ? reviewCount : 0L, 50L);
        double normalizedRecent = normalizeValue(recentOrderCount != null ? recentOrderCount : 0L, 20L);

        // 가중치 적용
        double bestScore = (normalizedSales * 0.4) +
                (normalizedRevenue * 0.3) +
                (normalizedRating * 0.15) +
                (normalizedReviews * 0.1) +
                (normalizedRecent * 0.05);

        return Math.round(bestScore * 100.0) / 100.0; // 소수점 2자리
    }

    /**
     * 값을 0-100 점수로 정규화
     */
    private double normalizeValue(Long value, Long maxReference) {
        if (value == null || value <= 0) return 0.0;
        return Math.min(100.0, (value.doubleValue() / maxReference.doubleValue()) * 100.0);
    }

    /**
     * 평점을 0-100 점수로 정규화 (5점 만점 기준)
     */
    private double normalizeRating(Double rating) {
        if (rating == null || rating <= 0) return 0.0;
        return Math.min(100.0, (rating / 5.0) * 100.0);
    }

    /**
     * 기본값으로 생성 (데이터가 없는 경우)
     */
    public static ProductBestScoreData empty(String productId) {
        return new ProductBestScoreData(productId, 0L, 0L, 0.0, 0L, 0L);
    }
}