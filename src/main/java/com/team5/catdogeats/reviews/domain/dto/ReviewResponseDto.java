package com.team5.catdogeats.reviews.domain.dto;

import com.team5.catdogeats.reviews.domain.Reviews;

public record ReviewResponseDto(
        String id,
        String productId,
        String buyerId,
        Double star,
        String contents,
        String summary
) {
    public static ReviewResponseDto fromEntity(Reviews review) {
        return new ReviewResponseDto(
                review.getId(),
                review.getProduct().getId(),
                review.getBuyer().getUserId(),
                review.getStar(),
                review.getContents(),
                review.getSummary()
        );
    }
}
