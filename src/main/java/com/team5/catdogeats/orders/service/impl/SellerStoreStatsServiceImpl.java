package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.orders.domain.dto.SellerStoreStats;
import com.team5.catdogeats.orders.mapper.SellerStoreStatsMapper;
import com.team5.catdogeats.orders.service.SellerStoreStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 판매자 상점 집계 정보 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerStoreStatsServiceImpl implements SellerStoreStatsService {

    private final SellerStoreStatsMapper sellerStoreStatsMapper;

    @Override
    @Cacheable(value = "sellerStoreStats", key = "#sellerId")
    public SellerStoreStats getSellerStoreStats(String sellerId) {
        log.debug("판매자 상점 집계 정보 조회 - sellerId: {}", sellerId);

        try {
            SellerStoreStats stats = sellerStoreStatsMapper.getSellerStoreStats(sellerId);

            if (stats == null) {
                log.debug("집계 정보가 없어 기본값 반환 - sellerId: {}", sellerId);
                return SellerStoreStats.empty();
            }

            log.debug("집계 정보 조회 완료 - sellerId: {}, totalSales: {}, avgDelivery: {}, totalReviews: {}",
                    sellerId, stats.totalSalesCount(), stats.avgDeliveryDays(), stats.totalReviews());

            return stats;
        } catch (Exception e) {
            log.error("집계 정보 조회 중 오류 발생 - sellerId: {}", sellerId, e);
            return SellerStoreStats.empty();
        }
    }
}