package com.team5.catdogeats.storage.service.impl;

import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.repository.ReviewRepository;
import com.team5.catdogeats.storage.domain.Images;
import com.team5.catdogeats.storage.domain.dto.ReviewImageUploadResponseDto;
import com.team5.catdogeats.storage.domain.mapping.ReviewsImages;
import com.team5.catdogeats.storage.repository.ImageRepository;
import com.team5.catdogeats.storage.repository.ReviewImageRepository;
import com.team5.catdogeats.storage.service.ObjectStorageService;
import com.team5.catdogeats.storage.service.ReviewImageService;
import com.team5.catdogeats.storage.util.ImageValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewImageSerivceImpl implements ReviewImageService {

    private final ObjectStorageService objectStorageService;
    private final ImageRepository imageRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewRepository reviewRepository;
    private final ImageValidationUtil imageValidationUtil;

    @JpaTransactional
    @Override
    public List<ReviewImageUploadResponseDto> uploadReviewImage(String reviewId, List<MultipartFile> images) throws IOException {
        Reviews review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("해당 리뷰 없음"));

        List<ReviewImageUploadResponseDto> result = new ArrayList<>();

        for (MultipartFile file : images) {
            imageValidationUtil.validateImageFile(file);

            // 동일한 이름을 가진 이미지 파일 덮어쓰기 방지
            String uniqueKey = generateUniqueFileName(file.getOriginalFilename(), reviewId);

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

            // reviews_images 매핑 저장
            ReviewsImages reviewsImages = ReviewsImages.builder()
                    .reviews(review)
                    .images(savedImage)
                    .build();
            reviewImageRepository.save(reviewsImages);

            result.add(new ReviewImageUploadResponseDto(image.getId(), s3Url));
        }

        return result;
    }

    @JpaTransactional
    @Override
    public List<ReviewImageUploadResponseDto> updateReviewImage(String reviewId, List<String> oldImageIds, List<MultipartFile> images) throws IOException {
        // 1. 기존 이미지/매핑/S3에 있는 이미지들 중 골라서 삭제
        for (String oldImageId : oldImageIds) {
            this.deleteReviewImage(reviewId, oldImageId);
        }

        // 2. 새 이미지 업로드/매핑
        return this.uploadReviewImage(reviewId, images);

    }

    @JpaTransactional
    @Override
    public void deleteReviewImage(String reviewId, String imageId) {
        ReviewsImages mapping = reviewImageRepository.findByReviewsIdAndImagesId(reviewId, imageId)
                .orElseThrow(() -> new NoSuchElementException("해당 매핑 데이터 없음"));

        // S3에서 이미지 파일 삭제 (images/{파일명})
        String imageUrl = mapping.getImages().getImageUrl();
        String fileKey = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        objectStorageService.deleteImage(fileKey);

        // 매핑, 이미지 DB 삭제
        reviewImageRepository.delete(mapping);
        imageRepository.deleteById(imageId);
    }

    /**
     * 고유한 파일명 생성
     * 형식: review_{reviewId}_{UUID}.{확장자}
     */
    private String generateUniqueFileName(String originalFileName, String reviewId) {
        String extension = imageValidationUtil.getFileExtension(originalFileName);
        String uuid = UUID.randomUUID().toString().replace("-", "");

        String shortReviewId = reviewId.length() > 8 ? reviewId.substring(0, 8) : reviewId;

        // 처음 8자리만 사용 (너무 길어지는 것 방지)
        return String.format("review_%s_%s.%s", shortReviewId, uuid, extension);
    }
}
