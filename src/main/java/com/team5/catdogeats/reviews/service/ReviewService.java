package com.team5.catdogeats.reviews.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.reviews.domain.dto.*;
import org.springframework.data.domain.Page;

public interface ReviewService {
    String registerReview(UserPrincipal userPrincipal, ReviewCreateRequestDto dto);

    Page<MyReviewResponseDto> getReviewsByBuyer(UserPrincipal userPrincipal, int page, int size);

    Page<ProductReviewResponseDto> getReviewsByProductNumber(Long productNumber, int page, int size);

    void updateReview(ReviewUpdateRequestDto dto);

    void deleteReview(ReviewDeleteRequestDto dto);
}
