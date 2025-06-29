package com.team5.catdogeats.products.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.dto.ProductCreateRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductDeleteRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductUpdateRequestDto;
import com.team5.catdogeats.products.exception.DuplicateProductNumberException;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.service.ProductService;
import com.team5.catdogeats.users.domain.dto.SellerDTO;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SellersRepository sellerRepository;

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
