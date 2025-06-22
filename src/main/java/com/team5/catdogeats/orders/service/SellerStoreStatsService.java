package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.orders.domain.dto.SellerStoreStats;


/**
 * 판매자 상점 표시용 집계 정보 서비스
 */
public interface SellerStoreStatsService {

    /**
     * 구매자에게 보여줄 판매자 집계 정보 조회
     *
     * @param sellerId 판매자 ID
     * @return 상점 표시용 집계 정보 (총 판매량, 평균 배송일, 총 리뷰 수)
     */
    SellerStoreStats getSellerStoreStats(String sellerId);
}