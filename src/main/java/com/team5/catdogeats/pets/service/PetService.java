package com.team5.catdogeats.pets.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.pets.domain.dto.PetCreateRequestDto;
import com.team5.catdogeats.pets.domain.dto.PetDeleteRequestDto;
import com.team5.catdogeats.pets.domain.dto.PetResponseDto;
import com.team5.catdogeats.pets.domain.dto.PetUpdateRequestDto;
import org.springframework.data.domain.Page;

public interface PetService {
    String registerPet(UserPrincipal userPrincipal, PetCreateRequestDto dto);
    Page<PetResponseDto> getMyPets(UserPrincipal userPrincipal, int page, int size);
    void updatePet(PetUpdateRequestDto dto);
    void deletePet(PetDeleteRequestDto dto);
}