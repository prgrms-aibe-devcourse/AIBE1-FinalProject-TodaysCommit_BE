package com.team5.catdogeats.reviews.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.reviews.domain.dto.ReviewCreateRequestDto;
import com.team5.catdogeats.reviews.domain.dto.ReviewResponseDto;
import org.springframework.data.domain.Page;

public interface ReviewService {
    String registerReview(UserPrincipal userPrincipal, ReviewCreateRequestDto dto);

    Page<ReviewResponseDto> getReviewsByBuyer(UserPrincipal userPrincipal, int page, int size);

    Page<ReviewResponseDto> getReviewsByProductId(String productId, int page, int size);
}
