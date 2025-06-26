package com.team5.catdogeats.storage.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.storage.domain.dto.ReviewImageUploadResponseDto;
import com.team5.catdogeats.storage.service.ReviewImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/buyers/reviews/images")
@Tag(name = "ReviewImage", description = "리뷰 이미지 관련 API")
public class ReviewImageController {

    private final ReviewImageService reviewImageService;

    // 리뷰 이미지 등록 (S3 업로드 + Images DB 저장 + reviews_images 매핑)
    @Operation(
            summary = "리뷰 이미지 업로드",
            description = "여러 장의 이미지를 한 번에 업로드합니다."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ReviewImageUploadResponseDto>>> uploadReviewImage(
            @Parameter(description = "이미지를 업로드할 리뷰 id", required = true)
            @RequestParam String reviewId,
            @Parameter(description = "업로드할 이미지 파일 리스트", required = true)
            @RequestPart("images") List<MultipartFile> images) {
        try {
            List<ReviewImageUploadResponseDto> response = reviewImageService.uploadReviewImage(reviewId, images);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_TYPE_VALUE, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(
            summary = "리뷰 이미지 수정",
            description = "여러 장의 이미지를 한 번에 수정합니다."
    )
    @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ReviewImageUploadResponseDto>>> updateReviewImage(
            @Parameter(description = "이미지를 수정할 리뷰 id", required = true)
            @RequestParam String reviewId,
            @Parameter(description = "수정할 이미지 ids", required = true)
            @RequestParam List<String> oldImageIds,
            @Parameter(description = "새로 업로드할 이미지 파일 리스트", required = true)
            @RequestPart List<MultipartFile> images
    ) {
        try {
            List<ReviewImageUploadResponseDto> response = reviewImageService.updateReviewImage(reviewId, oldImageIds, images);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_TYPE_VALUE, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    // 리뷰 이미지 삭제 (매핑, 이미지 DB, S3 모두 정리)
    // 리뷰 삭제에서 리뷰 내용 + 이미지 일괄로 삭제하므로 사용x
//    @Operation(
//            summary = "리뷰 이미지만 삭제",
//            description = "imageId로 전달한 이미지 파일들을 삭제합니다." +
//                    "(리뷰 삭제 api를 통해 리뷰 내용+이미지 일괄 삭제하므로 보통 사용x)"
//    )
//    @DeleteMapping
//    public ResponseEntity<ApiResponse<Void>> deleteReviewImage(
//            @Parameter(description = "이미지를 삭제할 리뷰 id", required = true)
//            @RequestParam String reviewId,
//            @Parameter(description = "삭제할 이미지 파일 id", required = true)
//            @RequestParam String imageId) {
//        try{
//            reviewImageService.deleteReviewImage(reviewId, imageId);
//            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS));
//        } catch (NoSuchElementException e) {
//            return ResponseEntity
//                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
//                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
//        } catch (Exception e) {
//            return ResponseEntity
//                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
//                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
//        }
//    }
}
