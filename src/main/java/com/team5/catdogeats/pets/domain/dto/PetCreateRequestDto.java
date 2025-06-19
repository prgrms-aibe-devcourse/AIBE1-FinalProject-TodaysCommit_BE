package com.team5.catdogeats.pets.domain.dto;

import com.team5.catdogeats.pets.domain.enums.Gender;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PetCreateRequestDto(
        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotNull(message = "카테고리는 필수입니다.")
        PetCategory petCategory,

        @NotNull(message = "성별은 필수입니다.")
        Gender gender,

        @NotBlank(message = "품종은 필수입니다.")
        String breed,

        @NotNull(message = "나이는 필수입니다.")
        @Min(value = 0, message = "나이는 0 이상이어야 합니다.")
        Short age
) {
}
