package com.team5.catdogeats.carts.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CartResponse {
    private String cartId;
    private String userId;
    private List<CartItemResponse> items;
    private Long totalAmount;
    private int totalItemCount;
}
