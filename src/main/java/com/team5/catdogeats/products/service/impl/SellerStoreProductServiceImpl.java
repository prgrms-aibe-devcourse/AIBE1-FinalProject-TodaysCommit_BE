package com.team5.catdogeats.products.service.impl;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.ProductStoreInfo;
import com.team5.catdogeats.products.mapper.ProductStoreMapper;
import com.team5.catdogeats.products.repository.ProductsRepository;
import com.team5.catdogeats.products.service.SellerStoreProductService;
import com.team5.catdogeats.users.domain.dto.SellerStoreStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerStoreProductServiceImpl implements SellerStoreProductService {

    private final ProductsRepository productsRepository;  // JPA - 단순 CRUD
    private final ProductStoreMapper productStoreMapper;  // MyBatis - 복잡한 조회

    // MyBatis 사용 - 상품 목록조회(상품카드) + 필터링
    @Override
    @Cacheable(value = "sellerProducts", key = "#sellerId + '_' + #category + '_' + #filter + '_' + #pageable.pageNumber")
    public Page<ProductStoreInfo> getSellerProductsForStore(UUID sellerId, PetCategory category, String filter, Pageable pageable) {
        log.debug("판매자 스토어 상품 목록 조회 - sellerId: {}, category: {}, filter: {}, page: {}",
                sellerId, category, filter, pageable.getPageNumber());

        String categoryStr = category != null ? category.name() : null;
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();

        // 필터 값 검증 및 정규화
        String normalizedFilter = validateAndNormalizeFilter(filter);

        // 베스트 상품은 무조건 10개만 (페이징 무시)
        if ("best".equals(normalizedFilter)) {
            List<ProductStoreInfo> products = productStoreMapper.findSellerProductsForStore(
                    sellerId, categoryStr, normalizedFilter, 10, 0
            );

            // 베스트 상품은 항상 10개 또는 그 이하로 고정
            long total = Math.min(products.size(), 10);

            log.debug("베스트 상품 조회 결과 - total: {}, products: {}", total, products.size());

            return new PageImpl<>(products, PageRequest.of(0, 10), total);
        }

        // MyBatis로 한 번의 쿼리로 모든 정보 조회 (필터 포함)
        List<ProductStoreInfo> products = productStoreMapper.findSellerProductsForStore(
                sellerId, categoryStr, normalizedFilter, limit, offset
        );

        // 페이징을 위한 개수 조회 (카테고리 + 필터별 개수)
        Long total = productStoreMapper.countSellerProductsForStore(sellerId, categoryStr, normalizedFilter);

        log.debug("조회 결과 - total: {}, products: {}", total, products.size());

        return new PageImpl<>(products, pageable, total);
    }

    // ProductsRepository - JPA 사용, 총 상품 수 조회
    @Override
    @Cacheable(value = "sellerActiveProductCount", key = "#sellerId")
    public Long countSellerActiveProducts(UUID sellerId) {
        return productsRepository.countSellerActiveProducts(sellerId);
    }

    // MyBatis 사용 - 판매자 스토어 통계 조회 (판매량 + 배송 정보)
    @Override
    @Cacheable(value = "sellerStoreStats", key = "#sellerId", cacheManager = "cacheManager")
    public SellerStoreStats getSellerStoreStats(UUID sellerId) {
        log.debug("판매자 스토어 통계 조회 - sellerId: {}", sellerId);

        try {
            SellerStoreStats stats = productStoreMapper.getSellerStoreStats(sellerId);

            if (stats == null) {
                log.debug("통계 데이터가 없어 기본값 반환 - sellerId: {}", sellerId);
                return new SellerStoreStats(0L, 0L, 0.0, 0.0, 0.0);
            }

            log.debug("통계 조회 완료 - sellerId: {}, totalSales: {}, avgDeliveryDays: {}",
                    sellerId, stats.totalSalesQuantity(), stats.avgDeliveryDays());

            return stats;
        } catch (Exception e) {
            log.error("판매자 스토어 통계 조회 중 오류 발생 - sellerId: {}", sellerId, e);
            // 오류 발생 시 기본값 반환
            return new SellerStoreStats(0L, 0L, 0.0, 0.0, 0.0);
        }
    }

    /**
     * 필터 값 검증 및 정규화
     */
    private String validateAndNormalizeFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return null;
        }

        String normalizedFilter = filter.trim().toLowerCase();

        // 허용된 필터 값 검증
        return switch (normalizedFilter) {
            case "best", "discount", "new", "exclude_sold_out" -> normalizedFilter;
            default -> {
                log.warn("유효하지 않은 필터 값: {}. 기본값(null)으로 처리합니다.", filter);
                yield null;
            }
        };
    }
}