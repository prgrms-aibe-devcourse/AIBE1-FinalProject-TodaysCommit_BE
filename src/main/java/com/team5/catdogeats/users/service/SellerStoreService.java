package com.team5.catdogeats.users.service;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.users.domain.dto.SellerStorePageResponse;
import org.springframework.data.domain.Pageable;

/**
 * 판매자 스토어 페이지 서비스
 */
public interface SellerStoreService {

    /**
     * 판매자 이름으로 스토어 페이지 조회
     *
     * @param vendorName 판매자 상점명
     * @param category 상품 카테고리 필터 (optional)
     * @param pageable 페이징 정보
     * @return 스토어 페이지 응답 데이터
     */
    SellerStorePageResponse getSellerStorePage(String vendorName, PetCategory category, Pageable pageable);
}