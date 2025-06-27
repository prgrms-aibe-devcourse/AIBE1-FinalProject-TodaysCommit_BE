package com.team5.catdogeats.products.domain.dto;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.domain.enums.StockStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductUpdateRequestDto(
        @NotNull(message = "productId는 필수입니다.")
        String productId,
        String title,
        String contents,
        PetCategory petCategory,
        ProductCategory productCategory,
        StockStatus stockStatus,
        Boolean isDiscounted,
        @DecimalMin(value = "0.0", message = "할인율은 0 이상이어야 합니다.")
        Double discountRate,
        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        Long price,
        @Min(value = 0, message = "리드타임은 0 이상이어야 합니다.")
        Short leadTime,
        @PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
        Integer stock
) {
}
