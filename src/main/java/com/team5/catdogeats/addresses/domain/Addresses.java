package com.team5.catdogeats.addresses.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.users.domain.Users;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;


@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Addresses extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private UUID id;

    @Column(name = "title", length = 30, nullable = false)
    private String title;

    //시/도
    @Column(name = "city", length = 100, nullable = false)
    private String city;

    //시/군/구
    @Column(name = "district", length = 100, nullable = false)
    private String district;

    //읍/면/동
    @Column(name = "neighborhood", length = 100, nullable = false)
    private String neighborhood;

    //도로명 주소
    @Column(name = "street_address", length = 200, nullable = false)
    private String streetAddress;

    //우편번호
    @Column(name = "postal_code", length = 20, nullable = false)
    private String postalCode;

    //상세 주소 (빌딩명, 호수 등)
    @Column(name = "detail_address", length = 200, nullable = false)
    private String detailAddress;

    @Column(name = "phone_number", length = 30, nullable = false)
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_addresses_user_id"))
    private Users user;
}