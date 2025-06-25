package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.storage.service.ObjectStorageService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.SellerBrandImageResponseDTO;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import com.team5.catdogeats.users.service.SellerBrandImageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerBrandImageServiceImpl implements SellerBrandImageService {

    private final SellersRepository sellersRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;

    @Override
    @JpaTransactional
    public SellerBrandImageResponseDTO uploadBrandImage(UserPrincipal userPrincipal, MultipartFile imageFile) {
        // 1. 파일 검증
        validateImageFile(imageFile);

        // 2. 사용자 및 판매자 조회
        Users user = findUserByPrincipal(userPrincipal);
        Sellers seller = findSellerByUserId(user.getId());

        // 3. 기존 이미지 삭제(있는 경우만)
        deleteExistingImage(seller.getVendorProfileImage());

        // 4. 새 이미지 업로드
        String newImageUrl = uploadNewImage(imageFile, seller.getUserId());

        seller.updateVendorProfileImage(newImageUrl); // Entity 메서드 사용
        Sellers savedSeller = sellersRepository.save(seller);

        return SellerBrandImageResponseDTO.from(savedSeller);
    }

    @Override
    @JpaTransactional
    public SellerBrandImageResponseDTO deleteBrandImage(UserPrincipal userPrincipal) {
        log.info("판매자 브랜드 이미지 삭제 요청 - provider: {}, providerId: {}",
                userPrincipal.provider(), userPrincipal.providerId());

        // 1. 사용자 조회
        Users user = findUserByPrincipal(userPrincipal);

        // 2. 판매자 정보 조회
        Sellers seller = findSellerByUserId(user.getId());

        // 3. 기존 이미지 삭제 (S3에서)
        String existingImageUrl = seller.getVendorProfileImage();
        if (existingImageUrl != null && !existingImageUrl.trim().isEmpty()) {
            deleteExistingImage(existingImageUrl);
            log.info("브랜드 이미지 S3 삭제 완료 - userId: {}, imageUrl: {}", user.getId(), existingImageUrl);
        } else {
            log.info("삭제할 브랜드 이미지가 없습니다 - userId: {}", user.getId());
        }

        // 4. DB에서 이미지 URL을 null로 설정
        int updateCount = sellersRepository.deleteVendorProfileImage(seller.getUserId());

        if (updateCount == 0) {
            throw new RuntimeException("브랜드 이미지 삭제에 실패했습니다.");
        }

        Sellers updatedSeller = findSellerByUserId(user.getId());

        return SellerBrandImageResponseDTO.from(updatedSeller);
    }

    /**
     * ️   통합 이미지 파일 검증
     * - 기본 속성 검사 (크기, NULL 등)
     * - 실제 파일 내용 검증 (Magic Number)
     * - 보안 강화된 단일 검증 메서드
     */
    private void validateImageFile(MultipartFile imageFile) {
        // 기본 검사
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어있습니다.");
        }

        // 파일 크기 검사
        long maxFileSize = 10 * 1024 * 1024; // 10MB
        if (imageFile.getSize() > maxFileSize) {
            throw new IllegalArgumentException("이미지 파일 크기는 10MB를 초과할 수 없습니다.");
        }

        // 실제 파일 내용 검증 (핵심 보안)
        validateFileSignature(imageFile);

        log.debug("이미지 파일 검증 완료 - fileName: {}, size: {}",
                imageFile.getOriginalFilename(), imageFile.getSize());
    }

    /**
     * UserPrincipal로 Users 엔티티 조회
     */
    private Users findUserByPrincipal(UserPrincipal userPrincipal) {
        return userRepository.findByProviderAndProviderId(
                userPrincipal.provider(),
                userPrincipal.providerId()
        ).orElseThrow(() -> new EntityNotFoundException(
                String.format("사용자를 찾을 수 없습니다 - provider: %s, providerId: %s",
                        userPrincipal.provider(), userPrincipal.providerId())));
    }

    /**
     * 판매자 정보 조회
     */
    private Sellers findSellerByUserId(String userId) {
        return sellersRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("판매자 정보를 찾을 수 없습니다 - userId: " + userId));
    }

    /**
     * 기존 이미지 삭제
     */
    private void deleteExistingImage(String existingImageUrl) {
        if (existingImageUrl == null || existingImageUrl.trim().isEmpty()) {
            log.debug("삭제할 기존 이미지가 없습니다.");
            return;
        }

        try {
            // URL에서 S3 키 추출 (images/ 부분 제거)
            String imageKey = extractImageKeyFromUrl(existingImageUrl);
            if (imageKey != null) {
                objectStorageService.deleteImage(imageKey);
                log.info("기존 브랜드 이미지 삭제 완료 - imageKey: {}", imageKey);
            }
        } catch (Exception e) {
            // 기존 이미지 삭제 실패는 로그만 남기고 계속 진행
            log.warn("기존 브랜드 이미지 삭제 실패 (계속 진행) - imageUrl: {}, error: {}",
                    existingImageUrl, e.getMessage());
        }
    }

    /**
     * URL에서 이미지 키 추출
     * 예: https://cdn.example.com/images/brand_image_123.jpg -> brand_image_123.jpg
     */
    private String extractImageKeyFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }

        try {
            // URL에서 마지막 '/' 이후의 파일명 추출
            int lastSlashIndex = imageUrl.lastIndexOf('/');
            if (lastSlashIndex != -1 && lastSlashIndex < imageUrl.length() - 1) {
                return imageUrl.substring(lastSlashIndex + 1);
            }
        } catch (Exception e) {
            log.warn("이미지 키 추출 실패 - imageUrl: {}, error: {}", imageUrl, e.getMessage());
        }

        return null;
    }

    /**
     * 새 이미지 업로드
     */
    private String uploadNewImage(MultipartFile imageFile, String userId) {
        try {
            // 고유한 파일명 생성 (UUID + 원본 확장자)
            String uniqueFileName = generateUniqueFileName(imageFile.getOriginalFilename(), userId);

            // S3에 업로드
            String imageUrl = objectStorageService.uploadImage(
                    uniqueFileName,
                    imageFile.getInputStream(),
                    imageFile.getSize(),
                    imageFile.getContentType()
            );

            log.debug("새 브랜드 이미지 업로드 완료 - fileName: {}, imageUrl: {}", uniqueFileName, imageUrl);
            return imageUrl;

        } catch (IOException e) {
            log.error("이미지 업로드 중 I/O 오류 발생 - fileName: {}", imageFile.getOriginalFilename(), e);
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("이미지 업로드 중 예상치 못한 오류 발생 - fileName: {}", imageFile.getOriginalFilename(), e);
            throw new RuntimeException("이미지 업로드 실패", e);
        }
    }

    /**
     * 고유한 파일명 생성
     * 형식: brand_{userId}_{UUID}.{확장자}
     */
    private String generateUniqueFileName(String originalFileName, String userId) {
        String extension = getFileExtension(originalFileName);
        String uuid = UUID.randomUUID().toString().replace("-", "");

        String shortUserId = userId.length() > 8 ? userId.substring(0, 8) : userId;

        // 처음 8자리만 사용 (너무 길어지는 것 방지)
        return String.format("brand_%s_%s.%s", shortUserId, uuid, extension);
    }


    /**
     * 🧹 안전한 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "jpg"; // 기본값
        }

        // 파일명 정화 (위험한 문자 제거)
        String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "");

        int lastDotIndex = safeName.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < safeName.length() - 1) {
            String ext = safeName.substring(lastDotIndex + 1).toLowerCase();

            // 허용된 확장자만 반환 (화이트리스트)
            if (ext.matches("^(jpg|jpeg|png|webp)$")) {
                return ext;
            }
        }

        return "jpg"; // 안전한 기본값
    }

    /**
     *   파일 시그니처 검증 (Magic Number)
     * - JPEG, PNG, WebP 실제 파일 형식 확인
     * - Content-Type 조작 공격 방어
     */
    private void validateFileSignature(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12]; // WebP 확인을 위해 12바이트
            int bytesRead = is.read(header);

            if (bytesRead < 4) {
                throw new IllegalArgumentException("파일 형식을 확인할 수 없습니다.");
            }

            // JPEG: FF D8 FF
            if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
                return; // 진짜 JPEG
            }

            // PNG: 89 50 4E 47
            if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
                return; // 진짜 PNG
            }

            // WebP: RIFF....WEBP
            // RIFF
            if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46 && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) { // WEBP
                return; // 진짜 WebP
            }

            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (JPEG, PNG, WebP만 지원)");

        } catch (IOException e) {
            throw new IllegalArgumentException("파일 형식 검증 중 오류가 발생했습니다.", e);
        }
    }





}
