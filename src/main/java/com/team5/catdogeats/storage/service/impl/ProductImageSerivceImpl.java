package com.team5.catdogeats.storage.service.impl;

import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.storage.domain.Images;
import com.team5.catdogeats.storage.domain.dto.ProductImageResponseDto;
import com.team5.catdogeats.storage.domain.dto.ProductImageUploadResponseDto;
import com.team5.catdogeats.storage.domain.mapping.ProductsImages;
import com.team5.catdogeats.storage.repository.ImageRepository;
import com.team5.catdogeats.storage.repository.ProductImageRepository;
import com.team5.catdogeats.storage.service.ObjectStorageService;
import com.team5.catdogeats.storage.service.ProductImageService;
import com.team5.catdogeats.storage.util.ImageValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageSerivceImpl implements ProductImageService {

    private final ObjectStorageService objectStorageService;
    private final ImageRepository imageRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final ImageValidationUtil imageValidationUtil;

    @JpaTransactional
    @Override
    public List<ProductImageUploadResponseDto> uploadProductImage(String productId, List<MultipartFile> images) throws IOException {

        if (images.size() > 10) {
            throw new IllegalArgumentException("이미지는 한 번에 최대 10개까지만 업로드할 수 있습니다.");
        }

        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("해당 아이템 정보를 찾을 수 없습니다."));

        List<ProductImageUploadResponseDto> result = new ArrayList<>();

        for (MultipartFile file : images) {
            imageValidationUtil.validateImageFile(file);

            // 동일한 이름을 가진 이미지 파일 덮어쓰기 방지
            String uniqueKey = generateUniqueFileName(file.getOriginalFilename(), productId);

            // S3 업로드
            String s3Url = objectStorageService.uploadImage(
                    uniqueKey,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );

            // Images 테이블 저장
            Images image = Images.builder()
                    .imageUrl(s3Url)
                    .build();
            Images savedImage = imageRepository.save(image);

            // products_images 매핑 저장
            ProductsImages productImages = ProductsImages.builder()
                    .products(product)
                    .images(savedImage)
                    .build();
            productImageRepository.save(productImages);

            result.add(new ProductImageUploadResponseDto(image.getId(), s3Url));
        }

        return result;
    }

    @JpaTransactional
    @Override
    public List<ProductImageUploadResponseDto> updateProductImage(String productId, List<String> oldImageIds, List<MultipartFile> images) throws IOException {
        // 1. 기존 이미지/매핑/S3에 있는 이미지들 중 골라서 삭제
        for (String oldImageId : oldImageIds) {
            this.deleteProductImage(productId, oldImageId);
        }

        // 2. 새 이미지 업로드/매핑
        return this.uploadProductImage(productId, images);

    }

    @JpaTransactional
    @Override
    public List<ProductImageResponseDto> getProductImagesByProductId(String productId) {
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("해당 아이템 정보를 찾을 수 없습니다."));

        List<ProductsImages> mappings = productImageRepository.findAllByProductsIdWithImages(productId);
        
        return mappings.stream()
                .map(mapping -> new ProductImageResponseDto(
                        mapping.getImages().getId(),
                        mapping.getImages().getImageUrl()
                ))
                .toList();
    }

    @JpaTransactional
    @Override
    public void deleteProductImage(String productId, String imageId) {
        ProductsImages mapping = productImageRepository.findByProductsIdAndImagesId(productId, imageId)
                .orElseThrow(() -> new NoSuchElementException("해당 매핑 데이터 없음"));

        // S3에서 이미지 파일 삭제 (images/{파일명})
        String imageUrl = mapping.getImages().getImageUrl();
        String fileKey = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        objectStorageService.deleteImage(fileKey);

        // 매핑, 이미지 DB 삭제
        productImageRepository.delete(mapping);
        imageRepository.deleteById(imageId);
    }

    /**
     * 고유한 파일명 생성
     * 형식: product_{productId}_{UUID}.{확장자}
     */
    private String generateUniqueFileName(String originalFileName, String productId) {
        String extension = imageValidationUtil.getFileExtension(originalFileName);
        String uuid = UUID.randomUUID().toString().replace("-", "");

        String shortReviewId = productId.length() > 8 ? productId.substring(0, 8) : productId;

        // 처음 8자리만 사용 (너무 길어지는 것 방지)
        return String.format("product_%s_%s.%s", shortReviewId, uuid, extension);
    }
}
