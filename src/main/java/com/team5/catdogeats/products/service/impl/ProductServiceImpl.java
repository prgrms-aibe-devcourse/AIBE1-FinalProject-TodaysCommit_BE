package com.team5.catdogeats.products.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.dto.*;
import com.team5.catdogeats.products.domain.enums.SellerProductSortType;
import com.team5.catdogeats.products.exception.DuplicateProductNumberException;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.service.ProductService;
import com.team5.catdogeats.reviews.domain.dto.SellerReviewSummaryResponseDto;
import com.team5.catdogeats.reviews.repository.ReviewRepository;
import com.team5.catdogeats.storage.domain.dto.ProductImageResponseDto;
import com.team5.catdogeats.storage.domain.mapping.ProductsImages;
import com.team5.catdogeats.storage.repository.ProductImageRepository;
import com.team5.catdogeats.storage.service.ProductImageService;
import com.team5.catdogeats.users.domain.dto.SellerDTO;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SellersRepository sellerRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductImageService productImageService;
    private final ReviewRepository reviewRepository;

    @Override
    public String registerProduct(UserPrincipal userPrincipal, ProductCreateRequestDto dto) {
        SellerDTO sellerDTO = sellerRepository.findSellerDtoByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        Sellers seller = Sellers.builder()
                .userId(sellerDTO.userId())
                .vendorName(sellerDTO.vendorName())
                .vendorProfileImage(sellerDTO.vendorProfileImage())
                .businessNumber(sellerDTO.businessNumber())
                .settlementBank(sellerDTO.settlementBank())
                .settlementAccount(sellerDTO.settlementAccount())
                .tags(sellerDTO.tags())
                .operatingStartTime(sellerDTO.operatingStartTime())
                .operatingEndTime(sellerDTO.operatingEndTime())
                .closedDays(sellerDTO.closedDays())
                .build();

        Long productNumber;
        try {
            productNumber = generateProductNumber();
        } catch (DuplicateProductNumberException e) {
            log.warn("상품 번호 중복 발생, 1회 재시도");
            try {
                productNumber = generateProductNumber();
            } catch (DuplicateProductNumberException ex) {
                throw new IllegalStateException("상품 번호 생성 실패: 중복으로 인한 재시도 실패", ex);
            }
        }

        Products product = Products.fromDto(dto, seller, productNumber);
        return productRepository.save(product).getId();
    }

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

    @JpaTransactional
    @Override
    public void updateProduct(ProductUpdateRequestDto dto) {
        Products product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new NoSuchElementException("해당 아이템 정보를 찾을 수 없습니다."));

        product.updateFromDto(dto);
    }

    @Override
    public void deleteProduct(ProductDeleteRequestDto dto) {
        Products product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new NoSuchElementException("해당 아이템 정보를 찾을 수 없습니다."));

        // 1. 리뷰와 연결된 모든 이미지 매핑 조회
        List<ProductsImages> mappings = productImageRepository.findAllByProductsId(dto.productId());
        // 2. 이미지 삭제 서비스 호출
        for (ProductsImages mapping : mappings) {
            productImageService.deleteProductImage(dto.productId(), mapping.getImages().getId());
        }

        productRepository.deleteById(dto.productId());
    }



    // TODO: 상품 조회 서비스 로직 / 상품 상세 조회 서비스 로직 구현하기


    /**
     * 고유한 상품 번호를 생성하는 메서드
     * (yyyyMMddHHmmss + 6자리 랜덤 숫자)
     */
    private Long generateProductNumber() {
        String timestamp = DateTimeFormatter.ofPattern("yyMMddHHmmss")
                .format(LocalDateTime.now());
        int randomNum = ThreadLocalRandom.current().nextInt(1000, 10000);
        Long productNumber = Long.parseLong(timestamp + randomNum);

        if (productRepository.existsByProductNumber(productNumber)) {
            throw new DuplicateProductNumberException("중복된 상품 번호: " + productNumber);
        }

        log.debug("상품 번호 생성 성공: {}", productNumber);
        return productNumber;
    }
}
