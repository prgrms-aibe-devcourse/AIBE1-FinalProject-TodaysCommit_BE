package com.team5.catdogeats.products.service.impl;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.dto.ProductStoreInfo;
import com.team5.catdogeats.products.repository.ProductsRepository;
import com.team5.catdogeats.products.service.SellerStoreProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 판매자 스토어 페이지용 상품 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerStoreProductServiceImpl implements SellerStoreProductService {

    private final ProductsRepository productsRepository;

    @Override
    public Page<ProductStoreInfo> getSellerProductsForStore(UUID sellerId, PetCategory category, Pageable pageable) {
        log.debug("판매자 스토어 상품 목록 조회 - sellerId: {}, category: {}, page: {}",
                sellerId, category, pageable.getPageNumber());

        // 1. 기본 상품 정보 조회 (페이징)
        Page<Products> productPage = productsRepository.findSellerProductsForStore(sellerId, category, pageable);

        // 2. Products를 ProductStoreInfo로 변환하고 이미지와 리뷰 정보 보완
        List<ProductStoreInfo> enrichedProducts = productPage.getContent()
                .stream()
                .map(this::convertToProductStoreInfo)
                .map(this::enrichProductInfo)
                .toList();

        return new PageImpl<>(enrichedProducts, pageable, productPage.getTotalElements());
    }

    @Override
    public Long countSellerActiveProducts(UUID sellerId) {
        log.debug("판매자 활성 상품 수 조회 - sellerId: {}", sellerId);

        return productsRepository.countSellerActiveProducts(sellerId);
    }

    /**
     * Products 엔티티를 ProductStoreInfo DTO로 변환
     */
    private ProductStoreInfo convertToProductStoreInfo(Products product) {
        return new ProductStoreInfo(
                product.getId(),
                product.getProductNumber(),
                product.getTitle(),
                product.getPrice(),
                product.isDiscounted(),
                product.getDiscountRate(),
                "", // 이미지는 별도 조회
                product.getPetCategory(),
                product.getStockStatus(),
                0.0, // 평점은 별도 조회
                0L  // 리뷰 수는 별도 조회
        );
    }

    /**
     * 상품 정보에 이미지와 리뷰 정보 추가
     */
    private ProductStoreInfo enrichProductInfo(ProductStoreInfo originalInfo) {
        try {
            // 첫 번째 이미지 URL 조회
            String imageUrl = productsRepository.findFirstImageUrlByProductId(originalInfo.productId());
            if (imageUrl == null) {
                imageUrl = "";
            }

            // 평균 평점 조회
            Double avgRating = productsRepository.findAvgRatingByProductId(originalInfo.productId());
            if (avgRating == null) {
                avgRating = 0.0;
            }

            // 리뷰 수 조회
            Long reviewCount = productsRepository.findReviewCountByProductId(originalInfo.productId());
            if (reviewCount == null) {
                reviewCount = 0L;
            }

            return new ProductStoreInfo(
                    originalInfo.productId(),
                    originalInfo.productNumber(),
                    originalInfo.title(),
                    originalInfo.price(),
                    originalInfo.isDiscounted(),
                    originalInfo.discountRate(),
                    imageUrl,
                    originalInfo.petCategory(),
                    originalInfo.stockStatus(),
                    avgRating,
                    reviewCount
            );
        } catch (Exception e) {
            log.warn("상품 정보 보완 중 오류 발생 - productId: {}, error: {}",
                    originalInfo.productId(), e.getMessage());
            return originalInfo; // 원본 정보 반환
        }
    }
}