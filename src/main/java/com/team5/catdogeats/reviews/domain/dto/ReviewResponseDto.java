package com.team5.catdogeats.reviews.domain.dto;

import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.storage.domain.dto.ReviewImageResponseDto;

import java.util.List;

public record ReviewResponseDto(
        String id,
        String productId,
        String buyerId,
        Double star,
        String contents,
        String summary,
        List<ReviewImageResponseDto> images
) {
    public static ReviewResponseDto fromEntity(Reviews review, List<ReviewImageResponseDto> images) {
        return new ReviewResponseDto(
                review.getId(),
                review.getProduct().getId(),
                review.getBuyer().getUserId(),
                review.getStar(),
                review.getContents(),
                review.getSummary(),
                images
        );
    }
}
