package com.team5.catdogeats.products.service.impl;


import com.team5.catdogeats.orders.service.ProductBestScoreService;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.ProductBestScoreDataDTO;
import com.team5.catdogeats.products.domain.dto.ProductStoreInfoDTO;
import com.team5.catdogeats.products.mapper.ProductStoreMapper;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.service.SellerStoreProductService;
import com.team5.catdogeats.users.controller.SellerStoreExceptionHandler.ProductDataRetrievalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 판매자 스토어용 상품 서비스 구현체 (예외 처리 적용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerStoreProductServiceImpl implements SellerStoreProductService {

    private final ProductRepository productsRepository;
    private final ProductStoreMapper productStoreMapper;
    private final ProductBestScoreService productBestScoreService;

    @Override
    public Page<ProductStoreInfoDTO> getSellerProductsBaseInfo(String sellerId, PetCategory category, String filter, Pageable pageable) {
        log.debug("판매자 상품 정보 조회 - sellerId: {}, category: {}, filter: {}, page: {}",
                sellerId, category, filter, pageable.getPageNumber());
        try {
            // 1. 파라미터 검증
            validateParameters(sellerId, pageable);

            String categoryStr = category != null ? category.name() : null;
            String normalizedFilter = validateAndNormalizeFilter(filter);

            // 2. 베스트 상품 특별 처리
            if ("best".equals(normalizedFilter)) {
                return getBestProducts(sellerId, categoryStr);
            }

            // 3. 일반 상품 조회
            int limit = pageable.getPageSize();
            int offset = (int) pageable.getOffset();

            List<ProductStoreInfoDTO> products = productStoreMapper.findSellerProductsBaseInfo(
                    sellerId, categoryStr, normalizedFilter, limit, offset
            );

            Long total = productStoreMapper.countSellerProductsForStore(sellerId, categoryStr, normalizedFilter);

            log.debug("상품 조회 결과 - total: {}, products: {}", total, products.size());

            return new PageImpl<>(products, pageable, total);

        } catch (IllegalArgumentException e) {
            // 파라미터 검증 실패
            log.warn("상품 조회 파라미터 오류 - sellerId: {}, error: {}", sellerId, e.getMessage());
            throw e;
        } catch (Exception e) {
            // Products 도메인 특화 예외로 변환
            log.error("상품 데이터 조회 실패 - sellerId: {}", sellerId, e);
            throw new ProductDataRetrievalException("상품 정보 조회 실패 - sellerId: " + sellerId, e);
        }
    }

    /**
     * 품절 제외 필터용
     */
    @Override
    public Long countSellerActiveProducts(String sellerId) {
        log.debug("판매자 활성 상품 수 조회 - sellerId: {}", sellerId);

        try {
            validateSellerId(sellerId);

            return productsRepository.countSellerActiveProducts(sellerId);
        } catch (IllegalArgumentException e) {
            // 파라미터 검증 실패
            log.warn("상품 개수 조회 파라미터 오류 - sellerId: {}, error: {}", sellerId, e.getMessage());
            throw e;
        } catch (Exception e) {
            //users.SellerStoreExceptionHandler
            log.error("상품 개수 조회 실패 - sellerId: {}", sellerId, e);
            throw new ProductDataRetrievalException("상품 개수 조회 실패 - sellerId: " + sellerId, e);
        }
    }

    /**
     *  파라미터 검증 로직
     */
    private void validateParameters(String sellerId, Pageable pageable) {
        validateSellerId(sellerId);

        if (pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("페이지 크기는 100을 초과할 수 없습니다.");
        }

        if (pageable.getPageSize() < 1) {
            throw new IllegalArgumentException("페이지 크기는 1 이상이어야 합니다.");
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

    /**
     *  필터 값 검증 및 정규화 (예외 발생 적용)
     */
    private String validateAndNormalizeFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return null;
        }

        String normalizedFilter = filter.trim().toLowerCase();

        //  검증 실패 시 IllegalArgumentException 발생
        return switch (normalizedFilter) {
            case "best", "discount", "new", "exclude_sold_out" -> normalizedFilter;
            default -> throw new IllegalArgumentException("유효하지 않은 필터 값: " + filter +
                    ". 허용된 값: best, discount, new, exclude_sold_out");
        };
    }

    /**
     * 베스트 상품 조회 (예외 처리 적용)
     */
    private Page<ProductStoreInfoDTO> getBestProducts(String sellerId, String categoryStr) {
        log.debug("베스트 상품 조회 시작 - sellerId: {}, category: {}", sellerId, categoryStr);

        try {
            // 1. Orders 도메인에서 베스트 점수 계산 데이터 조회
            List<ProductBestScoreDataDTO> bestScoreDataList = productBestScoreService.getProductBestScoreData(sellerId);

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
                    .map(ProductBestScoreDataDTO::productId)
                    .collect(Collectors.toList());

            if (topProductIds.isEmpty()) {
                log.debug("베스트 상품이 없음 - sellerId: {}", sellerId);
                return new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            }

            // 3. 상위 상품들의 기본 정보 조회
            List<ProductStoreInfoDTO> bestProducts = productStoreMapper.findProductsByIds(topProductIds, categoryStr);

            // 4. 베스트 점수를 ProductStoreInfo에 매핑
            Map<String, Double> bestScoreMap = bestScoreDataList.stream()
                    .collect(Collectors.toMap(
                            ProductBestScoreDataDTO::productId,
                            ProductBestScoreDataDTO::calculateBestScore
                    ));

            // 5. 베스트 점수 순으로 정렬된 최종 결과
            List<ProductStoreInfoDTO> sortedBestProducts = bestProducts.stream()
                    .map(product -> new ProductStoreInfoDTO(
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

        } catch (Exception e) {
            //  베스트 상품 조회 실패 시 Products 도메인 예외로 변환
            log.error("베스트 상품 조회 실패 - sellerId: {}", sellerId, e);
            throw new ProductDataRetrievalException("베스트 상품 조회 실패 - sellerId: " + sellerId, e);
        }
    }
}