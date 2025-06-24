package com.team5.catdogeats.products.component;

import com.team5.catdogeats.products.dto.StockAvailabilityDto;
import com.team5.catdogeats.products.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 재고 검증 컴포넌트
 * StockReservationService를 위임받아 재고 검증 기능을 제공합니다.
 * 패키지 위치: roducts/component
 * - Spring 컴포넌트로 관리되는 재고 검증 전용 클래스
 * - 여러 서비스에서 재사용 가능한 공통 컴포넌트
 * - 재고 검증이라는 단일 책임만 담당
 * 사용 예시:
 * - 장바구니 서비스: 상품 추가 시 재고 검증
 * - 주문 서비스: 주문 생성 시 재고 검증
 * - 상품 상세 페이지: 재고 가용성 표시
 * - 대량 주문 서비스: 여러 상품 일괄 검증
 * 위임 패턴 적용으로 기존 StockReservationService 로직 재활용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockValidator {

    private final StockReservationService stockReservationService;

    /**
     * 재고 가용성 검증
     * StockReservationService의 검증 로직을 위임받아 사용합니다.
     *
     * @param productId 상품 ID
     * @param requestedQuantity 요청 수량
     * @throws IllegalArgumentException 재고가 부족한 경우
     * @throws java.util.NoSuchElementException 상품을 찾을 수 없는 경우
     */
    public void validateStockAvailability(String productId, Integer requestedQuantity) {
        log.debug("재고 가용성 검증: productId={}, requestedQuantity={}", productId, requestedQuantity);

        // 기존 StockReservationService의 검증 로직 위임
        stockReservationService.validateStockAvailability(productId, requestedQuantity);

        log.debug("재고 가용성 검증 완료: productId={}", productId);
    }

    /**
     * 재고 가용성 정보 조회
     * StockReservationService의 조회 로직을 위임받아 사용합니다.
     *
     * @param productId 상품 ID
     * @return StockAvailabilityDto 재고 가용성 정보
     */
    public StockAvailabilityDto getStockAvailability(String productId) {
        log.debug("재고 가용성 정보 조회: productId={}", productId);

        // 기존 StockReservationService의 조회 로직 위임
        StockAvailabilityDto result = stockReservationService.getStockAvailability(productId);

        log.debug("재고 가용성 정보 조회 완료: {}", result);
        return result;
    }

    /**
     * 여러 상품의 재고 가용성 일괄 검증
     * 여러 상품에 대해 한 번에 재고 가용성을 검증합니다.
     * 하나라도 재고가 부족한 경우 즉시 예외가 발생합니다.
     *
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

    /**
     * 상품 검증 정보를 담는 Record
     *
     * @param productId 상품 ID
     * @param requestedQuantity 요청 수량
     */
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