package com.team5.catdogeats.products.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.products.domain.dto.ProductCreateRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductDeleteRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductUpdateRequestDto;

public interface ProductService {
    String registerProduct(UserPrincipal userPrincipal, ProductCreateRequestDto dto);
    void updateProduct(ProductUpdateRequestDto dto);
    void deleteProduct(ProductDeleteRequestDto dto);
}
