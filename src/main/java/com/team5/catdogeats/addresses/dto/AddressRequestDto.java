package com.team5.catdogeats.addresses.dto;

import com.team5.catdogeats.addresses.domain.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequestDto {

    @NotBlank(message = "주소 제목은 필수입니다")
    @Size(max = 30, message = "주소 제목은 30자 이하여야 합니다")
    private String title;

    @NotBlank(message = "시/도는 필수입니다")
    @Size(max = 100, message = "시/도는 100자 이하여야 합니다")
    private String city;

    @NotBlank(message = "시/군/구는 필수입니다")
    @Size(max = 100, message = "시/군/구는 100자 이하여야 합니다")
    private String district;

    @NotBlank(message = "읍/면/동은 필수입니다")
    @Size(max = 100, message = "읍/면/동은 100자 이하여야 합니다")
    private String neighborhood;

    @NotBlank(message = "도로명 주소는 필수입니다")
    @Size(max = 200, message = "도로명 주소는 200자 이하여야 합니다")
    private String streetAddress;

    @NotBlank(message = "우편번호는 필수입니다")
    @Size(max = 20, message = "우편번호는 20자 이하여야 합니다")
    private String postalCode;

    @NotBlank(message = "상세 주소는 필수입니다")
    @Size(max = 200, message = "상세 주소는 200자 이하여야 합니다")
    private String detailAddress;

    @NotBlank(message = "전화번호는 필수입니다")
    @Size(max = 30, message = "전화번호는 30자 이하여야 합니다")
    private String phoneNumber;

    @NotNull(message = "주소 타입은 필수입니다")
    private AddressType addressType;

    private boolean isDefault = false;
}