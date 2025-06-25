package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.storage.service.ObjectStorageService;
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
@DisplayName("SellerBrandImageServiceImpl 테스트")
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
            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");

            verify(userRepository).findByProviderAndProviderId("google", "12345");
            verifyNoInteractions(sellersRepository, objectStorageService);
        }

        @Test
        @DisplayName("실패: 판매자 정보를 찾을 수 없음")
        void uploadBrandImage_SellerNotFound() {
            // given
            when(userRepository.findByProviderAndProviderId("google", "12345"))
                    .thenReturn(Optional.of(user));
            when(sellersRepository.findByUserId("user-uuid-123"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, validImageFile))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("판매자 정보를 찾을 수 없습니다");

            verify(userRepository).findByProviderAndProviderId("google", "12345");
            verify(sellersRepository).findByUserId("user-uuid-123");
            verifyNoInteractions(objectStorageService);
        }

        @Test
        @DisplayName("실패: 이미지 파일이 null")
        void uploadBrandImage_NullFile() {
            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미지 파일이 비어있습니다.");

            verifyNoInteractions(userRepository, sellersRepository, objectStorageService);
        }

        @Test
        @DisplayName("실패: 이미지 파일이 비어있음")
        void uploadBrandImage_EmptyFile() {
            // given
            MultipartFile emptyFile = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, emptyFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미지 파일이 비어있습니다.");

            verifyNoInteractions(userRepository, sellersRepository, objectStorageService);
        }

        @Test
        @DisplayName("실패: 파일 크기가 10MB 초과")
        void uploadBrandImage_FileSizeExceeded() {
            // given
            byte[] largeFile = new byte[11 * 1024 * 1024]; // 11MB
            largeFile[0] = (byte) 0xFF; largeFile[1] = (byte) 0xD8; largeFile[2] = (byte) 0xFF; // JPEG header

            MultipartFile largeSizeFile = new MockMultipartFile("image", "large.jpg", "image/jpeg", largeFile);

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, largeSizeFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미지 파일 크기는 10MB를 초과할 수 없습니다.");

            verifyNoInteractions(userRepository, sellersRepository, objectStorageService);
        }

        @Test
        @DisplayName("실패: 지원하지 않는 파일 형식")
        void uploadBrandImage_UnsupportedFileType() {
            // given - PDF 파일 헤더 (25 50 44 46)
            byte[] pdfHeader = new byte[]{0x25, 0x50, 0x44, 0x46};
            MultipartFile unsupportedFile = new MockMultipartFile("image", "test.pdf", "application/pdf", pdfHeader);

            // when & then
            assertThatThrownBy(() -> sellerBrandImageService.uploadBrandImage(userPrincipal, unsupportedFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("지원하지 않는 이미지 형식입니다. (JPEG, PNG, WebP만 지원)");

            verifyNoInteractions(userRepository, sellersRepository, objectStorageService);
        }

        @Test
        @DisplayName("실패: S3 업로드 중 IOException 발생")
        void uploadBrandImage_S3UploadFailed() throws IOException {
            // given
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

            verify(userRepository).findByProviderAndProviderId("google", "12345");
            verify(sellersRepository).findByUserId("user-uuid-123");
            verify(objectStorageService).deleteImage("old_image.jpg");
            verify(objectStorageService).uploadImage(anyString(), any(InputStream.class), anyLong(), anyString());
            verify(sellersRepository, never()).save(any());
        }

        @Test
        @DisplayName("성공: PNG 파일 업로드")
        void uploadBrandImage_PngFile() throws IOException {
            // given - PNG magic number: 89 50 4E 47
            byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
            MultipartFile pngFile = new MockMultipartFile("image", "test.png", "image/png", pngHeader);
            String newImageUrl = "https://cdn.example.com/images/brand_user-uuid_new123.png";

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
            verify(objectStorageService).uploadImage(anyString(), any(InputStream.class), anyLong(), eq("image/png"));
        }

        @Test
        @DisplayName("성공: WebP 파일 업로드")
        void uploadBrandImage_WebPFile() throws IOException {
            // given - WebP magic number: RIFF....WEBP
            byte[] webpHeader = new byte[]{0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50};
            MultipartFile webpFile = new MockMultipartFile("image", "test.webp", "image/webp", webpHeader);
            String newImageUrl = "https://cdn.example.com/images/brand_user-uuid_new123.webp";

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
    @DisplayName("Private 메서드 간접 테스트")
    class PrivateMethodIndirectTest {

        @Test
        @DisplayName("파일명 생성 검증 - UUID 형식")
        void generateUniqueFileName_Verification() throws IOException {
            // given
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
            // given - 여러 다른 URL 형식으로 seller 설정
            Sellers sellerWithDifferentUrl = Sellers.builder()
                    .userId("user-uuid-123")
                    .user(user)
                    .vendorName("테스트 상점")
                    .vendorProfileImage("https://cdn.example.com/images/some_image_key.jpg")
                    .businessNumber("123-45-67890")
                    .build();

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
            // given - 짧은 userId로 새로운 user 생성
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
}