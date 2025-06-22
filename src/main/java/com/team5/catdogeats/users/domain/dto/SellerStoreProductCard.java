package com.team5.catdogeats.users.domain.dto;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.ProductStoreInfo;
import com.team5.catdogeats.products.domain.enums.StockStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 판매자 스토어 페이지 상품 카드 DTO
 */
@Schema(description = "스토어 상품 카드 정보")
public record SellerStoreProductCard(
        @Schema(description = "상품 ID", example = "product-uuid")
        String productId,

        @Schema(description = "상품 번호", example = "12345")
        Long productNumber,

        @Schema(description = "상품명", example = "강아지 수제 쿠키")
        String title,

        @Schema(description = "원가", example = "15000")
        Long price,

        @Schema(description = "할인된 가격", example = "12000")
        Long discountedPrice,

        @Schema(description = "할인율", example = "20.0")
        Double discountRate,

        @Schema(description = "상품 이미지 URL", example = "https://example.com/product.jpg")
        String imageUrl,

        @Schema(description = "상품 카테고리", example = "DOG")
        PetCategory category,

        @Schema(description = "재고 상태", example = "IN_STOCK")
        StockStatus stockStatus,

        @Schema(description = "평균 평점", example = "4.5")
        Double avgRating,

        @Schema(description = "리뷰 수", example = "10")
        Long reviewCount,

        @Schema(description = "베스트 점수", example = "85.5")
        Double bestScore
) {

    public static SellerStoreProductCard from(ProductStoreInfo productInfo) {
        if (productInfo == null) {
            return null;
        }

        Long discountedPrice = calculateDiscountedPrice(
                productInfo.price(),
                productInfo.isDiscounted(),
                productInfo.discountRate()
        );

        Double avgRating = productInfo.avgRating() != null
                ? BigDecimal.valueOf(productInfo.avgRating()).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        Double bestScore = productInfo.bestScore() != null
                ? BigDecimal.valueOf(productInfo.bestScore()).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return new SellerStoreProductCard(
                productInfo.productId().toString(),
                productInfo.productNumber(),
                productInfo.title(),
                productInfo.price(),
                discountedPrice,
                productInfo.discountRate(),
                productInfo.mainImageUrl(),
                productInfo.petCategory(),
                productInfo.stockStatus(),
                avgRating,
                productInfo.reviewCount(),
                bestScore
        );
    }

    private static Long calculateDiscountedPrice(Long originalPrice, boolean isDiscounted, Double discountRate) {
        if (!isDiscounted || discountRate == null || discountRate == 0.0) {
            return originalPrice;
        }

        double discountAmount = originalPrice * (discountRate / 100.0);
        return originalPrice - Math.round(discountAmount);
    }
}