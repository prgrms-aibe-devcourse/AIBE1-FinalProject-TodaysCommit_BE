package com.team5.catdogeats.addresses.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressUpdateRequestDto {

    @Size(max = 30, message = "주소 제목은 30자 이하여야 합니다")
    private String title;

    @Size(max = 100, message = "시/도는 100자 이하여야 합니다")
    private String city;

    @Size(max = 100, message = "시/군/구는 100자 이하여야 합니다")
    private String district;

    @Size(max = 100, message = "읍/면/동은 100자 이하여야 합니다")
    private String neighborhood;

    @Size(max = 200, message = "도로명 주소는 200자 이하여야 합니다")
    private String streetAddress;

    @Size(max = 20, message = "우편번호는 20자 이하여야 합니다")
    private String postalCode;

    @Size(max = 200, message = "상세 주소는 200자 이하여야 합니다")
    private String detailAddress;

    @Size(max = 30, message = "전화번호는 30자 이하여야 합니다")
    private String phoneNumber;

    private Boolean isDefault;

    // null이 아닌 필드만 업데이트하기 위한 헬퍼 메서드들
    public boolean hasTitle() {
        return title != null && !title.trim().isEmpty();
    }

    public boolean hasCity() {
        return city != null && !city.trim().isEmpty();
    }

    public boolean hasDistrict() {
        return district != null && !district.trim().isEmpty();
    }

    public boolean hasNeighborhood() {
        return neighborhood != null && !neighborhood.trim().isEmpty();
    }

    public boolean hasStreetAddress() {
        return streetAddress != null && !streetAddress.trim().isEmpty();
    }

    public boolean hasPostalCode() {
        return postalCode != null && !postalCode.trim().isEmpty();
    }

    public boolean hasDetailAddress() {
        return detailAddress != null && !detailAddress.trim().isEmpty();
    }

    public boolean hasPhoneNumber() {
        return phoneNumber != null && !phoneNumber.trim().isEmpty();
    }

    public boolean hasIsDefault() {
        return isDefault != null;
    }
}