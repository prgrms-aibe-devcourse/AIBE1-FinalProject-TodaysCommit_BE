package com.team5.catdogeats.addresses.domain;

import com.team5.catdogeats.addresses.domain.enums.AddressType;
import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.users.domain.Users;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Addresses extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "title", length = 30, nullable = false)
    private String title;

    // 시/도
    @Column(name = "city", length = 100, nullable = false)
    private String city;

    // 시/군/구
    @Column(name = "district", length = 100, nullable = false)
    private String district;

    // 읍/면/동
    @Column(name = "neighborhood", length = 100, nullable = false)
    private String neighborhood;

    // 도로명 주소
    @Column(name = "street_address", length = 200, nullable = false)
    private String streetAddress;

    // 우편번호
    @Column(name = "postal_code", length = 20, nullable = false)
    private String postalCode;

    // 상세 주소 (빌딩명, 호수 등)
    @Column(name = "detail_address", length = 200, nullable = false)
    private String detailAddress;

    @Column(name = "phone_number", length = 30, nullable = false)
    private String phoneNumber;

    // 주소 타입 (개인/사업자)
    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false)
    private AddressType addressType;

    // 기본 주소 여부
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_addresses_user_id"))
    private Users user;

    // 비즈니스 메서드

    // 주소 정보 업데이트
    public void updateAddress(String title, String city, String district, String neighborhood,
                              String streetAddress, String postalCode, String detailAddress, String phoneNumber) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title.trim();
        }
        if (city != null && !city.trim().isEmpty()) {
            this.city = city.trim();
        }
        if (district != null && !district.trim().isEmpty()) {
            this.district = district.trim();
        }
        if (neighborhood != null && !neighborhood.trim().isEmpty()) {
            this.neighborhood = neighborhood.trim();
        }
        if (streetAddress != null && !streetAddress.trim().isEmpty()) {
            this.streetAddress = streetAddress.trim();
        }
        if (postalCode != null && !postalCode.trim().isEmpty()) {
            this.postalCode = postalCode.trim();
        }
        if (detailAddress != null && !detailAddress.trim().isEmpty()) {
            this.detailAddress = detailAddress.trim();
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            this.phoneNumber = phoneNumber.trim();
        }
    }

    // 기본주소로 설정
    public void setAsDefault() {
        this.isDefault = true;
    }

    // 기본주소 해제
    public void removeDefault() {
        this.isDefault = false;
    }

    // 해당 사용자의 주소인지 확인
    public boolean isOwnedBy(String userId) {
        return this.user != null && this.user.getId().equals(userId);
    }

    // 전체 주소 문자열 반환
    public String getFullAddress() {
        return String.format("%s %s %s %s %s",
                city, district, neighborhood, streetAddress, detailAddress);
    }

    // 우편번호 포함 전체 주소 문자열 반환
    public String getFullAddressWithPostalCode() {
        return String.format("(%s) %s", postalCode, getFullAddress());
    }
}