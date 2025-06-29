package com.team5.catdogeats.products.domain.dto;

import com.team5.catdogeats.storage.domain.dto.ProductImageResponseDto;

import java.util.List;

// 특정 sellerId로 상품 조회시 response
public record MyProductResponseDto(
    String productId,
    String productName,
    int reviewCount,
    double averageStar,
    ProductImageResponseDto image
) {
}
