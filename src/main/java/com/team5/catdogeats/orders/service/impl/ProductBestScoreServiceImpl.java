package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.orders.mapper.ProductBestScoreMapper;
import com.team5.catdogeats.orders.service.ProductBestScoreService;
import com.team5.catdogeats.products.domain.dto.ProductBestScoreDataDTO;
import com.team5.catdogeats.users.controller.SellerStoreExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 상품 베스트 점수 계산 (주문수량 점수)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductBestScoreServiceImpl implements ProductBestScoreService {

    private final ProductBestScoreMapper productBestScoreMapper;

    @Override
    public List<ProductBestScoreDataDTO> getProductBestScoreData(String sellerId) {
        log.debug("판매자 상품 베스트 점수 데이터 조회 - sellerId: {}", sellerId);
        try {
            // 1. 파라미터 검증
            validateSellerId(sellerId);

            // 2. 베스트 점수 데이터 조회
            List<ProductBestScoreDataDTO> scoreData = productBestScoreMapper.getProductBestScoreDataBySeller(sellerId);

            log.debug("베스트 점수 데이터 조회 완료 - sellerId: {}, 상품 수: {}", sellerId, scoreData.size());

            return scoreData;

        } catch (IllegalArgumentException e) {
            // 파라미터 검증 실패
            log.warn("베스트 점수 데이터 조회 파라미터 오류 - sellerId: {}, error: {}", sellerId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("베스트 점수 데이터 조회 중 오류 발생 - sellerId: {}", sellerId, e);
            throw new SellerStoreExceptionHandler.OrderStatsRetrievalException("베스트 점수 데이터 조회 실패 - sellerId: " + sellerId, e);
        }
    }

    /**
     *  판매자 ID 검증
     */
    private void validateSellerId(String sellerId) {
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new IllegalArgumentException("판매자 ID는 필수입니다.");
        }
    }
}