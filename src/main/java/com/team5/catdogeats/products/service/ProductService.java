package com.team5.catdogeats.products.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.products.domain.dto.MyProductResponseDto;
import com.team5.catdogeats.products.domain.dto.ProductCreateRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductDeleteRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductUpdateRequestDto;
import com.team5.catdogeats.products.domain.enums.SellerProductSortType;
import com.team5.catdogeats.reviews.domain.dto.SellerReviewSummaryResponseDto;
import org.springframework.data.domain.Page;

public interface ProductService {
    String registerProduct(UserPrincipal userPrincipal, ProductCreateRequestDto dto);

    Page<MyProductResponseDto> getProductsBySeller(UserPrincipal userPrincipal, int page, int size, SellerProductSortType sortType);

    SellerReviewSummaryResponseDto getSellerReviewSummary(UserPrincipal userPrincipal);

    void updateProduct(ProductUpdateRequestDto dto);

    void deleteProduct(ProductDeleteRequestDto dto);
}
