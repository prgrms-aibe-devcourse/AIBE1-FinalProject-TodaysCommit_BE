package com.team5.catdogeats.products.component;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.dto.StockAvailabilityDto;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 재고 검증 컴포넌트 (리팩토링)
 * - 순환 참조를 해결하기 위해 StockReservationService 의존성 제거
 * - ProductRepository, StockReservationRepository를 직접 사용하여 재고 검증 로직 수행
 * - 단일 책임 원칙에 따라 재고 조회 및 검증 역할만 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockValidator {

    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;

    /**
     * 재고 가용성 검증
     * @param productId 상품 ID
     * @param requestedQuantity 요청 수량
     * @throws IllegalArgumentException 재고가 부족한 경우
     * @throws java.util.NoSuchElementException 상품을 찾을 수 없는 경우
     */
    public void validateStockAvailability(String productId, Integer requestedQuantity) {
        log.debug("재고 가용성 검증: productId={}, requestedQuantity={}", productId, requestedQuantity);

        StockAvailabilityDto availability = getStockAvailability(productId);

        if (!availability.canReserve(requestedQuantity)) {
            log.warn("재고 부족: 상품 ID={}, 요청 수량={}, 가용 재고={}",
                    productId, requestedQuantity, availability.getAvailableStock());
            throw new IllegalArgumentException(
                    String.format("재고가 부족합니다. (상품 ID: %s, 요청 수량: %d, 가용 재고: %d)",
                            productId, requestedQuantity, availability.getAvailableStock()));
        }

        log.debug("재고 가용성 검증 완료: productId={}", productId);
    }

    /**
     * 재고 가용성 정보 조회
     * @param productId 상품 ID
     * @return StockAvailabilityDto 재고 가용성 정보
     */
    public StockAvailabilityDto getStockAvailability(String productId) {
        log.debug("재고 가용성 정보 조회: productId={}", productId);

        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다: " + productId));

        Integer reservedStock = stockReservationRepository.getTotalReservedQuantity(productId);
        Integer actualStock = product.getStock();
        Integer availableStock = actualStock - reservedStock;

        StockAvailabilityDto result = StockAvailabilityDto.builder()
                .productId(productId)
                .actualStock(actualStock)
                .reservedStock(reservedStock)
                .availableStock(availableStock)
                .build();

        log.debug("재고 가용성 정보 조회 완료: {}", result);
        return result;
    }

    /**
     * 여러 상품의 재고 가용성 일괄 검증
     * @param productValidations 상품별 검증 정보 목록
     * @throws IllegalArgumentException 재고가 부족한 상품이 있는 경우
     */
    public void validateMultipleStockAvailability(List<ProductValidation> productValidations) {
        log.debug("다중 상품 재고 가용성 검증 시작: 상품 개수={}", productValidations.size());

        for (ProductValidation validation : productValidations) {
            validateStockAvailability(validation.productId(), validation.requestedQuantity());
        }

        log.debug("다중 상품 재고 가용성 검증 완료");
    }

    public record ProductValidation(String productId, Integer requestedQuantity) {
        public ProductValidation {
            if (productId == null || productId.trim().isEmpty()) {
                throw new IllegalArgumentException("상품 ID는 필수입니다");
            }
            if (requestedQuantity == null || requestedQuantity <= 0) {
                throw new IllegalArgumentException("요청 수량은 1 이상이어야 합니다");
            }
        }
    }
}