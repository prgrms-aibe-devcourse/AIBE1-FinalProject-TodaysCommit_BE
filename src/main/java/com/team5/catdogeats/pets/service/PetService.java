package com.team5.catdogeats.pets.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.pets.domain.dto.PetCreateRequestDto;
import com.team5.catdogeats.pets.domain.dto.PetDeleteRequestDto;
import com.team5.catdogeats.pets.domain.dto.PetResponseDto;
import com.team5.catdogeats.pets.domain.dto.PetUpdateRequestDto;

import java.util.List;

public interface PetService {
    String registerPet(UserPrincipal userPrincipal, PetCreateRequestDto dto);
    List<PetResponseDto> getMyPets(UserPrincipal userPrincipal);
    void updatePet(PetUpdateRequestDto dto);
    void deletePet(PetDeleteRequestDto dto);
}