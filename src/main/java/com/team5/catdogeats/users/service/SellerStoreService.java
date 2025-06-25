package com.team5.catdogeats.users.service;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.users.domain.dto.SellerStorePageResponse;
import jakarta.persistence.EntityNotFoundException;

/**
 * 판매자 스토어 페이지 서비스
 */
public interface SellerStoreService {

    /**
     * 판매자 이름으로 스토어 페이지 조회
     *
     * @param vendorName 판매자 상점명
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @param sort 정렬 기준
     * @param petCategory 상품 카테고리 필터 (optional)
     * @param filter 추가 필터 조건 (best, discount, new, exclude_sold_out)
     * @return 스토어 페이지 응답 데이터
     *
     * @throws IllegalArgumentException 잘못된 파라미터 값
     * @throws EntityNotFoundException 판매자를 찾을 수 없음
     * @throws RuntimeException 기타 처리 중 오류
     */
    SellerStorePageResponse getSellerStorePage(
            String vendorName,
            int page,
            int size,
            String sort,
            PetCategory petCategory,
            ProductCategory productCategory,
            String filter
    );
}