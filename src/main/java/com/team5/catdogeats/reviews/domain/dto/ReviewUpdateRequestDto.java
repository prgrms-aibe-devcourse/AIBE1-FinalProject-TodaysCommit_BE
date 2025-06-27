package com.team5.catdogeats.reviews.domain.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewUpdateRequestDto(
        @NotNull(message = "reviewId는 필수입니다.")
        String reviewId,
        Double star,
        String contents,
        String summary
) {
}
