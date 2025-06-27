package com.team5.catdogeats.pets.domain.dto;

import com.team5.catdogeats.pets.domain.enums.Gender;

public record PetInfoResponseDto(
        String breed,
        Short age,
        Gender gender
) {
}
