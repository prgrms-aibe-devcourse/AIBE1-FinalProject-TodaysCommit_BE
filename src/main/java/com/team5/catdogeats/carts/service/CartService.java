package com.team5.catdogeats.carts.service;

import com.team5.catdogeats.carts.dto.request.AddCartItemRequest;
import com.team5.catdogeats.carts.dto.request.UpdateCartItemRequest;
import com.team5.catdogeats.carts.dto.response.CartResponse;

public interface CartService {

    CartResponse getCartByUserId(String userId);

    CartResponse addItemToCart(String userId, AddCartItemRequest request);

    CartResponse updateCartItem(String userId, String cartItemId, UpdateCartItemRequest request);

    CartResponse removeCartItem(String userId, String cartItemId);

    void clearCart(String userId);
}