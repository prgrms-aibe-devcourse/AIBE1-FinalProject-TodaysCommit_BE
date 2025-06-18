package com.team5.catdogeats.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.ZonedDateTime;

public record SellerInfoRequest(
        @NotBlank(message = "업체명은 필수 입력 항목입니다.")
        @Size(max = 100, message = "업체명은 최대 100자까지 입력 가능합니다.")
        String vendorName,

        @NotBlank(message = "업체 프로필 이미지는 필수 입력 항목입니다.")
        @Size(max = 255, message = "이미지 URL은 최대 255자까지 입력 가능합니다.")
        String vendorProfileImage,

        @NotBlank(message = "사업자 등록번호는 필수 입력 항목입니다.")
        @Size(max = 20, message = "사업자 등록번호는 최대 20자까지 입력 가능합니다.")
        @Pattern(regexp = "^[0-9\\-]+$", message = "사업자 등록번호는 숫자와 하이픈만 입력 가능합니다.")
        String businessNumber,

        @Size(max = 50, message = "은행명은 최대 50자까지 입력 가능합니다.")
        String settlementBank,

        @Size(max = 30, message = "계좌번호는 최대 30자까지 입력 가능합니다.")
        String settlementAcc,

        @Size(max = 36, message = "태그는 최대 36자까지 입력 가능합니다.")
        String tags,

        ZonedDateTime operatingStartTime,

        ZonedDateTime operatingEndTime,

        @Size(max = 20, message = "휴무일은 최대 20자까지 입력 가능합니다.")
        String closedDays
) {}