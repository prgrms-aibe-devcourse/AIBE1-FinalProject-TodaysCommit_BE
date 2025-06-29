package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.dto.PageResponseDto;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.products.domain.dto.MyProductResponseDto;
import com.team5.catdogeats.products.domain.enums.SellerProductSortType;
import com.team5.catdogeats.reviews.domain.dto.ProductReviewResponseDto;
import com.team5.catdogeats.reviews.domain.dto.SellerReviewSummaryResponseDto;
import com.team5.catdogeats.reviews.service.ReviewService;
import com.team5.catdogeats.users.service.SellerRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sellers/products")
@Tag(name = "SellerRating", description = "판매자 평점 관련 API")
public class SellerRatingController {

    private final SellerRatingService sellerRatingService;
    private final ReviewService reviewService;

    @Operation(
            summary = "판매자 상품 목록 조회",
            description = "판매자가 등록한 상품 목록을 조회합니다. 상품명, 대표 이미지, 리뷰 개수, 평균 별점 정보를 제공합니다. with 정렬 기능"
    )
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResponseDto<MyProductResponseDto>>> getMyProductsBySeller(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 방식: LATEST(기본), STAR(평점순), REVIEW(리뷰갯수순)")
            @RequestParam(defaultValue = "LATEST") SellerProductSortType sortType
    ) {
        try {
            Page<MyProductResponseDto> data = sellerRatingService.getProductsBySeller(userPrincipal, page, size, sortType);

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, PageResponseDto.from(data)));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    // 리뷰 domain의 특정 상품에 대한 리뷰 조회 controller 재활용
    @Operation(summary = "특정 상품에 대한 리뷰 목록 조회",
            description = "리뷰 작성자, 작성자의 펫 정보, 별점, 내용, 등록/수정 날짜, 리뷰에 첨부된 이미지들 제공합니다.")
    @GetMapping("/{productNumber}/list")
    public ResponseEntity<ApiResponse<PageResponseDto<ProductReviewResponseDto>>> getReviewsByProduct(
            @Parameter(description = "조회할 상품 Number", required = true)
            @PathVariable Long productNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<ProductReviewResponseDto> reviews = reviewService.getReviewsByProductNumber(productNumber, page, size);

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, PageResponseDto.from(reviews)));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(summary = "특정 판매자의 전체 상품의 리뷰 통계 조회", description = "판매자가 등록한 모든 상품의 리뷰들 개수, 평균 별점, 별점구간별로 집계")
    @GetMapping("/reviewSummary")
    public ResponseEntity<ApiResponse<SellerReviewSummaryResponseDto>> getSellerReviewSummary(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            SellerReviewSummaryResponseDto summary = sellerRatingService.getSellerReviewSummary(userPrincipal);

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, summary));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }
}
