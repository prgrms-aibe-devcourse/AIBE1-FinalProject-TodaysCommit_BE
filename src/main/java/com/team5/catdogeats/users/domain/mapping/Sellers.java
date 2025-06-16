package com.team5.catdogeats.users.domain.mapping;

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
public class Sellers extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", length = 36)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_sellers_user_id"))
    private Users user;

    @Column(name = "vendor_name", length = 100)
    private String vendorName;

    @Column(name = "vendor_profile_image", length = 255)
    private String vendorProfileImage;

    @Column(name = "business_number", length = 20)
    private String businessNumber;

    @Column(name = "settlement_bank", length = 50)
    private String settlementBank;

    @Column(name = "settlement_acc", length = 30)
    private String settlementAccount;

    @Column(name = "tags")
    private String tags;
}
