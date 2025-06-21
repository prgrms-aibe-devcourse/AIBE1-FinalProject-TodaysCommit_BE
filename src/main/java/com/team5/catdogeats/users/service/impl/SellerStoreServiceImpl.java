package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.orders.domain.dto.SellerStoreStats;
import com.team5.catdogeats.orders.service.SellerStoreStatsService;
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
 * 판매자 스토어 페이지 Application Service (수정됨)
 * 여러 도메인 서비스를 조합하여 완전한 스토어 페이지 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerStoreServiceImpl implements SellerStoreService {

    private final SellersRepository sellersRepository;
    private final SellerStoreProductService productService;          // Products 도메인 (수정됨)
    private final SellerStoreStatsService sellerStoreStatsService;   // Orders 도메인 (새로 추가)

    @Override
    public SellerStorePageResponse getSellerStorePage(String vendorName, PetCategory category, String filter, Pageable pageable) {
        log.info("판매자 스토어 페이지 조회 요청 - vendorName: {}, category: {}, filter: {}, page: {}",
                vendorName, category, filter, pageable.getPageNumber());

        // 1. 판매자 정보 조회 (Users 도메인)
        Sellers seller = sellersRepository.findByVendorName(vendorName)
                .orElseThrow(() -> new EntityNotFoundException("판매자를 찾을 수 없습니다: " + vendorName));

        // 2. 상품 기본 정보 조회 (Products 도메인) - bestScore 없는 ProductStoreInfo
        Long totalProducts = productService.countSellerActiveProducts(seller.getUserId());
        Page<ProductStoreInfo> productInfoPage = productService
                .getSellerProductsBaseInfo(seller.getUserId(), category, filter, pageable);

        // 3. 상점 집계 정보 조회 (Orders 도메인)
        SellerStoreStats storeStats = sellerStoreStatsService.getSellerStoreStats(seller.getUserId());

        // 4. 판매자 정보 생성 (집계 정보 포함)
        SellerStoreInfo sellerInfo = SellerStoreInfo.from(seller, totalProducts, storeStats);

        // 5. 상품 카드로 변환 (bestScore 없는 ProductStoreInfo 그대로 사용)
        Page<SellerStoreProductCard> productCardPage = productInfoPage
                .map(SellerStoreProductCard::from);

        // 6. 페이징 응답 생성
        ProductCardPageResponse productResponse = ProductCardPageResponse.from(productCardPage);

        log.info("판매자 스토어 페이지 조회 완료 - vendorName: {}, filter: {}, totalProducts: {}, totalSales: {}, pageContent: {}",
                vendorName, filter, totalProducts, storeStats.totalSalesCount(), productCardPage.getNumberOfElements());

        return SellerStorePageResponse.of(sellerInfo, productResponse);
    }
}