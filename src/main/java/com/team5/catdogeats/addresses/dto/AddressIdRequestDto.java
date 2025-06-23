package com.team5.catdogeats.addresses.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class AddressIdRequestDto {

    @NotNull(message = "주소 ID는 필수입니다.")
    private UUID addressId;
}