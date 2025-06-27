package com.team5.catdogeats.storage.service;

import com.team5.catdogeats.storage.domain.dto.ProductImageResponseDto;
import com.team5.catdogeats.storage.domain.dto.ProductImageUploadResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductImageService {
    List<ProductImageUploadResponseDto> uploadProductImage(String productId, List<MultipartFile> images) throws IOException;
    void deleteProductImage(String productId, String imageId);
    List<ProductImageUploadResponseDto> updateProductImage(String productId, List<String> oldImageIds, List<MultipartFile> images) throws IOException;
    // 나중에 상품 일괄 조회에 사용 (구매자 / 판매자 모두)
    List<ProductImageResponseDto> getProductImagesByProductId(String productId);
}
