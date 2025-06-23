package com.team5.catdogeats.pets.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.pets.domain.Pets;
import com.team5.catdogeats.pets.domain.dto.PetCreateRequestDto;
import com.team5.catdogeats.pets.domain.dto.PetDeleteRequestDto;
import com.team5.catdogeats.pets.domain.dto.PetResponseDto;
import com.team5.catdogeats.pets.domain.dto.PetUpdateRequestDto;
import com.team5.catdogeats.pets.repository.PetRepository;
import com.team5.catdogeats.pets.service.PetService;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final BuyerRepository buyerRepository;

    @Override
    public String registerPet(UserPrincipal userPrincipal, PetCreateRequestDto dto) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        Buyers buyer = Buyers.builder()
                .userId(buyerDTO.userId())
                .nameMaskingStatus(buyerDTO.nameMaskingStatus())
                .build();

        Pets pet = Pets.fromDto(dto, buyer);
        return petRepository.save(pet).getId();
    }

    @Override
    public Page<PetResponseDto> getMyPets(UserPrincipal userPrincipal, int page, int size) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        Buyers buyer = Buyers.builder()
                .userId(buyerDTO.userId())
                .nameMaskingStatus(buyerDTO.nameMaskingStatus())
                .build();

        Pageable pageable = PageRequest.of(page, size);

        return petRepository.findByBuyer(buyer, pageable)
                .map(PetResponseDto::fromEntity);
    }

    @JpaTransactional
    @Override
    public void updatePet(PetUpdateRequestDto dto) {
        Pets pet = petRepository.findById(dto.petId())
                .orElseThrow(() -> new NoSuchElementException("해당 펫 정보를 찾을 수 없습니다."));

        pet.updateFromDto(dto);
    }

    @Override
    public void deletePet(PetDeleteRequestDto dto) {
        Pets pet = petRepository.findById(dto.petId())
                .orElseThrow(() -> new NoSuchElementException("해당 펫 정보를 찾을 수 없습니다."));

        petRepository.deleteById(dto.petId());
    }
}
