package com.team5.catdogeats.carts.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCartItemRequest {

    @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
    private int quantity;
}