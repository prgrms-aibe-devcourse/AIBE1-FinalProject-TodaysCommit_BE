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
     * 판매자 스토어 페이지 조회
     */
    @Operation(
            summary = "판매자 스토어 페이지 조회",
            description = """
                    판매자의 스토어 페이지를 조회합니다.
                    
                    **파라미터 설명:**
                    - vendor-name: 판매자 상점명 (URL 경로)
                    - page: 페이지 번호 (1부터 시작, 기본값: 1) ← 수정됨
                    - size: 페이지 크기 (기본값: 12)
                    - sort: 정렬 기준 (기본값: createdAt,desc)
                    - category: 상품 카테고리 필터 (DOG, CAT - 선택사항)
                    
                    **정렬 옵션:**
                    - createdAt,desc: 최신순 (기본값)
                    - createdAt,asc: 오래된순  
                    - price,asc: 가격낮은순
                    - price,desc: 가격높은순
                    
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

            @Parameter(description = "상품 카테고리 필터", example = "DOG")
            @RequestParam(value = "category", required = false) PetCategory category) {

        try {
            log.info("판매자 스토어 페이지 조회 요청 - vendorName: {}, page: {}, size: {}, sort: {}, category: {}",
                    vendorName, page, size, sort, category);

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

            // 3. 1-based 페이지를 0-based로 변환
            int adjustedPage = page - 1;

            // 4. 정렬 조건 파싱 및 페이징 객체 생성
            Pageable pageable = createPageable(adjustedPage, size, sort);

            // 5. 서비스 호출
            SellerStorePageResponse response = sellerStoreService.getSellerStorePage(vendorName, category, pageable);

            // 6. 응답에서 페이지 번호를 다시 1-based로 조정
            response = adjustPageNumberInResponse(response, adjustedPage);

            log.info("판매자 스토어 페이지 조회 완료 - vendorName: {}, 요청페이지: {}, 조정페이지: {}, 상품수: {}",
                    vendorName, page, adjustedPage, response.products().content().size());

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SELLER_STORE_SUCCESS, response)
            );

        } catch (Exception e) {
            log.error("판매자 스토어 페이지 조회 중 오류 발생 - vendorName: {}", vendorName, e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 페이징 및 정렬 조건 생성
     */
    private Pageable createPageable(int page, int size, String sort) {
        // 페이지 크기 제한 (최대 50개)
        size = Math.min(size, 50);

        // 정렬 조건 파싱
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
        return PageRequest.of(page, size, sortObj); // 이미 0-based로 조정된 page 사용
    }

    /**
     * 응답 데이터의 페이지 번호를 사용자 친화적(1-based)으로 조정
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

        // 새로운 SellerStorePageResponse 반환
        return new SellerStorePageResponse(
                response.sellerInfo(),
                adjustedProducts
        );
    }
}