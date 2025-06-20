package com.team5.catdogeats.products.service.impl;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.dto.ProductStoreInfo;
import com.team5.catdogeats.products.mapper.ProductStoreMapper;
import com.team5.catdogeats.products.repository.ProductsRepository;
import com.team5.catdogeats.products.service.SellerStoreProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerStoreProductServiceImpl implements SellerStoreProductService {

    private final ProductsRepository productsRepository;  // JPA - 단순 CRUD
    private final ProductStoreMapper productStoreMapper;  // MyBatis - 복잡한 조회


    //MyBatis 사용 - 상품 목록조회(상품카드)
    @Override
    @Cacheable(value = "sellerProducts", key = "#sellerId + '_' + #category + '_' + #pageable.pageNumber")
    public Page<ProductStoreInfo> getSellerProductsForStore(UUID sellerId, PetCategory category, Pageable pageable) {
        log.debug("판매자 스토어 상품 목록 조회 - sellerId: {}, category: {}, page: {}",
                sellerId, category, pageable.getPageNumber());

        String categoryStr = category != null ? category.name() : null;
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();

        // MyBatis로 한 번의 쿼리로 모든 정보 조회
        List<ProductStoreInfo> products = productStoreMapper.findSellerProductsForStore(
                sellerId, categoryStr, limit, offset
        );

        // 페이징을 위한 개수 조회 (카테고리 - 강아지,고양이 별 개수)
        Long total = productStoreMapper.countSellerProductsForStore(sellerId, categoryStr);

        return new PageImpl<>(products, pageable, total);
    }

    //ProductsRepository - JPA 사용 ,총 상품 수 조회
    @Override
    @Cacheable(value = "sellerActiveProductCount", key = "#sellerId")
    public Long countSellerActiveProducts(UUID sellerId) {

        return productsRepository.countSellerActiveProducts(sellerId);
    }

}