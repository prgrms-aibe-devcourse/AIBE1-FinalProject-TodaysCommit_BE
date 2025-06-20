package com.team5.catdogeats.users.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 상품 카드 페이징 응답 DTO
 */
@Schema(description = "상품 카드 페이징 응답")
public record ProductCardPageResponse(
        @Schema(description = "상품 목록")
        List<SellerStoreProductCard> content,

        @Schema(description = "전체 요소 수", example = "50")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages,

        @Schema(description = "현재 페이지", example = "0")
        int currentPage,

        @Schema(description = "페이지 크기", example = "12")
        int size,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "이전 페이지 존재 여부", example = "false")
        boolean hasPrevious
) {

    public static ProductCardPageResponse from(Page<SellerStoreProductCard> page) {
        return new ProductCardPageResponse(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}