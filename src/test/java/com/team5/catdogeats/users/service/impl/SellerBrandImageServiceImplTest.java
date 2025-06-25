package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.storage.service.ObjectStorageService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.SellerBrandImageResponseDTO;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerBrandImageServiceImplTest {

    @Mock
    private SellersRepository sellersRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @InjectMocks
    private SellerBrandImageServiceImpl sellerBrandImageService;

    private UserPrincipal userPrincipal;
    private Users user;
    private Sellers seller;
    private MultipartFile validImageFile;

    @BeforeEach
    void setUp() {
        // UserPrincipal 생성
        userPrincipal = new UserPrincipal("google", "12345");

        // Users 엔티티 생성
        user = Users.builder()
                .id("user-uuid-123")
                .provider("google")
                .providerId("12345")
                .name("테스트 사용자")
                .build();

        // Sellers 엔티티 생성
        seller = Sellers.builder()
                .userId("user-uuid-123")
                .user(user)
                .vendorName("멍멍이네 수제간식")
                .vendorProfileImage("https://cdn.example.com/images/old_brand_image.jpg")
                .businessNumber("123-45-67890")
                .build();

        // 유효한 이미지 파일 생성
        validImageFile = new MockMultipartFile(
                "image",
                "brand_image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
    }



    @Test
    @DisplayName("브랜드 이미지 업로드 성공")
    void uploadBrandImage_Success() throws IOException {
        // given
        String newImageUrl = "https://cdn.example.com/images/brand_user-uuid_abc123.jpg";

        when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                .thenReturn(Optional.of(user));
        when(sellersRepository.findByUserId(anyString()))
                .thenReturn(Optional.of(seller));
        when(objectStorageService.uploadImage(anyString(), any(), anyLong(), anyString()))
                .thenReturn(newImageUrl);
        when(sellersRepository.save(any(Sellers.class)))
                .thenReturn(seller);

        // when
        SellerBrandImageResponseDTO result = sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo("user-uuid-123");
        assertThat(result.vendorName()).isEqualTo("멍멍이네 수제간식");

        // 기존 이미지 삭제 호출 확인
        verify(objectStorageService).deleteImage("old_brand_image.jpg");

        // 새 이미지 업로드 호출 확인
        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(objectStorageService).uploadImage(
                fileNameCaptor.capture(),
                any(),
                eq(validImageFile.getSize()),
                eq("image/jpeg")
        );

        // 생성된 파일명이 올바른 형식인지 확인
        String capturedFileName = fileNameCaptor.getValue();
        assertThat(capturedFileName).startsWith("brand_123_");
        assertThat(capturedFileName).endsWith(".jpg");

        // 더 구체적인 형식 검증: brand_123_{32자리UUID}.jpg
        assertThat(capturedFileName).matches("brand_123_[a-f0-9]{32}\\.jpg");

        assertThat(capturedFileName.length()).isBetween(45, 50); // 범위로 검증

        // 판매자 정보 저장 호출 확인
        verify(sellersRepository).save(seller);
    }

    @Test
    @DisplayName("브랜드 이미지 업로드 성공 - 기존 이미지가 없는 경우")
    void uploadBrandImage_Success_NoExistingImage() throws IOException {
        // given
        seller = Sellers.builder()
                .userId("user-uuid-123")
                .user(user)
                .vendorName("멍멍이네 수제간식")
                .vendorProfileImage(null) // 기존 이미지 없음
                .businessNumber("123-45-67890")
                .build();

        String newImageUrl = "https://cdn.example.com/images/brand_user-uuid_abc123.jpg";

        when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                .thenReturn(Optional.of(user));
        when(sellersRepository.findByUserId(anyString()))
                .thenReturn(Optional.of(seller));
        when(objectStorageService.uploadImage(anyString(), any(), anyLong(), anyString()))
                .thenReturn(newImageUrl);
        when(sellersRepository.save(any(Sellers.class)))
                .thenReturn(seller);

        // when
        SellerBrandImageResponseDTO result = sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile);

        // then
        assertThat(result).isNotNull();

        // 기존 이미지 삭제는 호출되지 않아야 함
        verify(objectStorageService, never()).deleteImage(anyString());

        // 새 이미지 업로드는 호출되어야 함
        verify(objectStorageService).uploadImage(anyString(), any(), anyLong(), anyString());
    }

    @Test
    @DisplayName("브랜드 이미지 삭제 성공")
    void deleteBrandImage_Success() {
        // given
        when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                .thenReturn(Optional.of(user));
        when(sellersRepository.findByUserId(anyString()))
                .thenReturn(Optional.of(seller));
        when(sellersRepository.save(any(Sellers.class)))
                .thenReturn(seller);

        // when
        SellerBrandImageResponseDTO result = sellerBrandImageService.deleteBrandImage(userPrincipal);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo("user-uuid-123");

        // S3에서 이미지 삭제 호출 확인
        verify(objectStorageService).deleteImage("old_brand_image.jpg");

        // 판매자 정보 저장 호출 확인 (이미지 URL이 null로 설정됨)
        ArgumentCaptor<Sellers> sellerCaptor = ArgumentCaptor.forClass(Sellers.class);
        verify(sellersRepository).save(sellerCaptor.capture());
    }

    @Test
    @DisplayName("브랜드 이미지 삭제 성공 - 기존 이미지가 없는 경우")
    void deleteBrandImage_Success_NoExistingImage() {
        // given
        seller = Sellers.builder()
                .userId("user-uuid-123")
                .user(user)
                .vendorName("멍멍이네 수제간식")
                .vendorProfileImage(null) // 기존 이미지 없음
                .businessNumber("123-45-67890")
                .build();

        when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                .thenReturn(Optional.of(user));
        when(sellersRepository.findByUserId(anyString()))
                .thenReturn(Optional.of(seller));
        when(sellersRepository.save(any(Sellers.class)))
                .thenReturn(seller);

        // when
        SellerBrandImageResponseDTO result = sellerBrandImageService.deleteBrandImage(userPrincipal);

        // then
        assertThat(result).isNotNull();

        // S3 삭제는 호출되지 않아야 함
        verify(objectStorageService, never()).deleteImage(anyString());

        // 판매자 정보 저장은 호출되어야 함
        verify(sellersRepository).save(seller);
    }

    @Test
    @DisplayName("이미지 파일 유효성 검증 실패 - null 파일")
    void uploadBrandImage_Fail_NullFile() {
        // when & then
        assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미지 파일이 비어있습니다.");
    }

    @Test
    @DisplayName("이미지 파일 유효성 검증 실패 - 빈 파일")
    void uploadBrandImage_Fail_EmptyFile() {
        // given
        MultipartFile emptyFile = new MockMultipartFile(
                "image", "empty.jpg", "image/jpeg", new byte[0]
        );

        // when & then
        assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미지 파일이 비어있습니다.");
    }

    @Test
    @DisplayName("이미지 파일 유효성 검증 실패 - 파일 크기 초과")
    void uploadBrandImage_Fail_FileSizeExceeded() {
        // given - 11MB 파일 생성
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MultipartFile largeFile = new MockMultipartFile(
                "image", "large.jpg", "image/jpeg", largeContent
        );

        // when & then
        assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, largeFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미지 파일 크기는 10MB를 초과할 수 없습니다.");
    }

    @Test
    @DisplayName("이미지 파일 유효성 검증 실패 - 지원하지 않는 파일 형식")
    void uploadBrandImage_Fail_UnsupportedFileType() {
        // given
        MultipartFile textFile = new MockMultipartFile(
                "file", "document.txt", "text/plain", "text content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, textFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미지 파일만 업로드 가능합니다.");
    }

    @Test
    @DisplayName("이미지 파일 유효성 검증 실패 - 지원하지 않는 이미지 형식")
    void uploadBrandImage_Fail_UnsupportedImageFormat() {
        // given
        MultipartFile gifFile = new MockMultipartFile(
                "image", "animated.gif", "image/gif", "gif content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, gifFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 이미지 형식입니다. (JPEG, PNG, JPG, WebP만 지원)");
    }

    @Test
    @DisplayName("사용자 조회 실패")
    void uploadBrandImage_Fail_UserNotFound() {
        // given
        when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("판매자 조회 실패")
    void uploadBrandImage_Fail_SellerNotFound() {
        // given
        when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                .thenReturn(Optional.of(user));
        when(sellersRepository.findByUserId(anyString()))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("판매자 정보를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("S3 업로드 실패로 인한 예외")
    void uploadBrandImage_Fail_S3UploadError() throws IOException {
        // given
        when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                .thenReturn(Optional.of(user));
        when(sellersRepository.findByUserId(anyString()))
                .thenReturn(Optional.of(seller));
        when(objectStorageService.uploadImage(anyString(), any(), anyLong(), anyString()))
                .thenThrow(new RuntimeException("S3 업로드 실패"));

        // when & then
        assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미지 업로드 실패");
    }

    @Test
    @DisplayName("기존 이미지 삭제 실패해도 업로드는 계속 진행")
    void uploadBrandImage_Success_IgnoreDeleteError() throws IOException {
        // given
        String newImageUrl = "https://cdn.example.com/images/brand_user-uuid_abc123.jpg";

        when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                .thenReturn(Optional.of(user));
        when(sellersRepository.findByUserId(anyString()))
                .thenReturn(Optional.of(seller));
        doThrow(new RuntimeException("S3 삭제 실패"))
                .when(objectStorageService).deleteImage("old_brand_image.jpg");
        when(objectStorageService.uploadImage(anyString(), any(), anyLong(), anyString()))
                .thenReturn(newImageUrl);
        when(sellersRepository.save(any(Sellers.class)))
                .thenReturn(seller);

        // when
        SellerBrandImageResponseDTO result = sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile);

        // then
        assertThat(result).isNotNull();

        // 기존 이미지 삭제 시도는 했지만 실패
        verify(objectStorageService).deleteImage("old_brand_image.jpg");

        // 새 이미지 업로드는 성공
        verify(objectStorageService).uploadImage(anyString(), any(), anyLong(), anyString());

        // 최종 저장도 성공
        verify(sellersRepository).save(seller);
    }

    @Test
    @DisplayName("지원하는 모든 이미지 형식 테스트")
    void uploadBrandImage_Success_AllSupportedFormats() throws IOException {
        // given
        String newImageUrl = "https://cdn.example.com/images/brand_user-uuid_abc123.jpg";

        when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                .thenReturn(Optional.of(user));
        when(sellersRepository.findByUserId(anyString()))
                .thenReturn(Optional.of(seller));
        when(objectStorageService.uploadImage(anyString(), any(), anyLong(), anyString()))
                .thenReturn(newImageUrl);
        when(sellersRepository.save(any(Sellers.class)))
                .thenReturn(seller);

        // 지원하는 모든 형식 테스트
        String[] supportedTypes = {"image/jpeg", "image/png", "image/jpg", "image/webp"};
        String[] extensions = {"jpeg", "png", "jpg", "webp"};

        for (int i = 0; i < supportedTypes.length; i++) {
            // given
            MultipartFile imageFile = new MockMultipartFile(
                    "image",
                    "test." + extensions[i],
                    supportedTypes[i],
                    "test content".getBytes()
            );

            // when & then (예외가 발생하지 않아야 함)
            SellerBrandImageResponseDTO result = sellerBrandImageService.uploadBrandImage(userPrincipal, imageFile);
            assertThat(result).isNotNull();
        }

        // 모든 형식에 대해 업로드가 호출되었는지 확인
        verify(objectStorageService, times(4)).uploadImage(anyString(), any(), anyLong(), anyString());
    }

    @Test
    @DisplayName("파일명에서 확장자 추출 테스트")
    void extractFileExtension_Test() {
        // private 메서드이므로 실제로는 업로드를 통해 간접적으로 테스트
        // given
        when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                .thenReturn(Optional.of(user));
        when(sellersRepository.findByUserId(anyString()))
                .thenReturn(Optional.of(seller));
        when(objectStorageService.uploadImage(anyString(), any(), anyLong(), anyString()))
                .thenReturn("test-url");
        when(sellersRepository.save(any(Sellers.class)))
                .thenReturn(seller);

        MultipartFile pngFile = new MockMultipartFile(
                "image", "test.PNG", "image/png", "content".getBytes()
        );

        // when
        sellerBrandImageService.uploadBrandImage(userPrincipal, pngFile);

        // then
        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(objectStorageService).uploadImage(fileNameCaptor.capture(), any(), anyLong(), anyString());

        String capturedFileName = fileNameCaptor.getValue();
        assertThat(capturedFileName).endsWith(".png"); // 소문자로 변환되어야 함
        assertThat(capturedFileName).matches("brand_123_[a-f0-9]{32}\\.png"); // PNG 확장자 확인
    }
}