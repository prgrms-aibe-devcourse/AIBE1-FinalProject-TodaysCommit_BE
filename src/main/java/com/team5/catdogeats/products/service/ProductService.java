package com.team5.catdogeats.products.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.products.domain.dto.MyProductResponseDto;
import com.team5.catdogeats.products.domain.dto.ProductCreateRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductDeleteRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductUpdateRequestDto;
import com.team5.catdogeats.reviews.domain.dto.MyReviewResponseDto;
import org.springframework.data.domain.Page;

public interface ProductService {
    String registerProduct(UserPrincipal userPrincipal, ProductCreateRequestDto dto);

    Page<MyProductResponseDto> getProductsBySeller(UserPrincipal userPrincipal, int page, int size);

    void updateProduct(ProductUpdateRequestDto dto);

    void deleteProduct(ProductDeleteRequestDto dto);
}
