package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.users.domain.dto.SellerStorePageResponse;
import com.team5.catdogeats.users.service.SellerStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 스토어 페이지 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/v1/users/page")
@RequiredArgsConstructor
@Tag(name = "판매자 스토어", description = "판매자 스토어 페이지 조회 API")
public class SellerStoreController {

    private final SellerStoreService sellerStoreService;

    @Operation(
            summary = "판매자 스토어 페이지 조회",
            description = """
                    판매자의 스토어 페이지를 조회합니다.
                    
                    **파라미터 설명:**
                    - vendor-name: 판매자 상점명 (URL 경로)
                    - page: 페이지 번호 (1부터 시작, 기본값: 1)
                    - size: 페이지 크기 (기본값: 10)
                    - sort: 정렬 기준 (기본값: createdAt,desc)
                    - petCategory: 상품 카테고리 필터 (DOG,CAT - 선택사항)
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

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(value = "size", defaultValue = "10") int size,

            @Parameter(description = "정렬 기준", example = "createdAt,desc")
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort,

            @Parameter(description = "반려동물 카테고리 필터 (DOG: 강아지, CAT: 고양이)", example = "DOG")
            @RequestParam(value = "petcategory", required = false) PetCategory petcategory,

            @Parameter(description = "상품 카테고리 필터 (HANDMADE: 수제품, FINISHED: 완제품)", example = "HANDMADE")
            @RequestParam(value = "productCategory", required = false) ProductCategory productCategory,

            @Parameter(description = "추가 필터 조건 (best: 베스트상품, discount: 할인상품, new: 신규상품, exclude_sold_out: 품절제외)", example = "best")
            @RequestParam(value = "filter", required = false) String filter) {

        log.info("판매자 스토어 페이지 조회 요청 - vendorName: {}, page: {}, size: {}, sort: {}, petCategory: {},productCategory: {}, filter: {}",
                vendorName, page, size, sort, petcategory, productCategory, filter);

        SellerStorePageResponse response = sellerStoreService.getSellerStorePage(
                vendorName, page, size, sort, petcategory, productCategory, filter);

        log.info("판매자 스토어 페이지 조회 완료 - vendorName: {}, filter: {}, 상품수: {}",
                vendorName, filter, response.products().content().size());

        return ResponseEntity.ok(
                ApiResponse.success(ResponseCode.SELLER_STORE_SUCCESS, response)
        );
    }
}