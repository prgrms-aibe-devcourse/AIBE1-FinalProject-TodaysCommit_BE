package com.team5.catdogeats.carts.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.carts.dto.request.AddCartItemRequest;
import com.team5.catdogeats.carts.dto.request.UpdateCartItemRequest;
import com.team5.catdogeats.carts.dto.response.CartResponse;

public interface CartService {

    CartResponse getCartByUserPrincipal(UserPrincipal userPrincipal);

    CartResponse addItemToCart(UserPrincipal userPrincipal, AddCartItemRequest request);

    CartResponse updateCartItem(UserPrincipal userPrincipal, String cartItemId, UpdateCartItemRequest request);

    CartResponse removeCartItem(UserPrincipal userPrincipal, String cartItemId);

    void clearCart(UserPrincipal userPrincipal);
}