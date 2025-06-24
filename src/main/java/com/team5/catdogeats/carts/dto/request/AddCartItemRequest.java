package com.team5.catdogeats.carts.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCartItemRequest {

    @NotBlank(message = "상품 ID는 필수입니다.")
    private String productId;

    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private int quantity;
}
