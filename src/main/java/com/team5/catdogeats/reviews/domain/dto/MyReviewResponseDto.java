package com.team5.catdogeats.reviews.domain.dto;

import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.storage.domain.dto.ReviewImageResponseDto;

import java.util.List;

// 특정 buyerId로 리뷰 조회시 response
public record MyReviewResponseDto(
        String id,
        String productName,
        Double star,
        String contents,
        String updatedAt,
        List<ReviewImageResponseDto> images
) {
    public static MyReviewResponseDto fromEntity(Reviews review, List<ReviewImageResponseDto> images) {
        return new MyReviewResponseDto(
                review.getId(),
                review.getProduct().getTitle(),
                review.getStar(),
                review.getContents(),
                review.getUpdatedAt().toString(),
                images
        );
    }
}
