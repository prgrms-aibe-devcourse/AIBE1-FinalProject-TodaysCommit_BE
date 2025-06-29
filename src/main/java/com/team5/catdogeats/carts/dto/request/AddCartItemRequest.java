package com.team5.catdogeats.carts.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "장바구니 상품 추가 요청")
public class AddCartItemRequest {

    @Schema(description = "상품 ID", example = "prod-123", required = true)
    @NotBlank(message = "상품 ID는 필수입니다.")
    private String productId;

    @Schema(description = "수량", example = "2", minimum = "1")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private int quantity;
}
