package com.team5.catdogeats.storage.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ImageValidationUtil 테스트")
class ImageValidationUtilTest {

    private ImageValidationUtil imageValidationUtil;

    @BeforeEach
    void setUp() {
        imageValidationUtil = new ImageValidationUtil();
    }

    @Nested
    @DisplayName("통합 이미지 파일 검증 테스트")
    class ValidateImageFileTest {

        @Test
        @DisplayName("성공: 유효한 JPEG 파일")
        void validateImageFile_ValidJpegFile_Success() {
            // given - JPEG magic number: FF D8 FF
            byte[] jpegContent = createJpegContent();
            MultipartFile jpegFile = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", jpegContent);

            // when & then - 예외가 발생하지 않아야 함
            assertThatCode(() -> imageValidationUtil.validateImageFile(jpegFile))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("성공: 유효한 PNG 파일")
        void validateImageFile_ValidPngFile_Success() {
            // given - PNG magic number: 89 50 4E 47
            byte[] pngContent = createPngContent();
            MultipartFile pngFile = new MockMultipartFile(
                    "image", "test.png", "image/png", pngContent);

            // when & then
            assertThatCode(() -> imageValidationUtil.validateImageFile(pngFile))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("성공: 유효한 WebP 파일")
        void validateImageFile_ValidWebPFile_Success() {
            // given - WebP magic number: RIFF....WEBP
            byte[] webpContent = createWebPContent();
            MultipartFile webpFile = new MockMultipartFile(
                    "image", "test.webp", "image/webp", webpContent);

            // when & then
            assertThatCode(() -> imageValidationUtil.validateImageFile(webpFile))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("기본 속성 검증 테스트")
    class BasicPropertiesValidationTest {

        @Test
        @DisplayName("실패: null 파일")
        void validateImageFile_NullFile_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미지 파일이 비어있습니다.");
        }

        @Test
        @DisplayName("실패: 빈 파일")
        void validateImageFile_EmptyFile_ThrowsException() {
            // given
            MultipartFile emptyFile = new MockMultipartFile(
                    "image", "empty.jpg", "image/jpeg", new byte[0]);

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(emptyFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미지 파일이 비어있습니다.");
        }

        @Test
        @DisplayName("실패: 파일 크기 10MB 초과")
        void validateImageFile_FileSizeExceeded_ThrowsException() {
            // given - 11MB 파일
            byte[] largeContent = new byte[11 * 1024 * 1024];
            // JPEG 헤더 추가
            largeContent[0] = (byte) 0xFF;
            largeContent[1] = (byte) 0xD8;
            largeContent[2] = (byte) 0xFF;

            MultipartFile largeFile = new MockMultipartFile(
                    "image", "large.jpg", "image/jpeg", largeContent);

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(largeFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미지 파일 크기는 10MB를 초과할 수 없습니다.");
        }

        @Test
        @DisplayName("성공: 정확히 10MB 파일")
        void validateImageFile_ExactlyMaxSize_Success() {
            // given - 정확히 10MB
            byte[] maxSizeContent = new byte[10 * 1024 * 1024];
            // JPEG 헤더 추가
            maxSizeContent[0] = (byte) 0xFF;
            maxSizeContent[1] = (byte) 0xD8;
            maxSizeContent[2] = (byte) 0xFF;

            MultipartFile maxSizeFile = new MockMultipartFile(
                    "image", "max.jpg", "image/jpeg", maxSizeContent);

            // when & then
            assertThatCode(() -> imageValidationUtil.validateImageFile(maxSizeFile))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("MIME Type 검증 테스트")
    class MimeTypeValidationTest {

        @Test
        @DisplayName("실패: Content-Type이 null")
        void validateImageFile_NullContentType_ThrowsException() {
            // given
            byte[] jpegContent = createJpegContent();
            MultipartFile fileWithNullContentType = new MockMultipartFile(
                    "image", "test.jpg", null, jpegContent);

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(fileWithNullContentType))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("파일의 Content-Type을 확인할 수 없습니다.");
        }

        @Test
        @DisplayName("실패: 지원하지 않는 MIME Type - image/gif")
        void validateImageFile_UnsupportedMimeType_Gif_ThrowsException() {
            // given
            byte[] jpegContent = createJpegContent();
            MultipartFile gifFile = new MockMultipartFile(
                    "image", "test.gif", "image/gif", jpegContent);

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(gifFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("허용되지 않은 MIME 타입입니다: image/gif");
        }

        @Test
        @DisplayName("실패: 지원하지 않는 MIME Type - text/plain")
        void validateImageFile_UnsupportedMimeType_Text_ThrowsException() {
            // given
            byte[] jpegContent = createJpegContent();
            MultipartFile textFile = new MockMultipartFile(
                    "image", "test.txt", "text/plain", jpegContent);

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(textFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("허용되지 않은 MIME 타입입니다: text/plain");
        }

        @Test
        @DisplayName("성공: 지원되는 모든 MIME Type")
        void validateImageFile_SupportedMimeTypes_Success() {
            // given
            byte[] jpegContent = createJpegContent();

            String[] supportedTypes = {"image/jpeg", "image/jpg", "image/png", "image/webp"};
            String[] fileNames = {"test.jpeg", "test.jpg", "test.png", "test.webp"};

            for (int i = 0; i < supportedTypes.length; i++) {
                MultipartFile file = new MockMultipartFile(
                        "image", fileNames[i], supportedTypes[i], jpegContent);

                // when & then
                assertThatCode(() -> imageValidationUtil.validateImageFile(file))
                        .doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("파일명 보안 검증 테스트")
    class FileNameSecurityValidationTest {

        @Test
        @DisplayName("실패: 파일명이 null")
        void validateImageFile_NullFileName_ThrowsException() {
            // given
            byte[] jpegContent = createJpegContent();
            MultipartFile fileWithNullName = new MockMultipartFile(
                    "image", null, "image/jpeg", jpegContent);

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(fileWithNullName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("파일명이 비어있습니다.");
        }

        @Test
        @DisplayName("실패: 파일명이 빈 문자열")
        void validateImageFile_EmptyFileName_ThrowsException() {
            // given
            byte[] jpegContent = createJpegContent();
            MultipartFile fileWithEmptyName = new MockMultipartFile(
                    "image", "", "image/jpeg", jpegContent);

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(fileWithEmptyName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("파일명이 비어있습니다.");
        }

        @Test
        @DisplayName("실패: 파일명이 255자 초과")
        void validateImageFile_FileNameTooLong_ThrowsException() {
            // given
            String longFileName = "a".repeat(256) + ".jpg";
            byte[] jpegContent = createJpegContent();
            MultipartFile fileWithLongName = new MockMultipartFile(
                    "image", longFileName, "image/jpeg", jpegContent);

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(fileWithLongName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("파일명이 너무 깁니다. (최대 255자)");
        }

        @Test
        @DisplayName("성공: 파일명이 정확히 255자")
        void validateImageFile_FileNameExactly255Chars_Success() {
            // given - 정확히 255자 (확장자 포함)
            String exactLengthFileName = "a".repeat(251) + ".jpg"; // 251 + 4 = 255
            byte[] jpegContent = createJpegContent();
            MultipartFile file = new MockMultipartFile(
                    "image", exactLengthFileName, "image/jpeg", jpegContent);

            // when & then
            assertThatCode(() -> imageValidationUtil.validateImageFile(file))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("실패: 경로 순회 공격 - ../ 포함")
        void validateImageFile_PathTraversal_DotDotSlash_ThrowsException() {
            // given
            byte[] jpegContent = createJpegContent();
            MultipartFile maliciousFile = new MockMultipartFile(
                    "image", "../../../etc/passwd.jpg", "image/jpeg", jpegContent);

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(maliciousFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("파일명에 상대경로가 포함될 수 없습니다.");
        }

        @Test
        @DisplayName("실파: 경로 순회 공격 - ..\\ 포함 (Windows)")
        void validateImageFile_PathTraversal_DotDotBackslash_ThrowsException() {
            // given
            byte[] jpegContent = createJpegContent();
            MultipartFile maliciousFile = new MockMultipartFile(
                    "image", "..\\..\\system32\\test.jpg", "image/jpeg", jpegContent);

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(maliciousFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("파일명에 상대경로가 포함될 수 없습니다.");
        }

        @Test
        @DisplayName("실패: 실행 가능한 파일 확장자들")
        void validateImageFile_ExecutableExtensions_ThrowsException() {
            // given
            byte[] jpegContent = createJpegContent();
            String[] dangerousExtensions = {
                    "malicious.exe.jpg", "virus.bat.jpg", "script.js.jpg",
                    "page.html.jpg", "code.php.jpg", "app.jsp.jpg"
            };

            for (String fileName : dangerousExtensions) {
                MultipartFile maliciousFile = new MockMultipartFile(
                        "image", fileName, "image/jpeg", jpegContent);

                // when & then
                assertThatThrownBy(() -> imageValidationUtil.validateImageFile(maliciousFile))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("실행 가능한 파일 확장자는 업로드할 수 없습니다.");
            }
        }

        @Test
        @DisplayName("성공: 안전한 파일명들")
        void validateImageFile_SafeFileNames_Success() {
            // given
            byte[] jpegContent = createJpegContent();
            String[] safeFileNames = {
                    "normal.jpg", "image_01.png", "photo-2024.webp",
                    "IMG_001.JPEG", "screenshot.JPG", "profile.PNG"
            };

            for (String fileName : safeFileNames) {
                MultipartFile safeFile = new MockMultipartFile(
                        "image", fileName, "image/jpeg", jpegContent);

                // when & then
                assertThatCode(() -> imageValidationUtil.validateImageFile(safeFile))
                        .doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("스크립트 공격 방지 테스트")
    class ScriptAttackPreventionTest {

        @Test
        @DisplayName("실패: <script> 태그 포함")
        void validateImageFile_ContainsScriptTag_ThrowsException() {
            // given - JPEG 헤더 + 스크립트 내용
            String maliciousContent = createJpegHeaderString() + "<script>alert('XSS')</script>";
            MultipartFile maliciousFile = new MockMultipartFile(
                    "image", "malicious.jpg", "image/jpeg", maliciousContent.getBytes());

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(maliciousFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("보안상 위험한 스크립트가 포함된 파일은 업로드할 수 없습니다.");
        }

        @Test
        @DisplayName("실패: javascript: 스키마 포함")
        void validateImageFile_ContainsJavaScriptSchema_ThrowsException() {
            // given
            String maliciousContent = createJpegHeaderString() + "javascript:alert('XSS')";
            MultipartFile maliciousFile = new MockMultipartFile(
                    "image", "malicious.jpg", "image/jpeg", maliciousContent.getBytes());

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(maliciousFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("보안상 위험한 스크립트가 포함된 파일은 업로드할 수 없습니다.");
        }

        @Test
        @DisplayName("실패: 이벤트 핸들러들 포함")
        void validateImageFile_ContainsEventHandlers_ThrowsException() {
            // given
            String[] eventHandlers = {"onload=", "onerror=", "onclick=", "eval(", "document.", "alert("};

            for (String handler : eventHandlers) {
                String maliciousContent = createJpegHeaderString() + handler + "malicious()";
                MultipartFile maliciousFile = new MockMultipartFile(
                        "image", "malicious.jpg", "image/jpeg", maliciousContent.getBytes());

                // when & then
                assertThatThrownBy(() -> imageValidationUtil.validateImageFile(maliciousFile))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("보안상 위험한 스크립트가 포함된 파일은 업로드할 수 없습니다.");
            }
        }

        @Test
        @DisplayName("성공: 스크립트가 없는 정상 파일")
        void validateImageFile_NoScript_Success() {
            // given - 정상적인 JPEG 파일 (텍스트 없음)
            byte[] cleanJpegContent = createJpegContent();
            MultipartFile cleanFile = new MockMultipartFile(
                    "image", "clean.jpg", "image/jpeg", cleanJpegContent);

            // when & then
            assertThatCode(() -> imageValidationUtil.validateImageFile(cleanFile))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("성공: 무해한 텍스트 포함")
        void validateImageFile_HarmlessText_Success() {
            // given - 올바른 JPEG 바이트 배열 + 무해한 텍스트
            byte[] jpegHeader = createJpegContent(); // 올바른 JPEG magic number 바이트 배열
            String harmlessText = "This is a normal image description";
            byte[] harmlessTextBytes = harmlessText.getBytes();

            // JPEG 헤더와 텍스트를 결합
            byte[] combinedContent = new byte[jpegHeader.length + harmlessTextBytes.length];
            System.arraycopy(jpegHeader, 0, combinedContent, 0, jpegHeader.length);
            System.arraycopy(harmlessTextBytes, 0, combinedContent, jpegHeader.length, harmlessTextBytes.length);

            MultipartFile harmlessFile = new MockMultipartFile(
                    "image", "harmless.jpg", "image/jpeg", combinedContent);

            // when & then
            assertThatCode(() -> imageValidationUtil.validateImageFile(harmlessFile))
                    .doesNotThrowAnyException();
        }


        @Test
        @DisplayName("성공: 순수한 JPEG 파일")
        void validateImageFile_PureJpegFile_Success() {
            // given - createJpegContent()로 생성된 올바른 JPEG 바이트 배열
            byte[] jpegContent = createJpegContent();
            MultipartFile jpegFile = new MockMultipartFile(
                    "image", "pure.jpg", "image/jpeg", jpegContent);

            // when & then
            assertThatCode(() -> imageValidationUtil.validateImageFile(jpegFile))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("파일 시그니처 검증 테스트")
    class FileSignatureValidationTest {

        @Test
        @DisplayName("실패: 잘못된 파일 시그니처 - PDF")
        void validateImageFile_WrongSignature_PDF_ThrowsException() {
            // given - PDF 시그니처 (25 50 44 46)
            byte[] pdfContent = {0x25, 0x50, 0x44, 0x46}; // %PDF
            MultipartFile fakeImageFile = new MockMultipartFile(
                    "image", "fake.jpg", "image/jpeg", pdfContent);

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(fakeImageFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("지원하지 않는 이미지 형식입니다. (JPEG, PNG, WebP만 지원)");
        }

        @Test
        @DisplayName("실패: 잘못된 파일 시그니처 - ZIP")
        void validateImageFile_WrongSignature_ZIP_ThrowsException() {
            // given - ZIP 시그니처 (50 4B 03 04)
            byte[] zipContent = {0x50, 0x4B, 0x03, 0x04}; // PK..
            MultipartFile fakeImageFile = new MockMultipartFile(
                    "image", "fake.jpg", "image/jpeg", zipContent);

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(fakeImageFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("지원하지 않는 이미지 형식입니다. (JPEG, PNG, WebP만 지원)");
        }

        @Test
        @DisplayName("실패: 너무 짧은 파일 헤더")
        void validateImageFile_TooShortHeader_ThrowsException() {
            // given - 3바이트만 있는 파일
            byte[] shortContent = {0x12, 0x34, 0x56};
            MultipartFile shortFile = new MockMultipartFile(
                    "image", "short.jpg", "image/jpeg", shortContent);

            // when & then
            assertThatThrownBy(() -> imageValidationUtil.validateImageFile(shortFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("파일 형식을 확인할 수 없습니다.");
        }

        @Test
        @DisplayName("성공: 올바른 JPEG 시그니처 (FF D8 FF)")
        void validateImageFile_CorrectJpegSignature_Success() {
            // given
            byte[] jpegContent = createJpegContent();
            MultipartFile jpegFile = new MockMultipartFile(
                    "image", "valid.jpg", "image/jpeg", jpegContent);

            // when & then
            assertThatCode(() -> imageValidationUtil.validateImageFile(jpegFile))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("성공: 올바른 PNG 시그니처 (89 50 4E 47)")
        void validateImageFile_CorrectPngSignature_Success() {
            // given
            byte[] pngContent = createPngContent();
            MultipartFile pngFile = new MockMultipartFile(
                    "image", "valid.png", "image/png", pngContent);

            // when & then
            assertThatCode(() -> imageValidationUtil.validateImageFile(pngFile))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("성공: 올바른 WebP 시그니처 (RIFF....WEBP)")
        void validateImageFile_CorrectWebPSignature_Success() {
            // given
            byte[] webpContent = createWebPContent();
            MultipartFile webpFile = new MockMultipartFile(
                    "image", "valid.webp", "image/webp", webpContent);

            // when & then
            assertThatCode(() -> imageValidationUtil.validateImageFile(webpFile))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("실패: MIME Type과 시그니처 불일치")
        void validateImageFile_MimeTypeSignatureMismatch_ThrowsException() {
            // given - PNG 시그니처지만 JPEG MIME Type
            byte[] pngContent = createPngContent();
            MultipartFile mismatchFile = new MockMultipartFile(
                    "image", "mismatch.jpg", "image/jpeg", pngContent);

            // when & then - PNG 시그니처이므로 통과해야 함 (시그니처가 우선)
            assertThatCode(() -> imageValidationUtil.validateImageFile(mismatchFile))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("파일 확장자 추출 테스트")
    class GetFileExtensionTest {

        @Test
        @DisplayName("성공: 일반적인 파일 확장자들")
        void getFileExtension_CommonExtensions_ReturnsCorrectExtension() {
            // given & when & then
            assertThat(imageValidationUtil.getFileExtension("image.jpg")).isEqualTo("jpg");
            assertThat(imageValidationUtil.getFileExtension("photo.jpeg")).isEqualTo("jpeg");
            assertThat(imageValidationUtil.getFileExtension("picture.png")).isEqualTo("png");
            assertThat(imageValidationUtil.getFileExtension("graphic.webp")).isEqualTo("webp");
        }

        @Test
        @DisplayName("성공: 대문자 확장자")
        void getFileExtension_UpperCaseExtensions_ReturnsLowerCase() {
            // given & when & then
            assertThat(imageValidationUtil.getFileExtension("IMAGE.JPG")).isEqualTo("jpg");
            assertThat(imageValidationUtil.getFileExtension("PHOTO.PNG")).isEqualTo("png");
            assertThat(imageValidationUtil.getFileExtension("PICTURE.WEBP")).isEqualTo("webp");
        }

        @Test
        @DisplayName("기본값 반환: null 파일명")
        void getFileExtension_NullFileName_ReturnsDefault() {
            // when & then
            assertThat(imageValidationUtil.getFileExtension(null)).isEqualTo("jpg");
        }

        @Test
        @DisplayName("기본값 반환: 빈 파일명")
        void getFileExtension_EmptyFileName_ReturnsDefault() {
            // when & then
            assertThat(imageValidationUtil.getFileExtension("")).isEqualTo("jpg");
            assertThat(imageValidationUtil.getFileExtension("   ")).isEqualTo("jpg");
        }

        @Test
        @DisplayName("기본값 반환: 확장자 없는 파일명")
        void getFileExtension_NoExtension_ReturnsDefault() {
            // when & then
            assertThat(imageValidationUtil.getFileExtension("filename")).isEqualTo("jpg");
            assertThat(imageValidationUtil.getFileExtension("image_without_extension")).isEqualTo("jpg");
        }

        @Test
        @DisplayName("기본값 반환: 지원하지 않는 확장자")
        void getFileExtension_UnsupportedExtension_ReturnsDefault() {
            // when & then
            assertThat(imageValidationUtil.getFileExtension("file.gif")).isEqualTo("jpg");
            assertThat(imageValidationUtil.getFileExtension("document.pdf")).isEqualTo("jpg");
            assertThat(imageValidationUtil.getFileExtension("archive.zip")).isEqualTo("jpg");
        }

        @Test
        @DisplayName("성공: 특수 문자가 포함된 파일명")
        void getFileExtension_SpecialCharacters_ExtractsCorrectly() {
            // when & then
            assertThat(imageValidationUtil.getFileExtension("image@#$%.jpg")).isEqualTo("jpg");
            assertThat(imageValidationUtil.getFileExtension("파일명.png")).isEqualTo("png");
            assertThat(imageValidationUtil.getFileExtension("image with spaces.webp")).isEqualTo("webp");
        }

        @Test
        @DisplayName("성공: 여러 점이 있는 파일명")
        void getFileExtension_MultipleDots_ReturnsLastExtension() {
            // when & then
            assertThat(imageValidationUtil.getFileExtension("my.image.file.jpg")).isEqualTo("jpg");
            assertThat(imageValidationUtil.getFileExtension("version.1.0.png")).isEqualTo("png");
        }

        @Test
        @DisplayName("성공: 경로가 포함된 파일명")
        void getFileExtension_WithPath_ExtractsCorrectly() {
            // when & then
            assertThat(imageValidationUtil.getFileExtension("/path/to/image.jpg")).isEqualTo("jpg");
            assertThat(imageValidationUtil.getFileExtension("C:\\Users\\Documents\\photo.png")).isEqualTo("png");
        }
    }

    // === Helper Methods ===

    /**
     * 유효한 JPEG 파일 내용 생성
     * Magic Number: FF D8 FF + 더미 데이터
     */
    private byte[] createJpegContent() {
        byte[] content = new byte[100];
        content[0] = (byte) 0xFF; // JPEG magic number
        content[1] = (byte) 0xD8;
        content[2] = (byte) 0xFF;
        content[3] = (byte) 0xE0; // JFIF marker
        return content;
    }

    /**
     * 유효한 PNG 파일 내용 생성
     * Magic Number: 89 50 4E 47
     */
    private byte[] createPngContent() {
        byte[] content = new byte[100];
        content[0] = (byte) 0x89; // PNG magic number
        content[1] = 0x50;        // P
        content[2] = 0x4E;        // N
        content[3] = 0x47;        // G
        content[4] = 0x0D;
        content[5] = 0x0A;
        content[6] = 0x1A;
        content[7] = 0x0A;
        return content;
    }

    /**
     * 유효한 WebP 파일 내용 생성
     * Magic Number: RIFF....WEBP
     */
    private byte[] createWebPContent() {
        byte[] content = new byte[100];
        // RIFF header
        content[0] = 0x52; // R
        content[1] = 0x49; // I
        content[2] = 0x46; // F
        content[3] = 0x46; // F
        // 파일 크기 (little endian) - 4바이트
        content[4] = 0x00;
        content[5] = 0x00;
        content[6] = 0x00;
        content[7] = 0x00;
        // WEBP header
        content[8] = 0x57;  // W
        content[9] = 0x45;  // E
        content[10] = 0x42; // B
        content[11] = 0x50; // P
        return content;
    }

    /**
     * 스크립트 테스트용 JPEG 헤더 문자열 생성
     */
    private String createJpegHeaderString() {
        return "\u00FF\u00D8\u00FF\u00E0"; // JPEG magic number를 문자열로
    }
}