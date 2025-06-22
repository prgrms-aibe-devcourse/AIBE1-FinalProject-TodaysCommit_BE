package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.users.domain.dto.ProductCardPageResponse;
import com.team5.catdogeats.users.domain.dto.SellerStorePageResponse;
import com.team5.catdogeats.users.service.SellerStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 스토어 페이지 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "판매자 스토어", description = "판매자 스토어 페이지 조회 API")
public class SellerStoreController {

    private final SellerStoreService sellerStoreService;

    /**
     * 판매자 스토어 페이지 조회 (필터링 추가)
     */
    @Operation(
            summary = "판매자 스토어 페이지 조회",
            description = """
                    판매자의 스토어 페이지를 조회합니다.
                    
                    **파라미터 설명:**
                    - vendor-name: 판매자 상점명 (URL 경로)
                    - page: 페이지 번호 (1부터 시작, 기본값: 1)
                    - size: 페이지 크기 (기본값: 12)
                    - sort: 정렬 기준 (기본값: createdAt,desc)
                    - category: 상품 카테고리 필터 (DOG, CAT - 선택사항)
                    - filter: 추가 필터 조건 (선택사항)
                    
                    **필터 옵션:**
                    - best: 베스트 상품 (최대 10개, 베스트 점수 높은 순)
                    - discount: 할인 상품 (할인율 높은 순)
                    - new: 신규 상품 (최근 30일, 최신순)
                    - exclude_sold_out: 품절상품 제외
                    - 없음: 기본 조회 (최신순, 모든 재고 상태 상품)
                    
                    **정렬 옵션:**
                    - createdAt,desc: 최신순 (기본값)
                    - createdAt,asc: 오래된순
                    - price,asc: 가격낮은순
                    - price,desc: 가격높은순
                    
                    **베스트 점수 계산:**
                    - 베스트 점수 = (판매량 × 0.4) + (매출액 × 0.3) + (고객평점 × 0.15) + (리뷰수 × 0.1) + (최근주문수 × 0.05)
                    
                    **페이지 번호 안내:**
                    - 첫 번째 페이지: page=1
                    - 두 번째 페이지: page=2
                    - 빈 페이지 요청 시 빈 배열 반환
                    """
    )
    @GetMapping("/{vendor-name}")
    public ResponseEntity<ApiResponse<SellerStorePageResponse>> getSellerStorePage(
            @Parameter(description = "판매자 상점명", example = "멍멍이네수제간식")
            @PathVariable("vendor-name") String vendorName,

            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,

            @Parameter(description = "페이지 크기", example = "12")
            @RequestParam(value = "size", defaultValue = "12") int size,

            @Parameter(description = "정렬 기준", example = "createdAt,desc")
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort,

            @Parameter(description = "상품 카테고리 필터 (DOG: 강아지, CAT: 고양이)", example = "DOG")
            @RequestParam(value = "category", required = false) PetCategory category,

            @Parameter(description = "추가 필터 조건 (best: 베스트상품, discount: 할인상품, new: 신규상품, exclude_sold_out: 품절제외)", example = "best")
            @RequestParam(value = "filter", required = false) String filter) {

        try {
            log.info("판매자 스토어 페이지 조회 요청 - vendorName: {}, page: {}, size: {}, sort: {}, category: {}, filter: {}",
                    vendorName, page, size, sort, category, filter);

            // 1. 페이지 번호 검증
            if (page < 1) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, "페이지 번호는 1 이상이어야 합니다."));
            }

            // 2. 페이지 크기 검증
            if (size < 1 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, "페이지 크기는 1~100 사이여야 합니다."));
            }

            // 3. 필터 값 검증
            if (filter != null && !isValidFilter(filter)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE,
                                "유효하지 않은 필터 값입니다. 허용된 값: best, discount, new, exclude_sold_out"));
            }

            // 4. 1-based 페이지를 0-based로 변환
            int adjustedPage = page - 1;

            // 5. 정렬 조건 파싱 및 페이징 객체 생성
            Pageable pageable = createPageable(adjustedPage, size, sort, filter);

            // 6. 서비스 호출 (필터 추가)
            SellerStorePageResponse response = sellerStoreService.getSellerStorePage(vendorName, category, filter, pageable);

            // 7. 응답에서 페이지 번호를 다시 1-based로 조정
            response = adjustPageNumberInResponse(response, adjustedPage);

            log.info("판매자 스토어 페이지 조회 완료 - vendorName: {}, filter: {}, 요청페이지: {}, 조정페이지: {}, 상품수: {}",
                    vendorName, filter, page, adjustedPage, response.products().content().size());

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SELLER_STORE_SUCCESS, response)
            );

        } catch (Exception e) {
            log.error("판매자 스토어 페이지 조회 중 오류 발생 - vendorName: {}, filter: {}", vendorName, filter, e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 필터 값 유효성 검증
     */
    private boolean isValidFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return true; // null이나 빈 값은 허용
        }

        String normalizedFilter = filter.trim().toLowerCase();
        return normalizedFilter.equals("best") ||
                normalizedFilter.equals("discount") ||
                normalizedFilter.equals("new") ||
                normalizedFilter.equals("exclude_sold_out");
    }

    /**
     * 페이징 및 정렬 조건 생성 (필터에 따른 정렬 우선순위 적용)
     */
    private Pageable createPageable(int page, int size, String sort, String filter) {
        // 페이지 크기 제한 (최대 50개)
        size = Math.min(size, 50);

        // 필터가 있는 경우 필터 전용 정렬 적용, 그렇지 않으면 사용자 지정 정렬 적용
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
     * 필터에 따른 상품 카드 정렬
     */
    private Sort createFilterBasedSort(String filter, String sort) {

        // 동일 조건일 때의 정렬
        return switch (filter.toLowerCase()) {
            case "best" -> Sort.by(Sort.Direction.DESC, "createdAt"); // 베스트 점수 동일 시 최신순
            case "discount" -> Sort.by(Sort.Direction.DESC, "createdAt"); // 할인율 동일 시 최신순
            case "new" -> Sort.by(Sort.Direction.DESC, "createdAt"); // 신규 상품은 무조건 최신순
            case "exclude_sold_out" -> {
                // 품절 제외는 사용자 지정 정렬 허용
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
     * 응답 데이터의 페이지 번호를 1 부터 시작
     */
    private SellerStorePageResponse adjustPageNumberInResponse(SellerStorePageResponse response, int adjustedPage) {
        ProductCardPageResponse originalProducts = response.products();

        // 새로운 ProductCardPageResponse 생성 (페이지 번호를 1-based로 조정)
        ProductCardPageResponse adjustedProducts = new ProductCardPageResponse(
                originalProducts.content(),
                originalProducts.totalElements(),
                originalProducts.totalPages(),
                adjustedPage + 1, // 0-based를 1-based로 변환
                originalProducts.size(),
                originalProducts.hasNext(),
                adjustedPage > 0 // 이전 페이지 존재 여부 (페이지 1보다 큰 경우)
        );


        return new SellerStorePageResponse(
                response.sellerInfo(),
                adjustedProducts
        );
    }
}