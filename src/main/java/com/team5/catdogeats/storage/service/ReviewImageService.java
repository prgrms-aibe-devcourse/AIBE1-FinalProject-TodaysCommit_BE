package com.team5.catdogeats.storage.service;

import com.team5.catdogeats.storage.domain.dto.ReviewImageUploadResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ReviewImageService {
    List<ReviewImageUploadResponseDto> uploadReviewImage(String reviewId, List<MultipartFile> images) throws IOException;
    void deleteReviewImage(String reviewId, String imageId);
    List<ReviewImageUploadResponseDto> updateReviewImage(String reviewId, List<String> oldImageIds, List<MultipartFile> images) throws IOException;
}
