package com.team5.catdogeats.products.service.impl;

import com.team5.catdogeats.orders.service.ProductBestScoreService;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.ProductBestScoreData;
import com.team5.catdogeats.products.domain.dto.ProductStoreInfo;
import com.team5.catdogeats.products.mapper.ProductStoreMapper;
import com.team5.catdogeats.products.repository.ProductsRepository;
import com.team5.catdogeats.products.service.SellerStoreProductService;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 판매자 스토어용 상품 서비스 구현체 (String ID 적용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerStoreProductServiceImpl implements SellerStoreProductService {

    private final ProductsRepository productsRepository;
    private final ProductStoreMapper productStoreMapper;
    private final ProductBestScoreService productBestScoreService;  // Orders 도메인 - 베스트 점수 데이터

    @Override
    @Cacheable(value = "sellerProductsBaseInfo", key = "#sellerId + '_' + #category + '_' + #filter + '_' + #pageable.pageNumber")
    public Page<ProductStoreInfo> getSellerProductsBaseInfo(String sellerId, PetCategory category, String filter, Pageable pageable) {
        log.debug("판매자 상품 정보 조회 - sellerId: {}, category: {}, filter: {}, page: {}",
                sellerId, category, filter, pageable.getPageNumber());

        String categoryStr = category != null ? category.name() : null;  // 카테고리 enum -> String로 변환 mybatis 호환성 때문에
        int limit = pageable.getPageSize();  // 한 페이지에 보여줄 상품 개수
        int offset = (int) pageable.getOffset(); // 몇 번째 상품부터 가져올지

        // 필터 값 검증
        String normalizedFilter = validateAndNormalizeFilter(filter);

        // 베스트 상품 처리
        if ("best".equals(normalizedFilter)) {
            return getBestProducts(sellerId, categoryStr);
        }

        // 일반 상품 조회
        List<ProductStoreInfo> products = productStoreMapper.findSellerProductsBaseInfo(
                sellerId, categoryStr, normalizedFilter, limit, offset
        );

        // 페이징을 위한 개수 조회
        Long total = productStoreMapper.countSellerProductsForStore(sellerId, categoryStr, normalizedFilter);

        log.debug("상품 조회 결과 - total: {}, products: {}", total, products.size());

        return new PageImpl<>(products, pageable, total);
    }

    /**
     * 베스트 상품 조회 (베스트 점수 로직 적용)
     */
    private Page<ProductStoreInfo> getBestProducts(String sellerId, String categoryStr) {
        log.debug("베스트 상품 조회 시작 - sellerId: {}, category: {}", sellerId, categoryStr);

        // 1. Orders 도메인에서 베스트 점수 계산 데이터 조회
        List<ProductBestScoreData> bestScoreDataList = productBestScoreService.getProductBestScoreData(sellerId);

        if (bestScoreDataList.isEmpty()) {
            log.debug("베스트 점수 데이터가 없음 - sellerId: {}", sellerId);
            return new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        }

        // 2. 베스트 점수 계산 및 상위 10개 상품 ID 추출
        List<String> topProductIds = bestScoreDataList.stream()
                .peek(data -> log.debug("베스트 점수 계산 - productId: {}, score: {}",
                        data.productId(), data.calculateBestScore()))
                .sorted((a, b) -> Double.compare(b.calculateBestScore(), a.calculateBestScore()))
                .limit(10)
                .map(ProductBestScoreData::productId)
                .collect(Collectors.toList());

        if (topProductIds.isEmpty()) {
            log.debug("베스트 상품이 없음 - sellerId: {}", sellerId);
            return new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        }

        // 3. 상위 상품들의 기본 정보 조회
        List<ProductStoreInfo> bestProducts = productStoreMapper.findProductsByIds(topProductIds, categoryStr);

        // 4. 베스트 점수를 ProductStoreInfo에 매핑
        Map<String, Double> bestScoreMap = bestScoreDataList.stream()
                .collect(Collectors.toMap(
                        ProductBestScoreData::productId,
                        ProductBestScoreData::calculateBestScore
                ));

        // 5. 베스트 점수 순으로 정렬된 최종 결과
        List<ProductStoreInfo> sortedBestProducts = bestProducts.stream()
                .map(product -> new ProductStoreInfo(
                        product.productId(),
                        product.productNumber(),
                        product.title(),
                        product.price(),
                        product.isDiscounted(),
                        product.discountRate(),
                        product.mainImageUrl(),
                        product.petCategory(),
                        product.stockStatus(),
                        product.avgRating(),
                        product.reviewCount(),
                        bestScoreMap.getOrDefault(product.productId(), 0.0)
                ))
                .sorted((a, b) -> Double.compare(b.bestScore(), a.bestScore()))
                .collect(Collectors.toList());

        long total = Math.min(sortedBestProducts.size(), 10);

        log.debug("베스트 상품 조회 완료 - total: {}, products: {}", total, sortedBestProducts.size());

        return new PageImpl<>(sortedBestProducts, PageRequest.of(0, 10), total);
    }

    @Override
    @Cacheable(value = "sellerActiveProductCount", key = "#sellerId")
    public Long countSellerActiveProducts(String sellerId) {
        log.debug("판매자 활성 상품 수 조회 - sellerId: {}", sellerId);
        return productsRepository.countSellerActiveProducts(sellerId);
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