package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.global.config.MybatisTransactional;
import com.team5.catdogeats.orders.domain.dto.SellerStoreStatsDTO;
import com.team5.catdogeats.orders.service.SellerStoreStatsService;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.ProductStoreInfoDTO;
import com.team5.catdogeats.products.service.SellerStoreProductService;
import com.team5.catdogeats.users.domain.dto.*;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.service.SellerStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

/**
 * 판매자 스토어 페이지 Application Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerStoreServiceImpl implements SellerStoreService {

    private final SellersRepository sellersRepository;
    private final SellerStoreProductService productService;
    private final SellerStoreStatsService sellerStoreStatsService;

    @Override
    public SellerStorePageResponse getSellerStorePage(
            String vendorName,
            int page,
            int size,
            String sort,
            PetCategory category,
            String filter) {

        log.info("판매자 스토어 페이지 조회 요청 - vendorName: {}, page: {}, size: {}, sort: {}, category: {}, filter: {}",
                vendorName, page, size, sort, category, filter);

        //1. 파라미터 검증
        validateRequestParameters(vendorName, page, size, filter);

        //2. 페이징 처리 (1-based -> 0-based 변환)
        Pageable pageable = createPageable(page - 1, size, sort, filter);

        // 3. 판매자 정보 조회
        Sellers seller = findSellerByVendorName(vendorName);

        // 4. 상품 기본 정보 조회 (Products 도메인)
        Long totalProducts = productService.countSellerActiveProducts(seller.getUserId());
        Page<ProductStoreInfoDTO> productInfoPage = productService
                .getSellerProductsBaseInfo(seller.getUserId(), category, filter, pageable);

        // 5. 상점 집계 정보 조회 (Orders 도메인)
        SellerStoreStatsDTO storeStats = sellerStoreStatsService.getSellerStoreStats(seller.getUserId());

        // 6. 응답 데이터 생성
        SellerStorePageResponse response = buildResponse(seller, totalProducts, storeStats, productInfoPage);

        //7. 페이지 번호를 다시 1-based로 조정
        response = adjustPageNumberInResponse(response, pageable.getPageNumber());

        log.info("판매자 스토어 페이지 조회 완료 - vendorName: {}, filter: {}, totalProducts: {}, totalSales: {}, pageContent: {}",
                vendorName, filter, totalProducts, storeStats.totalSalesCount(), productInfoPage.getNumberOfElements());

        return response;
    }

    /**
     * 요청 파라미터 검증
     */
    private void validateRequestParameters(String vendorName, int page, int size, String filter) {
        // 판매자명 검증
        if (vendorName == null || vendorName.trim().isEmpty()) {
            throw new IllegalArgumentException("판매자 상점명은 필수입니다.");
        }

        // 페이지 번호 검증
        if (page < 1) {
            throw new IllegalArgumentException("페이지 번호는 1 이상이어야 합니다.");
        }

        // 페이지 크기 검증
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지 크기는 1~100 사이여야 합니다.");
        }

        // 필터 값 검증
        if (filter != null && !isValidFilter(filter)) {
            throw new IllegalArgumentException(
                    "유효하지 않은 필터 값입니다. 허용된 값: best, discount, new, exclude_sold_out");
        }
    }

    /**
     * 필터 값 유효성 검증
     */
    private boolean isValidFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return true;
        }

        String normalizedFilter = filter.trim().toLowerCase();
        return normalizedFilter.equals("best") ||
                normalizedFilter.equals("discount") ||
                normalizedFilter.equals("new") ||
                normalizedFilter.equals("exclude_sold_out");
    }

    /**
     * 페이징 및 정렬 조건 생성
     */
    private Pageable createPageable(int page, int size, String sort, String filter) {
        // 페이지 크기 제한 (최대 50개) -사용자가 악의적으로 개수를 조작하여 db부하 방지
        size = Math.min(size, 50);

        // 필터가 있는 경우 필터 전용 정렬 적용
        if (filter != null) {
            Sort sortObj = createFilterBasedSort(filter, sort);
            return PageRequest.of(page, size, sortObj);
        }

        // 기본 정렬 로직
        String[] sortParts = sort.split(",");
        String property = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // 허용된 정렬 필드만 사용
        String sortProperty = switch (property.toLowerCase()) {
            case "price" -> "price";
            case "createdat", "created_at" -> "createdAt";
            default -> "createdAt"; // 기본값
        };

        Sort sortObj = Sort.by(direction, sortProperty);
        return PageRequest.of(page, size, sortObj);
    }

    /**
     *필터에 따른 정렬 생성
     */
    private Sort createFilterBasedSort(String filter, String sort) {
        return switch (filter.toLowerCase()) {
            case "best" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "discount" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "new" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "exclude_sold_out" -> {
                String[] sortParts = sort.split(",");
                String property = sortParts[0];
                Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

                String sortProperty = switch (property.toLowerCase()) {
                    case "price" -> "price";
                    case "createdat", "created_at" -> "createdAt";
                    default -> "createdAt";
                };
                yield Sort.by(direction, sortProperty);
            }
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    /**
     * 판매자 조회
     */
    private Sellers findSellerByVendorName(String vendorName) {
        return sellersRepository.findByVendorName(vendorName)
                .orElseThrow(() -> new EntityNotFoundException("판매자를 찾을 수 없습니다: " + vendorName));
    }

    /**
     * 응답 데이터 생성
     */
    private SellerStorePageResponse buildResponse(
            Sellers seller,
            Long totalProducts,
            SellerStoreStatsDTO storeStats,
            Page<ProductStoreInfoDTO> productInfoPage) {

        // 판매자 정보 생성 (집계 정보 포함)
        SellerStoreInfoDTO sellerInfo = SellerStoreInfoDTO.from(seller, totalProducts, storeStats);

        // 상품 카드로 변환
        Page<SellerStoreProductCardDTO> productCardPage = productInfoPage
                .map(SellerStoreProductCardDTO::from);

        // 페이징 응답 생성
        ProductCardPageResponseDTO productResponse = ProductCardPageResponseDTO.from(productCardPage);

        return SellerStorePageResponse.of(sellerInfo, productResponse);
    }

    /**
     * 응답 데이터의 페이지 번호를 1-based로 조정
     */
    private SellerStorePageResponse adjustPageNumberInResponse(SellerStorePageResponse response, int adjustedPage) {
        ProductCardPageResponseDTO originalProducts = response.products();

        ProductCardPageResponseDTO adjustedProducts = new ProductCardPageResponseDTO(
                originalProducts.content(),
                originalProducts.totalElements(),
                originalProducts.totalPages(),
                adjustedPage + 1, // 0-based를 1-based로 변환
                originalProducts.size(),
                originalProducts.hasNext(),
                adjustedPage > 0 // 이전 페이지 존재 여부
        );

        return new SellerStorePageResponse(
                response.sellerInfo(),
                adjustedProducts
        );
    }
}