package com.team5.catdogeats.products.service;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.ProductStoreInfo;
import com.team5.catdogeats.users.domain.dto.SellerStoreStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SellerStoreProductService {

    /**
     * 판매자의 스토어 페이지용 상품 목록 조회 (페이징 + 필터링)
     *
     * @param sellerId 판매자 ID (UUID)
     * @param category 상품 카테고리 필터 (null이면 전체)
     * @param filter 추가 필터 조건 (best, discount, new, exclude_sold_out)
     * @param pageable 페이징 정보
     * @return 상품 정보 페이지
     */
    Page<ProductStoreInfo> getSellerProductsForStore(UUID sellerId, PetCategory category, String filter, Pageable pageable);

    /**
     * 판매자의 활성 상품 총 개수 조회
     *
     * @param sellerId 판매자 ID (UUID)
     * @return 활성 상품 총 개수
     */
    Long countSellerActiveProducts(UUID sellerId);

    /**
     * 판매자 스토어 통계 조회 (판매량 + 배송 정보)
     *
     * @param sellerId 판매자 ID (UUID)
     * @return 판매자 스토어 통계 정보
     */
    SellerStoreStats getSellerStoreStats(UUID sellerId);
}