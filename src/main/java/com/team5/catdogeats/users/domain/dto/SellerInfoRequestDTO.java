package com.team5.catdogeats.users.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

@Schema(description = "판매자 정보 등록/수정 요청 DTO")
public record SellerInfoRequestDTO(

        @Schema(description = "업체명", example = "멍멍이네 수제간식", required = true)
        @Size(max = 100, message = "업체명은 최대 100자까지 입력 가능합니다.")
        String vendorName,


        @Schema(description = "사업자 등록번호", example = "123-45-67890", required = true)
        @Size(max = 20, message = "사업자 등록번호는 최대 20자까지 입력 가능합니다.")
        @Pattern(regexp = "^[0-9\\-]+$", message = "사업자 등록번호는 숫자와 하이픈만 입력 가능합니다.")
        String businessNumber,


        @Schema(description = "정산 은행명", example = "국민은행")
        @Size(max = 50, message = "은행명은 최대 50자까지 입력 가능합니다.")
        String settlementBank,

        @Schema(description = "정산 계좌번호", example = "123456789012")
        @Size(max = 30, message = "계좌번호는 최대 30자까지 입력 가능합니다.")
        String settlementAcc,

        @Schema(description = "태그 (쉼표로 구분)", example = "수제간식,강아지")
        @Size(max = 36, message = "태그는 최대 36자까지 입력 가능합니다.")
        String tags,

        @Schema(description = "운영 시작 시간", example = "09:00:00")
        LocalTime operatingStartTime,

        @Schema(description = "운영 종료 시간", example = "18:00:00")
        LocalTime operatingEndTime,

        @Schema(description = "휴무일 (쉼표로 구분)", example = "월요일,화요일")
        @Size(max = 20, message = "휴무일은 최대 20자까지 입력 가능합니다.")
        String closedDays

) {
        public boolean isCreateRequest() {
                return vendorName != null && !vendorName.trim().isEmpty() && businessNumber != null && !businessNumber.trim().isEmpty();
        }
}