package com.team5.catdogeats.storage.domain.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("로컬 파일 저장소 서비스 유틸리티 테스트")
class LocalFileStorageServiceUtilTest {

    @InjectMocks
    private LocalFileStorageServiceImpl fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
    }

    @Test
    @DisplayName("파일명 추출 - 타임스탬프가 있는 파일명")
    void extractFileName_WithTimestamp() {
        // given
        String fileUrl = "/uploads/20240101_123456_original-file.pdf";

        // when
        String extractedName = fileStorageService.extractFileName(fileUrl);

        // then
        assertThat(extractedName).isEqualTo("original-file.pdf");
    }

    @Test
    @DisplayName("파일명 추출 - 일반 파일명")
    void extractFileName_Normal() {
        // given
        String fileUrl = "/uploads/simple-file.txt";

        // when
        String extractedName = fileStorageService.extractFileName(fileUrl);

        // then
        assertThat(extractedName).isEqualTo("simple-file.txt");
    }

    @Test
    @DisplayName("파일명 추출 - 복잡한 경로")
    void extractFileName_ComplexPath() {
        // given
        String fileUrl = "/var/uploads/notices/20231225_143021_document-with-spaces.docx";

        // when
        String extractedName = fileStorageService.extractFileName(fileUrl);

        // then
        assertThat(extractedName).isEqualTo("document-with-spaces.docx");
    }

    @Test
    @DisplayName("파일명 추출 - 잘못된 경로")
    void extractFileName_InvalidPath() {
        // given
        String invalidUrl = "";

        // when
        String extractedName = fileStorageService.extractFileName(invalidUrl);

        // then
        assertThat(extractedName).isEqualTo("파일명 불명");
    }

    @Test
    @DisplayName("파일명 추출 - null 경로")
    void extractFileName_NullPath() {
        // given
        String nullUrl = null;

        // when
        String extractedName = fileStorageService.extractFileName(nullUrl);

        // then
        assertThat(extractedName).isEqualTo("파일명 불명");
    }
}