package com.team5.catdogeats.pets.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final BuyerRepository buyerRepository;

    @Override
    public UUID registerPet(UserPrincipal userPrincipal, PetCreateRequestDto dto) {
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
    public List<PetResponseDto> getMyPets(UserPrincipal userPrincipal) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        Buyers buyer = Buyers.builder()
                .userId(buyerDTO.userId())
                .nameMaskingStatus(buyerDTO.nameMaskingStatus())
                .build();

        return petRepository.findByBuyer(buyer)
                .stream()
                .map(PetResponseDto::fromEntity)
                .toList();
    }

    @Transactional
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
