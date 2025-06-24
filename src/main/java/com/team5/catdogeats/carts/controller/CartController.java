package com.team5.catdogeats.carts.controller;

import com.team5.catdogeats.carts.dto.request.AddCartItemRequest;
import com.team5.catdogeats.carts.dto.request.UpdateCartItemRequest;
import com.team5.catdogeats.carts.dto.response.CartResponse;
import com.team5.catdogeats.carts.service.CartService;
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
public class CartController {

    private final CartService cartService;

    // 장바구니 목록 조회
    @GetMapping("/{cartId}/list")
    public ResponseEntity<CartResponse> getCartItems(
            @PathVariable String cartId,
            Authentication authentication) {

        String userId = authentication.getName(); // 인증된 사용자 ID
        CartResponse cartResponse = cartService.getCartByUserId(userId);

        return ResponseEntity.ok(cartResponse);
    }

    // 장바구니에 상품 추가
    @PostMapping
    public ResponseEntity<CartResponse> addCartItem(
            @Valid @RequestBody AddCartItemRequest request,
            Authentication authentication) {

        String userId = authentication.getName();
        CartResponse cartResponse = cartService.addItemToCart(userId, request);

        return ResponseEntity.ok(cartResponse);
    }

    // 장바구니 아이템 수량 수정
    @PatchMapping("/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable String cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {

        String userId = authentication.getName();
        CartResponse cartResponse = cartService.updateCartItem(userId, cartItemId, request);

        return ResponseEntity.ok(cartResponse);
    }

    // 장바구니에서 상품 삭제
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> removeCartItem(
            @PathVariable String cartItemId,
            Authentication authentication) {

        String userId = authentication.getName();
        cartService.removeCartItem(userId, cartItemId);

        return ResponseEntity.noContent().build();
    }

    // 장바구니 전체 비우기
    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        String userId = authentication.getName();
        cartService.clearCart(userId);

        return ResponseEntity.noContent().build();
    }
}