package com.team5.catdogeats.reviews.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.domain.dto.ReviewCreateRequestDto;
import com.team5.catdogeats.reviews.domain.dto.ReviewDeleteRequestDto;
import com.team5.catdogeats.reviews.domain.dto.ReviewResponseDto;
import com.team5.catdogeats.reviews.domain.dto.ReviewUpdateRequestDto;
import com.team5.catdogeats.reviews.repository.ReviewRepository;
import com.team5.catdogeats.reviews.service.ReviewService;
import com.team5.catdogeats.storage.repository.ImageRepository;
import com.team5.catdogeats.storage.repository.ReviewImageRepository;
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
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final BuyerRepository buyerRepository;

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

    @Override
    public Page<ReviewResponseDto> getReviewsByBuyer(UserPrincipal userPrincipal, int page, int size) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        Buyers buyer = Buyers.builder()
                .userId(buyerDTO.userId())
                .nameMaskingStatus(buyerDTO.nameMaskingStatus())
                .build();

        Pageable pageable = PageRequest.of(page, size);

        return reviewRepository.findByBuyer(buyer, pageable)
                .map(ReviewResponseDto::fromEntity);
    }

    @Override
    public Page<ReviewResponseDto> getReviewsByProductId(String productId, int page, int size) {
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("해당 상품 정보를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(page, size);

        return reviewRepository.findByProductId(productId, pageable)
                .map(ReviewResponseDto::fromEntity);
    }

    @JpaTransactional
    @Override
    public void updateReview(ReviewUpdateRequestDto dto) {
        Reviews review = reviewRepository.findById(dto.reviewId())
                .orElseThrow(() -> new NoSuchElementException("해당 리뷰를 찾을 수 없습니다."));

        review.updateFromDto(dto);
    }

    @Override
    public void deleteReview(ReviewDeleteRequestDto dto) {
        Reviews review = reviewRepository.findById(dto.reviewId())
                .orElseThrow(() -> new NoSuchElementException("해당 리뷰를 찾을 수 없습니다."));

        reviewRepository.deleteById(dto.reviewId());
    }
}
