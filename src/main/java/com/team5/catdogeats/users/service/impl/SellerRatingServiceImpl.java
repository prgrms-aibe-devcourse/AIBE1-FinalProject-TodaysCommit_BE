package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.products.domain.dto.MyProductResponseDto;
import com.team5.catdogeats.products.domain.dto.MyProductSummaryDto;
import com.team5.catdogeats.products.domain.enums.SellerProductSortType;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.reviews.domain.dto.SellerReviewSummaryResponseDto;
import com.team5.catdogeats.reviews.repository.ReviewRepository;
import com.team5.catdogeats.storage.domain.dto.ProductImageResponseDto;
import com.team5.catdogeats.storage.repository.ProductImageRepository;
import com.team5.catdogeats.users.domain.dto.SellerDTO;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.service.SellerRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SellerRatingServiceImpl implements SellerRatingService {

    private final ProductRepository productRepository;
    private final SellersRepository sellerRepository;
    private final ProductImageRepository productImageRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public Page<MyProductResponseDto> getProductsBySeller(UserPrincipal userPrincipal, int page, int size, SellerProductSortType sortType) {
        SellerDTO sellerDTO = sellerRepository.findSellerDtoByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        // Pageable은 무시하고, 모든 상품을 다 가져옴
        List<MyProductSummaryDto> allSummaries = productRepository.findSummaryBySellerId(sellerDTO.userId(), Pageable.unpaged())
                .getContent();

        List<MyProductResponseDto> dtos = allSummaries.stream().map(summary -> {
            List<ProductImageResponseDto> imageDtos = productImageRepository.findFirstImageDtoByProductId(summary.productId());
            ProductImageResponseDto imageDto = imageDtos.isEmpty() ? null : imageDtos.get(0);

            return new MyProductResponseDto(
                    summary.productId(),
                    summary.productName(),
                    summary.reviewCount(),
                    // 소수점 한 자리로 반올림
                    Math.round(summary.averageStar() * 10) / 10.0,
                    imageDto
            );
        }).toList();

        // 정렬 적용
        List<MyProductResponseDto> sorted = switch (sortType) {
            case STAR -> dtos.stream()
                    .sorted(Comparator.comparingDouble(MyProductResponseDto::averageStar).reversed())
                    .toList();
            case REVIEW -> dtos.stream()
                    .sorted(Comparator.comparingLong(MyProductResponseDto::reviewCount).reversed())
                    .toList();
            default -> dtos; // LATEST: product 생성순
        };

        // 페이지네이션 적용
        int start = Math.min(page * size, sorted.size());
        int end = Math.min(start + size, sorted.size());
        List<MyProductResponseDto> pageContent = sorted.subList(start, end);

        return new PageImpl<>(pageContent, PageRequest.of(page, size), sorted.size());
    }

    @Override
    public SellerReviewSummaryResponseDto getSellerReviewSummary(UserPrincipal userPrincipal) {
        SellerDTO sellerDTO = sellerRepository.findSellerDtoByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        // 전체 평균, 전체 개수
        List<Object[]> avgCountList = reviewRepository.findAvgAndCountBySellerId(sellerDTO.userId());
        Object[] avgCount = avgCountList.isEmpty() ? new Object[]{0.0, 0L} : avgCountList.get(0);
        double avgStar = avgCount[0] != null ? ((Number) avgCount[0]).doubleValue() : 0.0;
        long totalCount = avgCount[1] != null ? ((Number) avgCount[1]).longValue() : 0L;

        // 구간별 개수
        Map<Integer, Long> starGroupCount = new HashMap<>();
        // 0~5점대 기본값 0으로 초기화
        for (int i = 0; i <= 5; i++) starGroupCount.put(i, 0L);

        for (Object[] groupRow : reviewRepository.findGroupStarCountBySellerId(sellerDTO.userId())) {
            Integer group = groupRow[0] != null ? ((Number) groupRow[0]).intValue() : null;
            Long count = groupRow[1] != null ? ((Number) groupRow[1]).longValue() : null;
            if (group != null && count != null) {
                starGroupCount.put(group, count);
            }
        }

        // 소수점 한 자리로 평균 반올림
        avgStar = Math.round(avgStar * 10) / 10.0;

        return new SellerReviewSummaryResponseDto(avgStar, totalCount, starGroupCount);
    }
}
