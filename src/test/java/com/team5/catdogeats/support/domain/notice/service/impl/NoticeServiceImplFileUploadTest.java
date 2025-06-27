package com.team5.catdogeats.support.domain.notice.service.impl;

import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.storage.domain.mapping.NoticeFiles;
import com.team5.catdogeats.storage.repository.FilesRepository;
import com.team5.catdogeats.storage.service.NoticeFileManagementService;
import com.team5.catdogeats.storage.service.ObjectStorageService;
import com.team5.catdogeats.support.domain.Notices;
import com.team5.catdogeats.support.domain.notice.dto.NoticeResponseDTO;
import com.team5.catdogeats.support.domain.notice.repository.NoticeFilesRepository;
import com.team5.catdogeats.support.domain.notice.repository.NoticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("공지사항 파일 업로드 서비스 테스트")
class NoticeServiceImplFileUploadTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private FilesRepository filesRepository;

    @Mock
    private NoticeFilesRepository noticeFilesRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private NoticeFileManagementService noticeFileManagementService; // 🆕 추가

    @InjectMocks
    private NoticeServiceImpl noticeService;

    private Notices testNotice;

    @BeforeEach
    void setUp() {
        testNotice = Notices.builder()
                .id("test-notice-id")
                .title("테스트 공지사항")
                .content("테스트 내용입니다.")
                .viewCount(5L)
                .build();

        setTimeFields(testNotice);
    }

    // ========== 파일 업로드 테스트 ==========
    @Test
    @DisplayName("파일 업로드 - 성공")
    void uploadFile_Success() throws IOException {
        // given
        String noticeId = "test-notice-id";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "테스트 파일 내용".getBytes()
        );

        Files savedFile = Files.builder()
                .id("file-id")
                .fileUrl("https://cdn.example.com/files/notice_12345678_20250625_120000_test.pdf")
                .build();
        setTimeFields(savedFile);

        NoticeFiles noticeFile = NoticeFiles.builder()
                .id("notice-file-id")
                .notices(testNotice)
                .files(savedFile)
                .build();
        setTimeFields(noticeFile);

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(testNotice));
        given(noticeFileManagementService.uploadNoticeFile(file)).willReturn(savedFile);
        given(noticeFilesRepository.save(any(NoticeFiles.class))).willReturn(noticeFile);
        given(noticeFilesRepository.findByNoticesId(noticeId)).willReturn(List.of(noticeFile));

        // when
        NoticeResponseDTO result = noticeService.uploadFile(noticeId, file);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAttachments()).hasSize(1);

        verify(noticeRepository).findById(noticeId);
        verify(noticeFileManagementService).uploadNoticeFile(file);
        verify(noticeFilesRepository).save(any(NoticeFiles.class));
        verify(noticeFilesRepository).findByNoticesId(noticeId);
    }

    @Test
    @DisplayName("파일 업로드 - 존재하지 않는 공지사항")
    void uploadFile_NoticeNotFound() {
        // given
        String noticeId = "non-existing-id";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "테스트 파일 내용".getBytes()
        );

        given(noticeRepository.findById(noticeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.uploadFile(noticeId, file))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("공지사항을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("파일 업로드 - 허용되지 않는 파일 형식")
    void uploadFile_InvalidFileType() {
        // given
        String noticeId = "test-notice-id";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.exe", // 허용되지 않는 확장자
                "application/x-executable",
                "테스트 파일 내용".getBytes()
        );

        // noticeRepository.findById() 스터빙 제거
        // 파일 검증이 먼저 일어나서 이 메서드가 호출되지 않음

        // when & then
        assertThatThrownBy(() -> noticeService.uploadFile(noticeId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("허용되지 않는 파일 형식입니다");
    }

    @Test
    @DisplayName("파일 업로드 - 파일 크기 초과")
    void uploadFile_FileSizeExceeded() {
        // given
        String noticeId = "test-notice-id";

        // 10MB를 초과하는 파일 크기 시뮬레이션
        byte[] largeFileContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large-file.pdf",
                "application/pdf",
                largeFileContent
        );

        // noticeRepository.findById() 스터빙 제거
        // 파일 크기 검증이 먼저 일어나서 이 메서드가 호출되지 않음

        // when & then
        assertThatThrownBy(() -> noticeService.uploadFile(noticeId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("파일 크기는 10MB를 초과할 수 없습니다");
    }

    @Test
    @DisplayName("파일 업로드 - 서비스 업로드 실패")
    void uploadFile_ServiceUploadFailure() {
        // given
        String noticeId = "test-notice-id";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "테스트 파일 내용".getBytes()
        );

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(testNotice));
        given(noticeFileManagementService.uploadNoticeFile(file))
                .willThrow(new RuntimeException("파일 업로드 서비스 실패"));

        // when & then
        assertThatThrownBy(() -> noticeService.uploadFile(noticeId, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("파일 업로드 서비스 실패");
    }

    // ========== 헬퍼 메서드 ==========
    private void setTimeFields(Object entity) {
        try {
            ZonedDateTime now = ZonedDateTime.now();
            Class<?> superclass = entity.getClass().getSuperclass();

            java.lang.reflect.Field createdAtField = superclass.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(entity, now);

            java.lang.reflect.Field updatedAtField = superclass.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(entity, now);
        } catch (Exception e) {
            // 리플렉션 실패 시 무시
        }
    }
}