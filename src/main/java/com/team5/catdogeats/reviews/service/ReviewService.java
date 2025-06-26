package com.team5.catdogeats.reviews.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.reviews.domain.dto.ReviewCreateRequestDto;
import com.team5.catdogeats.reviews.domain.dto.ReviewDeleteRequestDto;
import com.team5.catdogeats.reviews.domain.dto.ReviewResponseDto;
import com.team5.catdogeats.reviews.domain.dto.ReviewUpdateRequestDto;
import org.springframework.data.domain.Page;

public interface ReviewService {
    String registerReview(UserPrincipal userPrincipal, ReviewCreateRequestDto dto);

    Page<ReviewResponseDto> getReviewsByBuyer(UserPrincipal userPrincipal, int page, int size);

    Page<ReviewResponseDto> getReviewsByProductNumber(Long productNumber, int page, int size);

    void updateReview(ReviewUpdateRequestDto dto);

    void deleteReview(ReviewDeleteRequestDto dto);
}
