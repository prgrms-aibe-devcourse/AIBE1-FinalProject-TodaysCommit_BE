package com.team5.catdogeats.reviews.domain.dto;

import java.util.Map;

public record SellerReviewSummaryResponseDto(
        double averageStar,
        long totalReviews,
        // 0~5점대 그룹별 개수 (예: {0:0, 1:2, 2:2, 3:2, 4:7, 5:7})
        Map<Integer, Long> starGroupCount
) {
}
