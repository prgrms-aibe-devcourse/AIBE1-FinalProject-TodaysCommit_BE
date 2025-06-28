package com.team5.catdogeats.reviews.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.pets.domain.Pets;
import com.team5.catdogeats.pets.repository.PetRepository;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.domain.dto.*;
import com.team5.catdogeats.reviews.repository.ReviewRepository;
import com.team5.catdogeats.reviews.service.ReviewService;
import com.team5.catdogeats.storage.domain.dto.ReviewImageResponseDto;
import com.team5.catdogeats.storage.domain.mapping.ReviewsImages;
import com.team5.catdogeats.storage.repository.ReviewImageRepository;
import com.team5.catdogeats.storage.service.ReviewImageService;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final BuyerRepository buyerRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewImageService reviewImageService;
    private final PetRepository petRepository;

    @Override
    public String registerReview(UserPrincipal userPrincipal, ReviewCreateRequestDto dto) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        Products product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new NoSuchElementException("해당 상품 정보를 찾을 수 없습니다."));

        Buyers buyer = Buyers.builder()
                .userId(buyerDTO.userId())
                .nameMaskingStatus(buyerDTO.nameMaskingStatus())
                .build();

        Reviews review = Reviews.fromDto(dto, buyer, product);

        return reviewRepository.save(review).getId();
    }

    @JpaTransactional(readOnly = true)
    @Override
    public Page<MyReviewResponseDto> getReviewsByBuyer(UserPrincipal userPrincipal, int page, int size) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        Buyers buyer = Buyers.builder()
                .userId(buyerDTO.userId())
                .nameMaskingStatus(buyerDTO.nameMaskingStatus())
                .build();

        Pageable pageable = PageRequest.of(page, size);

        Page<Reviews> reviewsPage = reviewRepository.findByBuyer(buyer, pageable);

        return reviewsPage.map(review -> {
            List<ReviewImageResponseDto> images = reviewImageService.getReviewImagesByReviewId(review.getId());
            return MyReviewResponseDto.fromEntity(review, images);
        });
    }

    @JpaTransactional(readOnly = true)
    @Override
    public Page<ProductReviewResponseDto> getReviewsByProductNumber(Long productNumber, int page, int size) {
        Products product = productRepository.findByProductNumber(productNumber)
                .orElseThrow(() -> new NoSuchElementException("해당 상품 정보를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(page, size);

        Page<Reviews> reviewsPage = reviewRepository.findByProductNumber(productNumber, pageable);


        return reviewsPage.map(review -> {
            //해당 리뷰 작성자(구매자) pet 조회
            List<Pets> petInfos = petRepository.findByBuyer(review.getBuyer());

            List<ReviewImageResponseDto> images = reviewImageService.getReviewImagesByReviewId(review.getId());

            return ProductReviewResponseDto.fromEntity(review, images, petInfos);
        });
    }

    @JpaTransactional
    @Override
    public void updateReview(ReviewUpdateRequestDto dto) {
        Reviews review = reviewRepository.findById(dto.reviewId())
                .orElseThrow(() -> new NoSuchElementException("해당 리뷰를 찾을 수 없습니다."));

        review.updateFromDto(dto);
    }

    @JpaTransactional
    @Override
    public void deleteReview(ReviewDeleteRequestDto dto) {
        Reviews review = reviewRepository.findById(dto.reviewId())
                .orElseThrow(() -> new NoSuchElementException("해당 리뷰를 찾을 수 없습니다."));

        // 1. 리뷰와 연결된 모든 이미지 매핑 조회
        List<ReviewsImages> mappings = reviewImageRepository.findAllByReviewsId(dto.reviewId());
        // 2. 이미지 삭제 서비스 호출
        for (ReviewsImages mapping : mappings) {
            reviewImageService.deleteReviewImage(dto.reviewId(), mapping.getImages().getId());
        }
        reviewRepository.deleteById(dto.reviewId());
    }
}
