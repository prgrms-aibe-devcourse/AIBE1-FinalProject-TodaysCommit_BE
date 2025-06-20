package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.ProductStoreInfo;
import com.team5.catdogeats.products.service.SellerStoreProductService;
import com.team5.catdogeats.users.domain.dto.*;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.service.SellerStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

/**
 * 판매자 스토어 페이지 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerStoreServiceImpl implements SellerStoreService {

    private final SellersRepository sellersRepository;
    private final SellerStoreProductService sellerStoreProductService; // Products 도메인 의존성

    @Override
    public SellerStorePageResponse getSellerStorePage(String vendorName, PetCategory category, String filter, Pageable pageable) {
        log.info("판매자 스토어 페이지 조회 요청 - vendorName: {}, category: {}, filter: {}, page: {}",
                vendorName, category, filter, pageable.getPageNumber());

        // 1. 판매자 정보 조회
        Sellers seller = sellersRepository.findByVendorName(vendorName)
                .orElseThrow(() -> new EntityNotFoundException("판매자를 찾을 수 없습니다: " + vendorName));

        // 2. 총 상품 수 조회 (필터와 무관하게 전체 활성 상품 수)
        Long totalProducts = sellerStoreProductService.countSellerActiveProducts(seller.getUserId());

        // 3. 판매자 정보 생성
        SellerStoreInfo sellerInfo = SellerStoreInfo.from(seller, totalProducts);

        // 4. 상품 목록 조회 (페이징 + 필터링)
        Page<ProductStoreInfo> productInfoPage = sellerStoreProductService
                .getSellerProductsForStore(seller.getUserId(), category, filter, pageable);

        // 5. 상품 카드로 변환
        Page<SellerStoreProductCard> productCardPage = productInfoPage
                .map(SellerStoreProductCard::from);

        // 6. 페이징 응답 생성
        ProductCardPageResponse productResponse = ProductCardPageResponse.from(productCardPage);

        log.info("판매자 스토어 페이지 조회 완료 - vendorName: {}, filter: {}, totalProducts: {}, pageContent: {}",
                vendorName, filter, totalProducts, productCardPage.getNumberOfElements());

        return SellerStorePageResponse.of(sellerInfo, productResponse);
    }
}