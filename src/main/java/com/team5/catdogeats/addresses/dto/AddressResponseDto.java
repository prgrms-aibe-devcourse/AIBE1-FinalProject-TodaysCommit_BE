package com.team5.catdogeats.addresses.dto;

import com.team5.catdogeats.addresses.domain.Addresses;
import com.team5.catdogeats.addresses.domain.enums.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponseDto {

    private UUID id;
    private String title;
    private String city;
    private String district;
    private String neighborhood;
    private String streetAddress;
    private String postalCode;
    private String detailAddress;
    private String phoneNumber;
    private AddressType addressType;
    private boolean isDefault;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public static AddressResponseDto from(Addresses address) {
        return AddressResponseDto.builder()
                .id(address.getId())
                .title(address.getTitle())
                .city(address.getCity())
                .district(address.getDistrict())
                .neighborhood(address.getNeighborhood())
                .streetAddress(address.getStreetAddress())
                .postalCode(address.getPostalCode())
                .detailAddress(address.getDetailAddress())
                .phoneNumber(address.getPhoneNumber())
                .addressType(address.getAddressType())
                .isDefault(address.isDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    public String getFullAddress() {
        return String.format("%s %s %s %s %s",
                city, district, neighborhood, streetAddress, detailAddress);
    }

    public String getFullAddressWithPostalCode() {
        return String.format("(%s) %s", postalCode, getFullAddress());
    }
}