package com.team5.catdogeats.pets.domain.dto;

import com.team5.catdogeats.pets.domain.enums.Gender;
import com.team5.catdogeats.pets.domain.enums.PetCategory;

public record PetUpdateRequestDto(
        String petId,
        String name,
        PetCategory petCategory,
        Gender gender,
        String breed,
        Short age
) {
}
