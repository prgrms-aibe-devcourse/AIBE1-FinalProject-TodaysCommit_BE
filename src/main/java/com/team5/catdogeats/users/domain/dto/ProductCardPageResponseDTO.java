package com.team5.catdogeats.users.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 상품 카드 페이징 응답 DTO
 */
@Schema(description = "상품 카드 페이징 응답")
public record ProductCardPageResponseDTO(
        @Schema(description = "상품 카드 목록")
        List<SellerStoreProductCardDTO> content,

        @Schema(description = "전체 요소 수", example = "100")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "10")
        int totalPages,

        @Schema(description = "현재 페이지 번호 (1-based)", example = "1")
        int currentPage,

        @Schema(description = "페이지 크기", example = "12")
        int size,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "이전 페이지 존재 여부", example = "false")
        boolean hasPrevious
) {

    public static ProductCardPageResponseDTO from(Page<SellerStoreProductCardDTO> page) {
        return new ProductCardPageResponseDTO(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber() + 1, // 0-based를 1-based로 변환
                page.getSize(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}