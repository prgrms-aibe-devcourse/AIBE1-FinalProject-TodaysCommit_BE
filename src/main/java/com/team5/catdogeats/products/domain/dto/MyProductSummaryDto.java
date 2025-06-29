package com.team5.catdogeats.products.domain.dto;

public record MyProductSummaryDto(
        String productId,
        String productName,
        long reviewCount,
        double averageStar
) {}

