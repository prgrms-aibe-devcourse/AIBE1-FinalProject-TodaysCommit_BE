package com.team5.catdogeats.storage.domain.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("로컬 파일 저장소 서비스 파일 작업 테스트")
class LocalFileStorageServiceFileOpsTest {

    @InjectMocks
    private LocalFileStorageServiceImpl fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
    }

    // ========== 업로드 테스트 (6개) ==========

    @Test
    @DisplayName("파일 업로드 - 성공")
    void uploadFile_Success() throws IOException {
        // given
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello World".getBytes()
        );

        // when
        String filePath = fileStorageService.uploadFile(mockFile);

        // then
        assertThat(filePath).isNotNull();
        assertThat(filePath).contains("test.txt");
        assertThat(filePath).contains(tempDir.toString());

        // 파일이 실제로 생성되었는지 확인
        Path uploadedFile = Paths.get(filePath);
        assertThat(Files.exists(uploadedFile)).isTrue();
        assertThat(Files.readString(uploadedFile)).isEqualTo("Hello World");

        // 파일명 형식 확인 (yyyyMMdd_HHmmss_원본파일명)
        String fileName = uploadedFile.getFileName().toString();
        assertThat(fileName).matches("\\d{8}_\\d{6}_test\\.txt");
    }

    @Test
    @DisplayName("파일 업로드 - 파일명이 null인 경우")
    void uploadFile_NullFileName() {
        // given
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                null,  // 파일명이 null
                "text/plain",
                "Hello World".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> fileStorageService.uploadFile(mockFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일명이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("파일 업로드 - 빈 파일명인 경우")
    void uploadFile_EmptyFileName() {
        // given
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "",  // 빈 파일명
                "text/plain",
                "Hello World".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> fileStorageService.uploadFile(mockFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일명이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("파일 업로드 - 공백만 있는 파일명인 경우")
    void uploadFile_WhitespaceFileName() {
        // given
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "   ",  // 공백만 있는 파일명
                "text/plain",
                "Hello World".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> fileStorageService.uploadFile(mockFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일명이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("업로드 디렉토리 자동 생성")
    void uploadFile_CreateDirectoryIfNotExists() throws IOException {
        // given
        Path newUploadDir = tempDir.resolve("new-upload-dir");
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", newUploadDir.toString());

        // 디렉토리가 존재하지 않는지 확인
        assertThat(Files.exists(newUploadDir)).isFalse();

        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Test Content".getBytes()
        );

        // when
        String filePath = fileStorageService.uploadFile(mockFile);

        // then
        assertThat(Files.exists(newUploadDir)).isTrue();
        assertThat(Files.exists(Paths.get(filePath))).isTrue();
    }

    @Test
    @DisplayName("파일 교체 - 성공")
    void replaceFile_Success() throws IOException {
        // given
        MockMultipartFile oldFile = new MockMultipartFile(
                "file", "old.txt", "text/plain", "Old Content".getBytes()
        );
        MockMultipartFile newFile = new MockMultipartFile(
                "file", "new.txt", "text/plain", "New Content".getBytes()
        );

        String oldFileUrl = fileStorageService.uploadFile(oldFile);

        // when
        String newFileUrl = fileStorageService.replaceFile(oldFileUrl, newFile);

        // then
        assertThat(newFileUrl).isNotNull();
        assertThat(newFileUrl).isNotEqualTo(oldFileUrl);
        assertThat(Files.exists(Paths.get(newFileUrl))).isTrue();
        assertThat(Files.exists(Paths.get(oldFileUrl))).isFalse(); // 기존 파일 삭제됨
        assertThat(Files.readString(Paths.get(newFileUrl))).isEqualTo("New Content");
    }

    // ========== 다운로드 테스트 (3개) ==========

    @Test
    @DisplayName("파일 다운로드 - 성공")
    void downloadFile_Success() throws IOException {
        // given
        Path testFile = tempDir.resolve("download-test.txt");
        Files.write(testFile, "Download Test Content".getBytes());

        // when
        Resource resource = fileStorageService.downloadFile(testFile.toString());

        // then
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();

        // 파일 내용 확인
        try (InputStream inputStream = resource.getInputStream()) {
            String content = new String(inputStream.readAllBytes());
            assertThat(content).isEqualTo("Download Test Content");
        }
    }

    @Test
    @DisplayName("파일 다운로드 - 존재하지 않는 파일")
    void downloadFile_FileNotFound() {
        // given
        String nonExistentPath = tempDir.resolve("non-existent.txt").toString();

        // when & then
        assertThatThrownBy(() -> fileStorageService.downloadFile(nonExistentPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("파일을 찾을 수 없거나 읽을 수 없습니다");
    }

    @Test
    @DisplayName("파일 다운로드 - 잘못된 파일 경로")
    void downloadFile_InvalidPath() {
        // given
        String invalidPath = "invalid://path/file.txt";

        // when & then
        assertThatThrownBy(() -> fileStorageService.downloadFile(invalidPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("잘못된 파일 경로입니다");
    }

    // ========== 삭제 테스트 (2개) ==========

    @Test
    @DisplayName("파일 삭제 - 성공")
    void deleteFile_Success() throws IOException {
        // given
        Path testFile = tempDir.resolve("delete-test.txt");
        Files.write(testFile, "Delete Test Content".getBytes());
        assertThat(Files.exists(testFile)).isTrue();

        // when
        fileStorageService.deleteFile(testFile.toString());

        // then
        assertThat(Files.exists(testFile)).isFalse();
    }

    @Test
    @DisplayName("파일 삭제 - 존재하지 않는 파일")
    void deleteFile_FileNotFound() throws IOException {
        // given
        String nonExistentPath = tempDir.resolve("non-existent.txt").toString();

        // when & then
        // 존재하지 않는 파일 삭제는 예외가 발생하지 않아야 함
        assertThatCode(() -> fileStorageService.deleteFile(nonExistentPath))
                .doesNotThrowAnyException();
    }
}