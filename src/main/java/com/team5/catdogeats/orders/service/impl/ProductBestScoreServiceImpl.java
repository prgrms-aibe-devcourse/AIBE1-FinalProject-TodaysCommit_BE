package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.orders.mapper.ProductBestScoreMapper;
import com.team5.catdogeats.orders.service.ProductBestScoreService;
import com.team5.catdogeats.products.domain.dto.ProductBestScoreData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 상품 베스트 점수 계산 (주문수량 점수)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductBestScoreServiceImpl implements ProductBestScoreService {

    private final ProductBestScoreMapper productBestScoreMapper;

    @Override
    @Cacheable(value = "productBestScoreData", key = "#sellerId")
    public List<ProductBestScoreData> getProductBestScoreData(String sellerId) {
        log.debug("판매자 상품 베스트 점수 데이터 조회 - sellerId: {}", sellerId);

        try {
            List<ProductBestScoreData> scoreData = productBestScoreMapper.getProductBestScoreDataBySeller(sellerId);

            log.debug("베스트 점수 데이터 조회 완료 - sellerId: {}, 상품 수: {}", sellerId, scoreData.size());

            return scoreData;
        } catch (Exception e) {
            log.error("베스트 점수 데이터 조회 중 오류 발생 - sellerId: {}", sellerId, e);
            return List.of();
        }
    }
}