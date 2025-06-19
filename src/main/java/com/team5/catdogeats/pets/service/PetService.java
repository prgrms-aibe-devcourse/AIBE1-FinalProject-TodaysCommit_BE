package com.team5.catdogeats.pets.service;

import com.team5.catdogeats.pets.domain.dto.PetCreateRequestDto;
import com.team5.catdogeats.pets.domain.dto.PetDeleteRequestDto;
import com.team5.catdogeats.pets.domain.dto.PetResponseDto;
import com.team5.catdogeats.pets.domain.dto.PetUpdateRequestDto;

import java.util.List;
import java.util.UUID;

public interface PetService {
    UUID registerPet(PetCreateRequestDto dto);
    List<PetResponseDto> getMyPets();
    void updatePet(PetUpdateRequestDto dto);
    void deletePet(PetDeleteRequestDto dto);
}