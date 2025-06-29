package com.team5.catdogeats.products.domain.dto;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.domain.enums.StockStatus;


/**
 * 판매자 스토어 페이지에서 사용할 상품 정보 DTO
 * Products 도메인에서 Users 도메인으로 데이터 전달용
 */
public record ProductStoreInfoDTO(
        String productId,
        Long productNumber,
        String title,
        Long price,
        boolean isDiscounted,
        Double discountRate,
        String mainImageUrl,
        PetCategory petCategory,
        ProductCategory productCategory,
        StockStatus stockStatus,
        Double avgRating,
        Long reviewCount,
        Double bestScore
) {}