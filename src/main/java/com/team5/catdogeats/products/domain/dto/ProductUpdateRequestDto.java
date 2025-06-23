package com.team5.catdogeats.products.domain.dto;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.domain.enums.StockStatus;
import jakarta.validation.constraints.NotNull;

public record ProductUpdateRequestDto(
        @NotNull(message = "productId는 필수입니다.")
        String productId,
        String title,
        String contents,
        PetCategory petCategory,
        ProductCategory productCategory,
        StockStatus stockStatus,
        Boolean isDiscounted,
        Double discountRate,
        Long price,
        Short leadTime,
        Integer stock
) {
}
