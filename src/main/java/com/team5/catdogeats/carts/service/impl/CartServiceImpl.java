package com.team5.catdogeats.carts.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.carts.domain.Carts;
import com.team5.catdogeats.carts.domain.mapping.CartItems;
import com.team5.catdogeats.carts.dto.request.AddCartItemRequest;
import com.team5.catdogeats.carts.dto.request.UpdateCartItemRequest;
import com.team5.catdogeats.carts.dto.response.CartItemResponse;
import com.team5.catdogeats.carts.dto.response.CartResponse;
import com.team5.catdogeats.carts.repository.CartItemRepository;
import com.team5.catdogeats.carts.repository.CartRepository;
import com.team5.catdogeats.carts.service.CartService;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    public CartResponse getCartByUserPrincipal(UserPrincipal userPrincipal) {
        Users user = getUserByPrincipal(userPrincipal);
        Carts cart = getOrCreateCart(user.getId());
        List<CartItems> cartItems = cartItemRepository.findByCartsIdWithProduct(cart.getId());

        return buildCartResponse(cart, cartItems);
    }

    @Override
    @Transactional
    public CartResponse addItemToCart(UserPrincipal userPrincipal, AddCartItemRequest request) {
        Users user = getUserByPrincipal(userPrincipal);
        Carts cart = getOrCreateCart(user.getId());
        Products product = getProductById(request.getProductId());

        // 기존에 같은 상품이 있는지 확인
        CartItems existingItem = cartItemRepository
                .findByCartsIdAndProductId(cart.getId(), request.getProductId())
                .orElse(null);

        if (existingItem != null) {
            // 기존 상품이 있으면 수량 증가
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            cartItemRepository.save(existingItem);
        } else {
            // 새로운 상품 추가
            CartItems newItem = CartItems.builder()
                    .carts(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        return getCartByUserPrincipal(userPrincipal);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(UserPrincipal userPrincipal, String cartItemId, UpdateCartItemRequest request) {
        Users user = getUserByPrincipal(userPrincipal);
        Carts cart = getOrCreateCart(user.getId());
        CartItems cartItem = getCartItemById(cartItemId);

        // 권한 확인
        if (!cartItem.getCarts().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("해당 장바구니 아이템에 접근 권한이 없습니다.");
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        return getCartByUserPrincipal(userPrincipal);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(UserPrincipal userPrincipal, String cartItemId) {
        Users user = getUserByPrincipal(userPrincipal);
        Carts cart = getOrCreateCart(user.getId());
        CartItems cartItem = getCartItemById(cartItemId);

        // 권한 확인
        if (!cartItem.getCarts().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("해당 장바구니 아이템에 접근 권한이 없습니다.");
        }

        cartItemRepository.delete(cartItem);

        return getCartByUserPrincipal(userPrincipal);
    }

    @Override
    @Transactional
    public void clearCart(UserPrincipal userPrincipal) {
        Users user = getUserByPrincipal(userPrincipal);
        Carts cart = getOrCreateCart(user.getId());
        List<CartItems> cartItems = cartItemRepository.findByCartsId(cart.getId());
        cartItemRepository.deleteAll(cartItems);
    }

    // === Private Helper Methods ===

    private Carts getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCart(userId));
    }

    private Carts createNewCart(String userId) {
        Users user = getUserById(userId);
        Carts newCart = Carts.builder()
                .user(user)
                .build();
        return cartRepository.save(newCart);
    }

    // UserPrincipal로 사용자 조회
    private Users getUserByPrincipal(UserPrincipal userPrincipal) {
        return userRepository.findByProviderAndProviderId(
                        userPrincipal.provider(),
                        userPrincipal.providerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다: " + userPrincipal.provider() + "/" + userPrincipal.providerId()));
    }

    private Users getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    private Products getProductById(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
    }

    private CartItems getCartItemById(String cartItemId) {
        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 아이템을 찾을 수 없습니다: " + cartItemId));
    }

    private CartResponse buildCartResponse(Carts cart, List<CartItems> cartItems) {
        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(this::convertToCartItemResponse)
                .collect(Collectors.toList());

        Long totalAmount = itemResponses.stream()
                .mapToLong(CartItemResponse::getTotalPrice)
                .sum();

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemResponses)
                .totalAmount(totalAmount)
                .totalItemCount(itemResponses.size())
                .build();
    }

    private CartItemResponse convertToCartItemResponse(CartItems cartItem) {
        Products product = cartItem.getProduct();
        Long totalPrice = product.getPrice() * cartItem.getQuantity();

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(product.getId())
                .productName(product.getTitle()) // Products 엔티티의 실제 필드명 사용
                .productImage("") // 이미지는 나중에 S3 로직 완성 후 추가
                .productPrice(product.getPrice())
                .quantity(cartItem.getQuantity())
                .totalPrice(totalPrice)
                .addedAt(cartItem.getAddedAt())
                .build();
    }
}