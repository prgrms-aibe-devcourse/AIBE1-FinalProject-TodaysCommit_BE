package com.team5.catdogeats.pets.service.impl;

import com.team5.catdogeats.global.exception.CustomException;
import com.team5.catdogeats.pets.domain.Pets;
import com.team5.catdogeats.pets.domain.dto.PetCreateRequestDto;
import com.team5.catdogeats.pets.domain.dto.PetDeleteRequestDto;
import com.team5.catdogeats.pets.domain.dto.PetResponseDto;
import com.team5.catdogeats.pets.domain.dto.PetUpdateRequestDto;
import com.team5.catdogeats.pets.repository.PetRepository;
import com.team5.catdogeats.pets.service.PetService;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final BuyerRepository buyerRepository;

    @Override
    public UUID registerPet(PetCreateRequestDto dto) {
//      TODO: UUID userId = SecurityUtil.getCurrentUserId();
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Buyers buyer = buyerRepository.findById(userId)
                .orElseThrow(() -> new CustomException("해당 유저가 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        Pets pet = Pets.fromDto(dto, buyer);
        return petRepository.save(pet).getId();
    }

    @Override
    public List<PetResponseDto> getMyPets() {
//      TODO:  UUID userId = SecurityUtil.getCurrentUserId();
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Buyers buyer = buyerRepository.findById(userId)
                .orElseThrow(() -> new CustomException("해당 유저가 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        return petRepository.findByBuyer(buyer)
                .stream()
                .map(PetResponseDto::fromEntity)
                .toList();
    }

    @Transactional
    @Override
    public void updatePet(PetUpdateRequestDto dto) {
        Pets pet = petRepository.findById(dto.petId())
                .orElseThrow(() -> new CustomException("해당하는 펫의 정보가 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        pet.updateFromDto(dto);
    }

    @Override
    public void deletePet(PetDeleteRequestDto dto) {
        Pets pet = petRepository.findById(dto.petId())
                .orElseThrow(() -> new CustomException("해당하는 펫의 정보가 존재하지 않습니다.", HttpStatus.NOT_FOUND));

        petRepository.deleteById(dto.petId());
    }
}
