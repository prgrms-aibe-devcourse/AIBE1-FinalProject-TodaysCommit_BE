package com.team5.catdogeats.support.domain.notice.service.impl;

import com.team5.catdogeats.storage.domain.service.NoticeFileManagementService;
import com.team5.catdogeats.support.domain.notice.dto.NoticeFileDownloadResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("공지사항 파일 다운로드 서비스 테스트")
class NoticeServiceImplFileDownloadTest {

    @Mock
    private NoticeFileManagementService noticeFileManagementService;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    // ========== 파일 다운로드 테스트 ==========
    @Test
    @DisplayName("파일 다운로드 - 성공")
    void downloadFile_Success() {
        // given
        String fileId = "test-file-id";

        Resource mockResource = mock(Resource.class);

        NoticeFileDownloadResponseDTO expectedResponse = new NoticeFileDownloadResponseDTO(
                mockResource,
                "test.pdf",
                "application/pdf"
        );

        given(noticeFileManagementService.downloadNoticeFile(fileId))
                .willReturn(expectedResponse);

        // when
        NoticeFileDownloadResponseDTO result = noticeService.downloadFile(fileId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getResource()).isEqualTo(mockResource);
        assertThat(result.getFilename()).isEqualTo("test.pdf");
        assertThat(result.getContentType()).isEqualTo("application/pdf");

        verify(noticeFileManagementService).downloadNoticeFile(fileId);
    }

    @Test
    @DisplayName("파일 다운로드 - 존재하지 않는 파일 ID")
    void downloadFile_FileNotFound() {
        // given
        String fileId = "non-existing-file-id";

        given(noticeFileManagementService.downloadNoticeFile(fileId))
                .willThrow(new NoSuchElementException("파일을 찾을 수 없습니다. ID: " + fileId));

        // when & then
        assertThatThrownBy(() -> noticeService.downloadFile(fileId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("파일을 찾을 수 없습니다");

        verify(noticeFileManagementService).downloadNoticeFile(fileId);
    }

    @Test
    @DisplayName("파일 다운로드 - 스토리지 서비스 오류")
    void downloadFile_StorageServiceError() {
        // given
        String fileId = "test-file-id";

        given(noticeFileManagementService.downloadNoticeFile(fileId))
                .willThrow(new RuntimeException("스토리지 서비스 오류"));

        // when & then
        assertThatThrownBy(() -> noticeService.downloadFile(fileId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("스토리지 서비스 오류");

        verify(noticeFileManagementService).downloadNoticeFile(fileId);
    }

    @Test
    @DisplayName("파일 다운로드 - 파일이 존재하지만 읽을 수 없는 경우")
    void downloadFile_FileNotReadable() {
        // given
        String fileId = "test-file-id";

        Resource mockResource = mock(Resource.class);

        NoticeFileDownloadResponseDTO expectedResponse = new NoticeFileDownloadResponseDTO(
                mockResource,
                "corrupted-file.pdf",
                "application/pdf"
        );

        given(noticeFileManagementService.downloadNoticeFile(fileId))
                .willReturn(expectedResponse);

        // when
        NoticeFileDownloadResponseDTO result = noticeService.downloadFile(fileId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getResource()).isEqualTo(mockResource);
        assertThat(result.getFilename()).isEqualTo("corrupted-file.pdf");
        assertThat(result.getContentType()).isEqualTo("application/pdf");

        verify(noticeFileManagementService).downloadNoticeFile(fileId);
    }
}