package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.products.domain.dto.ProductBestScoreData;

import java.util.List;

/**
 * 상품 베스트 점수 계산을 위한 Orders 도메인 서비스
 * Products 도메인에 판매/주문 통계 데이터 제공
 */
public interface ProductBestScoreService {

    /**
     * 판매자의 모든 상품에 대한 베스트 점수 계산 데이터 조회
     *
     * @param sellerId 판매자 ID
     * @return 상품별 베스트 점수 계산 데이터 목록
     */
    List<ProductBestScoreData> getProductBestScoreData(String sellerId);

    /**
     * 특정 상품들의 베스트 점수 계산 데이터 조회
     *
     * @param productIds 상품 ID 목록
     * @return 상품별 베스트 점수 계산 데이터 목록
     */
    List<ProductBestScoreData> getProductBestScoreDataByProductIds(List<String> productIds);
}