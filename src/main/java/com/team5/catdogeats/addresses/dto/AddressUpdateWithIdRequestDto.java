package com.team5.catdogeats.addresses.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class AddressUpdateWithIdRequestDto {

    @NotNull(message = "주소 ID는 필수입니다.")
    private UUID addressId;

    @Size(max = 30, message = "주소 제목은 30자 이하여야 합니다.")
    private String title;

    @Size(max = 100, message = "시/도는 100자 이하여야 합니다.")
    private String city;

    @Size(max = 100, message = "시/군/구는 100자 이하여야 합니다.")
    private String district;

    @Size(max = 100, message = "읍/면/동은 100자 이하여야 합니다.")
    private String neighborhood;

    @Size(max = 200, message = "도로명 주소는 200자 이하여야 합니다.")
    private String streetAddress;

    @Size(max = 20, message = "우편번호는 20자 이하여야 합니다.")
    private String postalCode;

    @Size(max = 200, message = "상세 주소는 200자 이하여야 합니다.")
    private String detailAddress;

    @Size(max = 30, message = "전화번호는 30자 이하여야 합니다.")
    private String phoneNumber;

    private Boolean isDefault;

    // AddressUpdateRequestDto로 변환하는 메서드
    public AddressUpdateRequestDto toAddressUpdateRequestDto() {
        return AddressUpdateRequestDto.builder()
                .title(this.title)
                .city(this.city)
                .district(this.district)
                .neighborhood(this.neighborhood)
                .streetAddress(this.streetAddress)
                .postalCode(this.postalCode)
                .detailAddress(this.detailAddress)
                .phoneNumber(this.phoneNumber)
                .isDefault(this.isDefault)
                .build();
    }
}