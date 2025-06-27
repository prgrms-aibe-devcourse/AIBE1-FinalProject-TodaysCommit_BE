package com.team5.catdogeats.reviews.domain.dto;

import com.team5.catdogeats.pets.domain.Pets;
import com.team5.catdogeats.pets.domain.dto.PetInfoResponseDto;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.storage.domain.dto.ReviewImageResponseDto;

import java.util.List;

// 특정 productNumber로 리뷰 조회시 response
public record ProductReviewResponseDto(
        String id,
        String writerName,
        List<PetInfoResponseDto> petInfoDtoList,
        Double star,
        String contents,
        String updatedAt,
        List<ReviewImageResponseDto> images
) {
    public static ProductReviewResponseDto fromEntity(Reviews review, List<ReviewImageResponseDto> images, List<Pets> petInfos) {
        List<PetInfoResponseDto> petInfoDtoList = petInfos.stream()
                .map(pet -> new PetInfoResponseDto(pet.getBreed(), pet.getAge(), pet.getGender()))
                .toList();

        return new ProductReviewResponseDto(
                review.getId(),
                review.getBuyer().getUser().getName(),
                petInfoDtoList,
                review.getStar(),
                review.getContents(),
                review.getUpdatedAt().toString(),
                images
        );
    }
}
