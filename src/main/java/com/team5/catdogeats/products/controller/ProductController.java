package com.team5.catdogeats.products.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.pets.domain.dto.PetDeleteRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductCreateRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductDeleteRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductUpdateRequestDto;
import com.team5.catdogeats.products.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
@Tag(name = "Product", description = "상품 정보 관련 API")
public class ProductController {
    private final ProductService productService;

    @Operation(
            summary = "상품 등록",
            description = "판매자가 새로운 상품을 등록합니다. 등록 성공 시 생성된 상품 ID를 Location 헤더로 반환합니다."
    )
    @PostMapping("/sellers/products")
    public ResponseEntity<ApiResponse<Void>> registerProduct(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody @Valid @Parameter(description = "등록할 상품 정보", required = true)ProductCreateRequestDto dto) {
        try {
            UUID productId = productService.registerProduct(userPrincipal, dto);
            return ResponseEntity
                    .created(URI.create("/v1/sellers/products/" + productId))
                    .body(ApiResponse.success(ResponseCode.CREATED));
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

    @Operation(
            summary = "상품 수정",
            description = "판매자가 기존 상품 정보를 수정합니다. 수정할 상품 ID와 내용을 요청 바디로 전달합니다."
    )
    @PatchMapping("/sellers/products")
    public ResponseEntity<ApiResponse<Void>> updateProduct(@RequestBody @Valid @Parameter(description = "수정할 상품 정보", required = true) ProductUpdateRequestDto dto) {
        try {
            productService.updateProduct(dto);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS));
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

    @Operation(
            summary = "상품 삭제",
            description = "판매자가 특정 상품을 삭제합니다. 삭제할 상품의 ID를 요청 바디로 전달합니다."
    )
    @DeleteMapping("/sellers/pet")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@RequestBody @Valid @Parameter(description = "삭제할 상품 id", required = true) ProductDeleteRequestDto dto) {
        try {
            productService.deleteProduct(dto);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS));
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

    // TODO: 상품 조회 서비스 로직 / 상품 상세 조회 컨트롤러 구현하기
}
