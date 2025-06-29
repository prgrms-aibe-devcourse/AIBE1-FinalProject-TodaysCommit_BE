package com.team5.catdogeats.users.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.products.domain.dto.MyProductResponseDto;
import com.team5.catdogeats.products.domain.enums.SellerProductSortType;
import com.team5.catdogeats.reviews.domain.dto.SellerReviewSummaryResponseDto;
import org.springframework.data.domain.Page;

public interface SellerRatingService {
    Page<MyProductResponseDto> getProductsBySeller(UserPrincipal userPrincipal, int page, int size, SellerProductSortType sortType);

    SellerReviewSummaryResponseDto getSellerReviewSummary(UserPrincipal userPrincipal);
}
