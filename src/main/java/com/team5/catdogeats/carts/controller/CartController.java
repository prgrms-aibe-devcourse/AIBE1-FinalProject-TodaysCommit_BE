package com.team5.catdogeats.carts.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.carts.dto.request.AddCartItemRequest;
import com.team5.catdogeats.carts.dto.request.UpdateCartItemRequest;
import com.team5.catdogeats.carts.dto.response.CartResponse;
import com.team5.catdogeats.carts.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/buyers/carts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "장바구니", description = "장바구니 관리 API")
@SecurityRequirement(name = "Bearer Authentication")
public class CartController {

    private final CartService cartService;

    @Operation(
            summary = "장바구니 조회",
            description = "사용자의 장바구니 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "장바구니를 찾을 수 없음")
    })

    // 장바구니 목록 조회
    @GetMapping("/{cartId}/list")
    public ResponseEntity<CartResponse> getCartItems(
            @PathVariable String cartId,
            Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        CartResponse cartResponse = cartService.getCartByUserPrincipal(userPrincipal);

        return ResponseEntity.ok(cartResponse);
    }

    // 장바구니에 상품 추가
    @Operation(
            summary = "장바구니에 상품 추가",
            description = "장바구니에 새로운 상품을 추가하거나 기존 상품의 수량을 증가시킵니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<CartResponse> addCartItem(
            @Valid @RequestBody AddCartItemRequest request,
            Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        CartResponse cartResponse = cartService.addItemToCart(userPrincipal, request);

        return ResponseEntity.ok(cartResponse);
    }

    // 장바구니 아이템 수량 수정
    @Operation(
            summary = "장바구니 아이템 수량 수정",
            description = "장바구니에 있는 특정 상품의 수량을 수정합니다."
    )
    @PatchMapping("/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable String cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        CartResponse cartResponse = cartService.updateCartItem(userPrincipal, cartItemId, request);

        return ResponseEntity.ok(cartResponse);
    }

    // 장바구니에서 상품 삭제
    @Operation(
            summary = "장바구니 아이템 삭제",
            description = "장바구니에서 특정 상품을 삭제합니다."
    )
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> removeCartItem(
            @PathVariable String cartItemId,
            Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        cartService.removeCartItem(userPrincipal, cartItemId);

        return ResponseEntity.noContent().build();
    }

    // 장바구니 전체 비우기
    @Operation(
            summary = "장바구니 비우기",
            description = "장바구니의 모든 상품을 삭제합니다."
    )
    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        cartService.clearCart(userPrincipal);

        return ResponseEntity.noContent().build();
    }
}