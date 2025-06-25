package com.team5.catdogeats.reviews.domain.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record ReviewCreateRequestDto(
        @NotBlank(message = "상품 ID는 필수입니다.")
        String productId,

        @NotNull(message = "별점은 필수입니다.")
        @DecimalMin(value = "0.0", message = "별점은 0.0 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "별점은 5.0 이하여야 합니다.")
        Double star,

        @NotBlank(message = "리뷰 내용은 필수입니다.")
        @Size(max = 1000, message = "리뷰 내용은 1000자 이하여야 합니다.")
        String contents,

        // TODO: RequestDto에 빠져서 나중에 LLM으로 요약 후 작성됨
        String summary
) {
}
