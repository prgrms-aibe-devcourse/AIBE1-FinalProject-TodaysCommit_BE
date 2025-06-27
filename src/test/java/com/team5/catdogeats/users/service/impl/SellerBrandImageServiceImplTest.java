package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.storage.domain.service.ObjectStorageService;
import com.team5.catdogeats.storage.util.ImageValidationUtil;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.SellerBrandImageResponseDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) 
@DisplayName("SellerBrandImageServiceImpl 테스트")
class SellerBrandImageServiceImplTest {

    @Mock
    private SellersRepository sellersRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private ImageValidationUtil imageValidationUtil;

    @InjectMocks
    private SellerBrandImageServiceImpl sellerBrandImageService;

    private UserPrincipal userPrincipal;
    private Users user;
    private Sellers seller;
    private MultipartFile validImageFile;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal("google", "12345");

        user = Users.builder()
                .id("user-uuid-123")
                .provider("google")
                .providerId("12345")
                .name("Test User")
                .role(Role.ROLE_SELLER)
                .userNameAttribute("sub")
                .build();

        seller = Sellers.builder()
                .userId("user-uuid-123")
                .user(user)
                .vendorName("테스트 상점")
                .vendorProfileImage("https://cdn.example.com/images/old_image.jpg")
                .businessNumber("123-45-67890")
                .build();

        // Valid JPEG file mock (JPEG magic number: FF D8 FF)
        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        validImageFile = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                jpegHeader
        );
    }

    @Nested
    @DisplayName("브랜드 이미지 업로드 테스트")
    class UploadBrandImageTest {

        @Test
        @DisplayName("성공: 새 브랜드 이미지 업로드")
        void uploadBrandImage_Success() throws IOException {
            // given
            String newImageUrl = "https://cdn.example.com/images/brand_user-uuid_new123.jpg";

            // ImageValidationUtil이 성공적으로 검증 완료 (예외 없음)
            doNothing().when(imageValidationUtil).validateImageFile(validImageFile);
            when(imageValidationUtil.getFileExtension(anyString())).thenReturn("jpg");

            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(seller));
            when(objectStorageService.uploadImage(anyString(), any(InputStream.class), anyLong(), anyString()))
                    .thenReturn(newImageUrl);

            Sellers updatedSeller = Sellers.builder()
                    .userId("user-uuid-123")
                    .user(user)
                    .vendorName("테스트 상점")
                    .vendorProfileImage(newImageUrl)
                    .businessNumber("123-45-67890")
                    .build();

            when(sellersRepository.save(any(Sellers.class))).thenReturn(updatedSeller);

            // when
            SellerBrandImageResponseDTO result = sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile);

            // then
            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo("user-uuid-123");
            assertThat(result.vendorName()).isEqualTo("테스트 상점");
            assertThat(result.vendorProfileImage()).isEqualTo(newImageUrl);

            // verify interactions
            verify(imageValidationUtil).validateImageFile(validImageFile); // 추가 검증
            verify(userRepository).findByProviderAndProviderId("google", "12345");
            verify(sellersRepository).findByUserId("user-uuid-123");
            verify(objectStorageService).deleteImage("old_image.jpg"); // 기존 이미지 삭제
            verify(objectStorageService).uploadImage(anyString(), any(InputStream.class), anyLong(), eq("image/jpeg"));
            verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("실패: 사용자를 찾을 수 없음")
        void uploadBrandImage_UserNotFound() {
            // given
            doNothing().when(imageValidationUtil).validateImageFile(validImageFile);
            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");

            verify(imageValidationUtil).validateImageFile(validImageFile);
            verify(userRepository).findByProviderAndProviderId("google", "12345");
            verifyNoInteractions(sellersRepository, objectStorageService);
        }

        @Test
        @DisplayName("실패: 판매자 정보를 찾을 수 없음")
        void uploadBrandImage_SellerNotFound() {
            // given
            doNothing().when(imageValidationUtil).validateImageFile(validImageFile);
            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("판매자 정보를 찾을 수 없습니다");

            verify(imageValidationUtil).validateImageFile(validImageFile);
            verify(userRepository).findByProviderAndProviderId("google", "12345");
            verify(sellersRepository).findByUserId("user-uuid-123");
            verifyNoInteractions(objectStorageService);
        }

        @Test
        @DisplayName("실패: ImageValidationUtil에서 파일 검증 실패 - null 파일")
        void uploadBrandImage_ValidationFailed_NullFile() {
            // given
            doThrow(new IllegalArgumentException("이미지 파일이 비어있습니다."))
                    .when(imageValidationUtil).validateImageFile(null);

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미지 파일이 비어있습니다.");

            verify(imageValidationUtil).validateImageFile(null);
            verifyNoInteractions(userRepository, sellersRepository, objectStorageService);
        }

        @Test
        @DisplayName("실패: ImageValidationUtil에서 파일 크기 초과")
        void uploadBrandImage_ValidationFailed_FileSizeExceeded() {
            // given - MockMultipartFile로 실제 큰 파일 생성
            byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
            MultipartFile largeFile = new MockMultipartFile("image", "large.jpg", "image/jpeg", largeContent);

            doThrow(new IllegalArgumentException("이미지 파일 크기는 10MB를 초과할 수 없습니다."))
                    .when(imageValidationUtil).validateImageFile(largeFile);

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, largeFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미지 파일 크기는 10MB를 초과할 수 없습니다.");

            verify(imageValidationUtil).validateImageFile(largeFile);
            verifyNoInteractions(userRepository, sellersRepository, objectStorageService);
        }

        @Test
        @DisplayName("실패: ImageValidationUtil에서 지원하지 않는 파일 형식")
        void uploadBrandImage_ValidationFailed_UnsupportedFormat() {
            // given
            MultipartFile unsupportedFile = new MockMultipartFile(
                    "image", "test.gif", "image/gif", new byte[]{0x47, 0x49, 0x46}); // GIF header

            doThrow(new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (JPEG, PNG, WebP만 지원)"))
                    .when(imageValidationUtil).validateImageFile(unsupportedFile);

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, unsupportedFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("지원하지 않는 이미지 형식입니다. (JPEG, PNG, WebP만 지원)");

            verify(imageValidationUtil).validateImageFile(unsupportedFile);
            verifyNoInteractions(userRepository, sellersRepository, objectStorageService);
        }

        @Test
        @DisplayName("실패: ImageValidationUtil에서 보안 검증 실패")
        void uploadBrandImage_ValidationFailed_SecurityCheck() {
            // given
            MultipartFile maliciousFile = new MockMultipartFile(
                    "image", "script.jpg", "image/jpeg", "<script>alert('xss')</script>".getBytes());

            doThrow(new IllegalArgumentException("보안상 위험한 스크립트가 포함된 파일은 업로드할 수 없습니다."))
                    .when(imageValidationUtil).validateImageFile(maliciousFile);

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, maliciousFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("보안상 위험한 스크립트가 포함된 파일은 업로드할 수 없습니다.");

            verify(imageValidationUtil).validateImageFile(maliciousFile);
            verifyNoInteractions(userRepository, sellersRepository, objectStorageService);
        }

        @Test
        @DisplayName("실패: S3 업로드 중 예외 발생")
        void uploadBrandImage_S3UploadFailed() throws IOException {
            // given
            doNothing().when(imageValidationUtil).validateImageFile(validImageFile);
            when(imageValidationUtil.getFileExtension(anyString())).thenReturn("jpg");

            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(seller));
            when(objectStorageService.uploadImage(anyString(), any(InputStream.class), anyLong(), anyString()))
                    .thenThrow(new RuntimeException("S3 업로드 실패"));

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("이미지 업로드 실패");

            verify(imageValidationUtil).validateImageFile(validImageFile);
            verify(userRepository).findByProviderAndProviderId("google", "12345");
            verify(sellersRepository).findByUserId("user-uuid-123");
            verify(objectStorageService).deleteImage("old_image.jpg");
            verify(objectStorageService).uploadImage(anyString(), any(InputStream.class), anyLong(), anyString());
            verify(sellersRepository, never()).save(any());
        }

        @Test
        @DisplayName("성공: PNG 파일 업로드")
        void uploadBrandImage_PngFile() throws IOException {
            // given
            MultipartFile pngFile = new MockMultipartFile("image", "test.png", "image/png",
                    new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
            String newImageUrl = "https://cdn.example.com/images/brand_user-uuid_new123.png";

            doNothing().when(imageValidationUtil).validateImageFile(pngFile);
            when(imageValidationUtil.getFileExtension(anyString())).thenReturn("png");

            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(seller));
            when(objectStorageService.uploadImage(anyString(), any(InputStream.class), anyLong(), anyString()))
                    .thenReturn(newImageUrl);
            when(sellersRepository.save(any(Sellers.class))).thenReturn(seller);

            // when
            SellerBrandImageResponseDTO result = sellerBrandImageService.uploadBrandImage(userPrincipal, pngFile);

            // then
            assertThat(result).isNotNull();
            verify(imageValidationUtil).validateImageFile(pngFile);
            verify(objectStorageService).uploadImage(anyString(), any(InputStream.class), anyLong(), eq("image/png"));
        }

        @Test
        @DisplayName("성공: WebP 파일 업로드")
        void uploadBrandImage_WebPFile() throws IOException {
            // given
            MultipartFile webpFile = new MockMultipartFile("image", "test.webp", "image/webp",
                    new byte[]{0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50});
            String newImageUrl = "https://cdn.example.com/images/brand_user-uuid_new123.webp";

            doNothing().when(imageValidationUtil).validateImageFile(webpFile);
            when(imageValidationUtil.getFileExtension(anyString())).thenReturn("webp");

            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(seller));
            when(objectStorageService.uploadImage(anyString(), any(InputStream.class), anyLong(), anyString()))
                    .thenReturn(newImageUrl);
            when(sellersRepository.save(any(Sellers.class))).thenReturn(seller);

            // when
            SellerBrandImageResponseDTO result = sellerBrandImageService.uploadBrandImage(userPrincipal, webpFile);

            // then
            assertThat(result).isNotNull();
            verify(imageValidationUtil).validateImageFile(webpFile);
            verify(objectStorageService).uploadImage(anyString(), any(InputStream.class), anyLong(), eq("image/webp"));
        }
    }

    @Nested
    @DisplayName("브랜드 이미지 삭제 테스트")
    class DeleteBrandImageTest {

        @Test
        @DisplayName("성공: 브랜드 이미지 삭제")
        void deleteBrandImage_Success() {
            // given
            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(seller));
            when(sellersRepository.deleteVendorProfileImage("user-uuid-123"))
                    .thenReturn(1);

            Sellers updatedSeller = Sellers.builder()
                    .userId("user-uuid-123")
                    .user(user)
                    .vendorName("테스트 상점")
                    .vendorProfileImage(null) // 이미지 삭제됨
                    .businessNumber("123-45-67890")
                    .build();

            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(seller))
                    .thenReturn(Optional.of(updatedSeller));

            // when
            SellerBrandImageResponseDTO result = sellerBrandImageService.deleteBrandImage(userPrincipal);

            // then
            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo("user-uuid-123");
            assertThat(result.vendorProfileImage()).isNull();

            verify(userRepository).findByProviderAndProviderId("google", "12345");
            verify(sellersRepository, times(2)).findByUserId("user-uuid-123");
            verify(objectStorageService).deleteImage("old_image.jpg");
            verify(sellersRepository).deleteVendorProfileImage("user-uuid-123");
        }

        @Test
        @DisplayName("성공: 삭제할 이미지가 없는 경우")
        void deleteBrandImage_NoExistingImage() {
            // given
            Sellers sellerWithoutImage = Sellers.builder()
                    .userId("user-uuid-123")
                    .user(user)
                    .vendorName("테스트 상점")
                    .vendorProfileImage(null)
                    .businessNumber("123-45-67890")
                    .build();

            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(sellerWithoutImage));
            when(sellersRepository.deleteVendorProfileImage("user-uuid-123"))
                    .thenReturn(1);

            // when
            SellerBrandImageResponseDTO result = sellerBrandImageService.deleteBrandImage(userPrincipal);

            // then
            assertThat(result).isNotNull();
            assertThat(result.vendorProfileImage()).isNull();

            verify(userRepository).findByProviderAndProviderId("google", "12345");
            verify(sellersRepository, times(2)).findByUserId("user-uuid-123");
            verify(objectStorageService, never()).deleteImage(anyString());
            verify(sellersRepository).deleteVendorProfileImage("user-uuid-123");
        }

        @Test
        @DisplayName("실패: 사용자를 찾을 수 없음")
        void deleteBrandImage_UserNotFound() {
            // given
            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.deleteBrandImage(userPrincipal))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");

            verify(userRepository).findByProviderAndProviderId("google", "12345");
            verifyNoInteractions(sellersRepository, objectStorageService);
        }

        @Test
        @DisplayName("실패: 판매자 정보를 찾을 수 없음")
        void deleteBrandImage_SellerNotFound() {
            // given
            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.deleteBrandImage(userPrincipal))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("판매자 정보를 찾을 수 없습니다");

            verify(userRepository).findByProviderAndProviderId("google", "12345");
            verify(sellersRepository).findByUserId("user-uuid-123");
            verifyNoInteractions(objectStorageService);
        }

        @Test
        @DisplayName("실패: DB 업데이트 실패")
        void deleteBrandImage_DatabaseUpdateFailed() {
            // given
            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(seller));
            when(sellersRepository.deleteVendorProfileImage("user-uuid-123"))
                    .thenReturn(0); // 업데이트된 행이 없음

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.deleteBrandImage(userPrincipal))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("브랜드 이미지 삭제에 실패했습니다.");

            verify(userRepository).findByProviderAndProviderId("google", "12345");
            verify(sellersRepository).findByUserId("user-uuid-123");
            verify(objectStorageService).deleteImage("old_image.jpg");
            verify(sellersRepository).deleteVendorProfileImage("user-uuid-123");
        }
    }

    @Nested
    @DisplayName("파일명 생성 및 키 추출 테스트")
    class FileHandlingTest {

        @Test
        @DisplayName("파일명 생성 검증 - UUID 형식")
        void generateUniqueFileName_Verification() throws IOException {
            // given
            doNothing().when(imageValidationUtil).validateImageFile(validImageFile);
            when(imageValidationUtil.getFileExtension("test.jpg")).thenReturn("jpg");

            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(seller));
            when(sellersRepository.save(any(Sellers.class))).thenReturn(seller);

            ArgumentCaptor<String> filenameCaptor = ArgumentCaptor.forClass(String.class);
            when(objectStorageService.uploadImage(filenameCaptor.capture(), any(InputStream.class), anyLong(), anyString()))
                    .thenReturn("https://cdn.example.com/images/generated_filename.jpg");

            // when
            sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile);

            // then
            String generatedFilename = filenameCaptor.getValue();
            // user-uuid-123에서 8자리 잘림: user-uui
            assertThat(generatedFilename).matches("brand_user-uui_[a-f0-9]{32}\\.jpg");
            assertThat(generatedFilename).startsWith("brand_user-uui");
            assertThat(generatedFilename).endsWith(".jpg");
        }

        @Test
        @DisplayName("이미지 키 추출 검증")
        void extractImageKeyFromUrl_Verification() throws IOException {
            // given
            Sellers sellerWithDifferentUrl = Sellers.builder()
                    .userId("user-uuid-123")
                    .user(user)
                    .vendorName("테스트 상점")
                    .vendorProfileImage("https://cdn.example.com/images/some_image_key.jpg")
                    .businessNumber("123-45-67890")
                    .build();

            doNothing().when(imageValidationUtil).validateImageFile(validImageFile);
            when(imageValidationUtil.getFileExtension("test.jpg")).thenReturn("jpg");

            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(sellerWithDifferentUrl));
            when(sellersRepository.save(any(Sellers.class))).thenReturn(sellerWithDifferentUrl);
            when(objectStorageService.uploadImage(anyString(), any(InputStream.class), anyLong(), anyString()))
                    .thenReturn("https://cdn.example.com/images/new_image.jpg");

            ArgumentCaptor<String> imageKeyCaptor = ArgumentCaptor.forClass(String.class);

            // when
            sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile);

            // then
            verify(objectStorageService).deleteImage(imageKeyCaptor.capture());
            String extractedKey = imageKeyCaptor.getValue();
            assertThat(extractedKey).isEqualTo("some_image_key.jpg");
        }

        @Test
        @DisplayName("8자리 이하 userId 파일명 생성 검증")
        void generateUniqueFileName_ShortUserId() throws IOException {
            // given
            Users shortUser = Users.builder()
                    .id("short123")  // 8자리 이하
                    .provider("google")
                    .providerId("12345")
                    .name("Test User")
                    .role(Role.ROLE_SELLER)
                    .userNameAttribute("sub")
                    .build();

            Sellers shortSeller = Sellers.builder()
                    .userId("short123")
                    .user(shortUser)
                    .vendorName("테스트 상점")
                    .vendorProfileImage("https://cdn.example.com/images/old_image.jpg")
                    .businessNumber("123-45-67890")
                    .build();

            doNothing().when(imageValidationUtil).validateImageFile(validImageFile);
            when(imageValidationUtil.getFileExtension("test.jpg")).thenReturn("jpg");

            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(shortUser));
            when(sellersRepository.findByUserId("short123"))
                    .thenReturn(Optional.of(shortSeller));
            when(sellersRepository.save(any(Sellers.class))).thenReturn(shortSeller);

            ArgumentCaptor<String> filenameCaptor = ArgumentCaptor.forClass(String.class);
            when(objectStorageService.uploadImage(filenameCaptor.capture(), any(InputStream.class), anyLong(), anyString()))
                    .thenReturn("https://cdn.example.com/images/generated_filename.jpg");

            // when
            sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile);

            // then
            String generatedFilename = filenameCaptor.getValue();
            // 짧은 userId는 그대로 사용됨
            assertThat(generatedFilename).matches("brand_short123_[a-f0-9]{32}\\.jpg");
            assertThat(generatedFilename).startsWith("brand_short123");
            assertThat(generatedFilename).endsWith(".jpg");
        }
    }

    @Nested
    @DisplayName("ImageValidationUtil 통합 테스트")
    class ImageValidationUtilIntegrationTest {

        @Test
        @DisplayName("getFileExtension 메서드 호출 검증")
        void getFileExtension_Called() throws IOException {
            // given
            doNothing().when(imageValidationUtil).validateImageFile(validImageFile);
            when(imageValidationUtil.getFileExtension("test.jpg")).thenReturn("jpg");

            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(seller));
            when(sellersRepository.save(any(Sellers.class))).thenReturn(seller);
            when(objectStorageService.uploadImage(anyString(), any(InputStream.class), anyLong(), anyString()))
                    .thenReturn("https://cdn.example.com/images/new_image.jpg");

            // when
            sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile);

            // then
            verify(imageValidationUtil).validateImageFile(validImageFile);
            verify(imageValidationUtil).getFileExtension("test.jpg");
        }

        @Test
        @DisplayName("다양한 확장자에 대한 getFileExtension 호출")
        void getFileExtension_DifferentExtensions() throws IOException {
            // given - PNG 파일
            MultipartFile pngFile = new MockMultipartFile("image", "test.png", "image/png", new byte[4]);

            doNothing().when(imageValidationUtil).validateImageFile(pngFile);
            when(imageValidationUtil.getFileExtension("test.png")).thenReturn("png");

            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(seller));
            when(sellersRepository.save(any(Sellers.class))).thenReturn(seller);
            when(objectStorageService.uploadImage(anyString(), any(InputStream.class), anyLong(), anyString()))
                    .thenReturn("https://cdn.example.com/images/new_image.png");

            // when
            sellerBrandImageService.uploadBrandImage(userPrincipal, pngFile);

            // then
            verify(imageValidationUtil).validateImageFile(pngFile);
            verify(imageValidationUtil).getFileExtension("test.png");
        }

        @Test
        @DisplayName("파일명이 null일 때 getFileExtension 호출")
        void getFileExtension_NullFileName() throws IOException {
            // given - 파일명이 null인 경우
            MultipartFile nullNameFile = new MockMultipartFile("image", null, "image/jpeg", new byte[4]);

            doNothing().when(imageValidationUtil).validateImageFile(nullNameFile);

            when(imageValidationUtil.getFileExtension(anyString())).thenReturn("jpg");

            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(seller));
            when(sellersRepository.save(any(Sellers.class))).thenReturn(seller);
            when(objectStorageService.uploadImage(anyString(), any(InputStream.class), anyLong(), anyString()))
                    .thenReturn("https://cdn.example.com/images/new_image.jpg");

            // when
            sellerBrandImageService.uploadBrandImage(userPrincipal, nullNameFile);

            // then
            verify(imageValidationUtil).validateImageFile(nullNameFile);
            verify(imageValidationUtil).getFileExtension(anyString());
        }

        @Test
        @DisplayName("ImageValidationUtil에서 예외 발생 시 전파 확인")
        void validation_ExceptionPropagation() {
            // given
            RuntimeException validationException = new IllegalArgumentException("Custom validation error");
            doThrow(validationException).when(imageValidationUtil).validateImageFile(validImageFile);

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Custom validation error");

            verify(imageValidationUtil).validateImageFile(validImageFile);
            verifyNoInteractions(userRepository, sellersRepository, objectStorageService);
        }
    }

    @Nested
    @DisplayName("Service 레이어 경계 테스트")
    class ServiceBoundaryTest {

        @Test
        @DisplayName("모든 의존성 호출 순서 검증")
        void verifyCallOrder() throws IOException {
            // given
            doNothing().when(imageValidationUtil).validateImageFile(validImageFile);
            when(imageValidationUtil.getFileExtension("test.jpg")).thenReturn("jpg");

            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(seller));
            when(sellersRepository.save(any(Sellers.class))).thenReturn(seller);
            when(objectStorageService.uploadImage(anyString(), any(InputStream.class), anyLong(), anyString()))
                    .thenReturn("https://cdn.example.com/images/new_image.jpg");

            // when
            sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile);

            // then - 호출 순서 검증
            var inOrder = inOrder(imageValidationUtil, userRepository, sellersRepository, objectStorageService);

            inOrder.verify(imageValidationUtil).validateImageFile(validImageFile);
            inOrder.verify(userRepository).findByProviderAndProviderId("google", "12345");
            inOrder.verify(sellersRepository).findByUserId("user-uuid-123");
            inOrder.verify(objectStorageService).deleteImage("old_image.jpg");
            inOrder.verify(imageValidationUtil).getFileExtension("test.jpg");
            inOrder.verify(objectStorageService).uploadImage(anyString(), any(InputStream.class), anyLong(), anyString());
            inOrder.verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("중간 단계 실패 시 후속 작업 미실행 확인")
        void verifyNoSubsequentCallsOnFailure() throws IOException {
            // given - 사용자 조회에서 실패
            doNothing().when(imageValidationUtil).validateImageFile(validImageFile);
            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile))
                    .isInstanceOf(EntityNotFoundException.class);

            // then - 실패 후 호출되지 않아야 하는 메서드들 검증
            verify(imageValidationUtil).validateImageFile(validImageFile);
            verify(userRepository).findByProviderAndProviderId("google", "12345");

            // 이후 작업들은 호출되지 않아야 함
            verifyNoInteractions(sellersRepository, objectStorageService);
            verify(imageValidationUtil, never()).getFileExtension(anyString());
        }

        @Test
        @DisplayName("S3 업로드 실패 시 rollback 동작 확인")
        void verifyRollbackOnS3Failure() throws IOException {
            // given
            doNothing().when(imageValidationUtil).validateImageFile(validImageFile);
            when(imageValidationUtil.getFileExtension("test.jpg")).thenReturn("jpg");

            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.of(seller));

            // S3 업로드에서 실패
            when(objectStorageService.uploadImage(anyString(), any(InputStream.class), anyLong(), anyString()))
                    .thenThrow(new RuntimeException("S3 connection failed"));

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("이미지 업로드 실패");

            // then - 기존 이미지는 삭제되었지만, DB는 저장되지 않아야 함
            verify(objectStorageService).deleteImage("old_image.jpg"); // 기존 이미지 삭제는 실행됨
            verify(objectStorageService).uploadImage(anyString(), any(InputStream.class), anyLong(), anyString());
            verify(sellersRepository, never()).save(any(Sellers.class)); // DB 저장은 실행되지 않음
        }
    }
}
