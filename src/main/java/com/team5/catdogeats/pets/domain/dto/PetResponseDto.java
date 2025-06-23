package com.team5.catdogeats.pets.domain.dto;

import com.team5.catdogeats.pets.domain.Pets;
import com.team5.catdogeats.pets.domain.enums.Gender;
import com.team5.catdogeats.pets.domain.enums.PetCategory;

public record PetResponseDto(
        String id,
        String name,
        PetCategory petCategory,
        Gender gender,
        String breed,
        Short age
) {
    public static PetResponseDto fromEntity(Pets pet) {
        return new PetResponseDto(
                pet.getId(), pet.getName(), pet.getPetCategory(),
                pet.getGender(), pet.getBreed(), pet.getAge()
        );
    }
}
