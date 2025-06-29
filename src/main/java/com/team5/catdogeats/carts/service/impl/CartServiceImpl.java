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
import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@JpaTransactional(readOnly = true)
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

        log.debug("장바구니 조회 - userId: {}, 상품 수: {}", user.getId(), cartItems.size());
        return buildCartResponse(cart, cartItems);
    }

    @Override
    @JpaTransactional
    public CartResponse addItemToCart(UserPrincipal userPrincipal, AddCartItemRequest request) {
        // 입력 값 검증
        if (request.getQuantity() <= 0) {
            log.warn("잘못된 수량으로 장바구니 추가 시도 - quantity: {}", request.getQuantity());
            throw new IllegalArgumentException("상품 수량은 1개 이상이어야 합니다.");
        }

        Users user = getUserByPrincipal(userPrincipal);
        Carts cart = getOrCreateCart(user.getId());
        Products product = getProductById(request.getProductId());

        // 기존에 같은 상품이 있는지 확인
        CartItems existingItem = cartItemRepository
                .findByCartsIdAndProductId(cart.getId(), request.getProductId())
                .orElse(null);

        if (existingItem != null) {
            // 기존 상품이 있으면 수량 증가
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
            log.info("장바구니 기존 상품 수량 증가 - productId: {}, 기존: {}, 추가: {}, 총합: {}",
                    request.getProductId(), existingItem.getQuantity() - request.getQuantity(),
                    request.getQuantity(), newQuantity);
        } else {
            // 새로운 상품 추가
            CartItems newItem = CartItems.builder()
                    .carts(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
            log.info("장바구니 새 상품 추가 - productId: {}, quantity: {}",
                    request.getProductId(), request.getQuantity());
        }

        return getCartByUserPrincipal(userPrincipal);
    }

    @Override
    @JpaTransactional
    public CartResponse updateCartItem(UserPrincipal userPrincipal, String cartItemId, UpdateCartItemRequest request) {
        // 입력 값 검증
        if (request.getQuantity() <= 0) {
            log.warn("잘못된 수량으로 장바구니 수정 시도 - cartItemId: {}, quantity: {}",
                    cartItemId, request.getQuantity());
            throw new IllegalArgumentException("상품 수량은 1개 이상이어야 합니다.");
        }

        Users user = getUserByPrincipal(userPrincipal);

        // 권한확인 + 조회
        CartItems cartItem = cartItemRepository.findByIdAndUserId(cartItemId, user.getId())
                .orElseThrow(() -> {
                    log.warn("장바구니 아이템 접근 권한 없음 또는 존재하지 않음 - userId: {}, cartItemId: {}",
                            user.getId(), cartItemId);
                    return new SecurityException("해당 장바구니 아이템에 접근 권한이 없거나 존재하지 않습니다.");
                });

        int oldQuantity = cartItem.getQuantity();
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        log.info("장바구니 상품 수량 수정 - cartItemId: {}, 이전: {}, 변경: {}",
                cartItemId, oldQuantity, request.getQuantity());

        return getCartByUserPrincipal(userPrincipal);
    }

    @Override
    @JpaTransactional
    public CartResponse removeCartItem(UserPrincipal userPrincipal, String cartItemId) {
        Users user = getUserByPrincipal(userPrincipal);

        // 권한 확인 + 조회
        CartItems cartItem = cartItemRepository.findByIdAndUserId(cartItemId, user.getId())
                .orElseThrow(() -> {
                    log.warn("장바구니 아이템 삭제 권한 없음 또는 존재하지 않음 - userId: {}, cartItemId: {}",
                            user.getId(), cartItemId);
                    return new SecurityException("해당 장바구니 아이템에 접근 권한이 없거나 존재하지 않습니다.");
                });

        String productName = cartItem.getProduct().getTitle();
        cartItemRepository.delete(cartItem);

        log.info("장바구니 상품 삭제 - cartItemId: {}, productName: {}", cartItemId, productName);

        return getCartByUserPrincipal(userPrincipal);
    }

    @Override
    @JpaTransactional
    public void clearCart(UserPrincipal userPrincipal) {
        Users user = getUserByPrincipal(userPrincipal);
        Carts cart = getOrCreateCart(user.getId());
        List<CartItems> cartItems = cartItemRepository.findByCartsId(cart.getId());

        int deletedCount = cartItems.size();
        cartItemRepository.deleteAll(cartItems);

        log.info("장바구니 전체 비우기 - userId: {}, 삭제된 상품 수: {}", user.getId(), deletedCount);
    }

    // === Private Helper Methods ===

    private Carts getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("새 장바구니 생성 - userId: {}", userId);
                    return createNewCart(userId);
                });
    }

    private Carts createNewCart(String userId) {
        Users user = getUserById(userId);
        Carts newCart = Carts.builder()
                .user(user)
                .build();
        return cartRepository.save(newCart);
    }

    // 사용자 조회
    private Users getUserByPrincipal(UserPrincipal userPrincipal) {
        return userRepository.findByProviderAndProviderId(
                        userPrincipal.provider(),
                        userPrincipal.providerId())
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없음 - provider: {}, providerId: {}",
                            userPrincipal.provider(), userPrincipal.providerId());
                    return new NoSuchElementException("사용자를 찾을 수 없습니다.");
                });
    }

    private Users getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자 ID로 조회 실패 - userId: {}", userId);
                    return new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId);
                });
    }

    private Products getProductById(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("상품을 찾을 수 없음 - productId: {}", productId);
                    return new NoSuchElementException("상품을 찾을 수 없습니다: " + productId);
                });
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