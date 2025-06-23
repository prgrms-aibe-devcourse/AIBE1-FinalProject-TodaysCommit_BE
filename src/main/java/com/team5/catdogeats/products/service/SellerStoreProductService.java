package com.team5.catdogeats.products.service;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.ProductStoreInfoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 판매자 스토어용 상품 서비스 (수정됨)
 * Products 도메인에서 상품 정보만 담당, 주문/판매 통계는 Orders 도메인으로 분리
 */
public interface SellerStoreProductService {

    /**
     * 판매자의 상품 기본 정보 조회 (페이징 + 필터링)
     * 상품 정보와 해당상품의 리뷰 정보(모든 리뷰 개수 , 리뷰 평균점수),
     *
     * @param sellerId 판매자 ID (String)
     * @param category 상품 카테고리 필터
     * @param filter 추가 필터 조건 (best, discount, new, exclude_sold_out)
     * @param pageable 페이징 정보
     * @return 상품 기본 정보 페이지
     */
    Page<ProductStoreInfoDTO> getSellerProductsBaseInfo(String sellerId, PetCategory category, String filter, Pageable pageable);

    /**
     * 판매자의 활성 상품 총 개수 조회 (재고 있는것)
     *
     * @param sellerId 판매자 ID (String)
     * @return 활성 상품 총 개수
     */
    Long countSellerActiveProducts(String sellerId);

}