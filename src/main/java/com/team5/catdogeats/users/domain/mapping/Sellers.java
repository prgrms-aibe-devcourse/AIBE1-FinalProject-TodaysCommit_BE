package com.team5.catdogeats.users.domain.mapping;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.users.domain.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Sellers extends BaseEntity {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_sellers_user_id"), nullable = false)
    private Users user;

    @Column(name = "vendor_name", length = 100, unique = true)
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

    @Column(name = "operating_start_time")
    private LocalTime operatingStartTime;

    @Column(name = "operating_end_time")
    private LocalTime operatingEndTime;

    @Column(name = "closed_days", length = 20)
    private String closedDays;

    @Column(nullable = false, name = "is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deledAt;


    public void updateVendorName(String vendorName) {
        if (vendorName != null && !vendorName.trim().isEmpty()) {
            this.vendorName = vendorName;
        }
    }

    public void updateVendorProfileImage(String vendorProfileImage) {
        if (vendorProfileImage != null && !vendorProfileImage.trim().isEmpty()) {
            this.vendorProfileImage = vendorProfileImage;
        }
    }

    public void updateBusinessNumber(String businessNumber) {
        if (businessNumber != null && !businessNumber.trim().isEmpty()) {
            this.businessNumber = businessNumber;
        }
    }

    public void updateSettlementBank(String settlementBank) {
        this.settlementBank = settlementBank;
    }

    public void updateSettlementAcc(String settlementAcc) {
        this.settlementAccount = settlementAcc;
    }

    public void updateTags(String tags) {
        this.tags = tags;
    }

    public void updateOperatingStartTime(LocalTime operatingStartTime) {
        this.operatingStartTime = operatingStartTime;
    }

    public void updateOperatingEndTime(LocalTime operatingEndTime) {
        this.operatingEndTime = operatingEndTime;
    }

    public void updateClosedDays(String closedDays) {
        this.closedDays = closedDays;
    }
}

