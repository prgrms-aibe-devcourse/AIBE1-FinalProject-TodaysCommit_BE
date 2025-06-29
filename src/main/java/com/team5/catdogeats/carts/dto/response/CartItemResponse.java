package com.team5.catdogeats.carts.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@Builder
public class CartItemResponse {
    private String id;
    private String productId;
    private String productName;
    private String productImage;
    private Long productPrice;
    private int quantity;
    private Long totalPrice; // price * quantity
    private ZonedDateTime addedAt;
}
