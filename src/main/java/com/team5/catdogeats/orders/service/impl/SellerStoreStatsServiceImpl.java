package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.orders.domain.dto.SellerStoreStatsDTO;
import com.team5.catdogeats.orders.mapper.SellerStoreStatsMapper;
import com.team5.catdogeats.orders.service.SellerStoreStatsService;
import com.team5.catdogeats.users.controller.SellerStoreExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * 판매자 상점 집계 정보 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerStoreStatsServiceImpl implements SellerStoreStatsService {

    private final SellerStoreStatsMapper sellerStoreStatsMapper;

    @Override
    public SellerStoreStatsDTO getSellerStoreStats(String sellerId) {
        log.debug("판매자 상점 집계 정보 조회 - sellerId: {}", sellerId);
        try {
            // 1. 파라미터 검증
            validateSellerId(sellerId);

            // 2. MyBatis 매퍼를 통한 집계 정보 조회
            SellerStoreStatsDTO stats = sellerStoreStatsMapper.getSellerStoreStats(sellerId);

            if (stats == null) {
                log.debug("집계 정보가 없어 기본값 반환 - sellerId: {}", sellerId);
                return SellerStoreStatsDTO.empty();
            }

            log.debug("집계 정보 조회 완료 - sellerId: {}, totalSales: {}, avgDelivery: {}, totalReviews: {}",
                    sellerId, stats.totalSalesCount(), stats.avgDeliveryDays(), stats.totalReviews());

            return stats;

        } catch (IllegalArgumentException e) {
            // 파라미터 검증 실패
            log.warn("집계 정보 조회 파라미터 오류 - sellerId: {}, error: {}", sellerId, e.getMessage());
            throw e;
        } catch (Exception e) {
            // Orders 도메인 특화 예외로 변환
            log.error("집계 정보 조회 중 오류 발생 - sellerId: {}", sellerId, e);
            throw new SellerStoreExceptionHandler.OrderStatsRetrievalException("판매자 집계 정보 조회 실패 - sellerId: " + sellerId, e);
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