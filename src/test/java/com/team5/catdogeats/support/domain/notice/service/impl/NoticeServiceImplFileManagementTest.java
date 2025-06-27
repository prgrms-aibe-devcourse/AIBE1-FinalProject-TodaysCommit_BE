package com.team5.catdogeats.support.domain.notice.service.impl;

import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.storage.domain.mapping.NoticeFiles;
import com.team5.catdogeats.storage.service.NoticeFileManagementService;
import com.team5.catdogeats.support.domain.Notices;
import com.team5.catdogeats.support.domain.notice.dto.NoticeResponseDTO;
import com.team5.catdogeats.support.domain.notice.repository.NoticeFilesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("공지사항 파일 관리 서비스 테스트")
class NoticeServiceImplFileManagementTest {

    @Mock
    private NoticeFilesRepository noticeFilesRepository;

    @Mock
    private NoticeFileManagementService noticeFileManagementService;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    private Notices testNotice;
    private Files testFile;
    private NoticeFiles testNoticeFile;

    @BeforeEach
    void setUp() {
        testNotice = Notices.builder()
                .id("test-notice-id")
                .title("테스트 공지사항")
                .content("테스트 내용입니다.")
                .viewCount(5L)
                .build();
        setTimeFields(testNotice);

        testFile = Files.builder()
                .id("test-file-id")
                .fileUrl("https://cdn.example.com/files/test-file.txt")
                .build();
        setTimeFields(testFile);

        testNoticeFile = NoticeFiles.builder()
                .id("notice-file-id")
                .notices(testNotice)
                .files(testFile)
                .build();
        setTimeFields(testNoticeFile);
    }

    // ========== 파일 삭제 테스트 ==========
    @Test
    @DisplayName("파일 삭제 - 성공")
    void deleteFile_Success() {
        // given
        String noticeId = "test-notice-id";
        String fileId = "test-file-id";

        given(noticeFilesRepository.findByNoticesIdAndFilesId(noticeId, fileId))
                .willReturn(Optional.of(testNoticeFile));

        // when
        noticeService.deleteFile(noticeId, fileId);

        // then
        verify(noticeFilesRepository).findByNoticesIdAndFilesId(noticeId, fileId);
        verify(noticeFilesRepository).deleteById(testNoticeFile.getId());
        verify(noticeFileManagementService).deleteNoticeFileCompletely(fileId);
    }

    @Test
    @DisplayName("파일 삭제 - 해당 공지사항에 연결되지 않은 파일")
    void deleteFile_FileNotLinkedToNotice() {
        // given
        String noticeId = "test-notice-id";
        String fileId = "test-file-id";

        given(noticeFilesRepository.findByNoticesIdAndFilesId(noticeId, fileId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.deleteFile(noticeId, fileId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 공지사항에 연결되지 않은 파일입니다");

        verify(noticeFilesRepository).findByNoticesIdAndFilesId(noticeId, fileId);
        verifyNoInteractions(noticeFileManagementService);
    }

    @Test
    @DisplayName("파일 삭제 - 파일 관리 서비스 오류")
    void deleteFile_ManagementServiceError() {
        // given
        String noticeId = "test-notice-id";
        String fileId = "test-file-id";

        given(noticeFilesRepository.findByNoticesIdAndFilesId(noticeId, fileId))
                .willReturn(Optional.of(testNoticeFile));

        doThrow(new RuntimeException("파일 삭제 서비스 오류"))
                .when(noticeFileManagementService).deleteNoticeFileCompletely(fileId);

        // when & then
        assertThatThrownBy(() -> noticeService.deleteFile(noticeId, fileId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("파일 삭제 서비스 오류");

        verify(noticeFilesRepository).findByNoticesIdAndFilesId(noticeId, fileId);
        verify(noticeFilesRepository).deleteById(testNoticeFile.getId());
        verify(noticeFileManagementService).deleteNoticeFileCompletely(fileId);
    }

    // ========== 파일 수정(교체) 테스트 ==========
    @Test
    @DisplayName("파일 수정(교체) - 성공")
    void replaceFile_Success() {
        // given
        String noticeId = "test-notice-id";
        String fileId = "test-file-id";
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "new-test.pdf",
                "application/pdf",
                "새로운 파일 내용".getBytes()
        );

        given(noticeFilesRepository.findByNoticesIdAndFilesId(noticeId, fileId))
                .willReturn(Optional.of(testNoticeFile));
        given(noticeFilesRepository.findByNoticesId(noticeId))
                .willReturn(List.of(testNoticeFile));

        // when
        NoticeResponseDTO result = noticeService.replaceFile(noticeId, fileId, newFile);

        // then
        assertThat(result).isNotNull();

        verify(noticeFilesRepository).findByNoticesIdAndFilesId(noticeId, fileId);
        verify(noticeFileManagementService).replaceNoticeFile(fileId, newFile);
        verify(noticeFilesRepository).findByNoticesId(noticeId);
    }

    @Test
    @DisplayName("파일 수정(교체) - 허용되지 않는 파일 형식")
    void replaceFile_InvalidFileType() {
        // given
        String noticeId = "test-notice-id";
        String fileId = "test-file-id";
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "malicious.exe", // 허용되지 않는 확장자
                "application/x-executable",
                "악성 파일 내용".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> noticeService.replaceFile(noticeId, fileId, newFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("허용되지 않는 파일 형식입니다");

        // 파일 검증이 먼저 일어나므로 다른 메서드들은 호출되지 않음
        verifyNoInteractions(noticeFilesRepository);
        verifyNoInteractions(noticeFileManagementService);
    }

    @Test
    @DisplayName("파일 수정(교체) - 파일 크기 초과")
    void replaceFile_FileSizeExceeded() {
        // given
        String noticeId = "test-notice-id";
        String fileId = "test-file-id";

        // 10MB를 초과하는 파일 크기 시뮬레이션
        byte[] largeFileContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "large-file.pdf",
                "application/pdf",
                largeFileContent
        );

        // when & then
        assertThatThrownBy(() -> noticeService.replaceFile(noticeId, fileId, newFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("파일 크기는 10MB를 초과할 수 없습니다");

        // 파일 검증이 먼저 일어나므로 다른 메서드들은 호출되지 않음
        verifyNoInteractions(noticeFilesRepository);
        verifyNoInteractions(noticeFileManagementService);
    }

    @Test
    @DisplayName("파일 수정(교체) - 해당 공지사항에 연결되지 않은 파일")
    void replaceFile_FileNotLinkedToNotice() {
        // given
        String noticeId = "test-notice-id";
        String fileId = "test-file-id";
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "new-test.pdf",
                "application/pdf",
                "새로운 파일 내용".getBytes()
        );

        given(noticeFilesRepository.findByNoticesIdAndFilesId(noticeId, fileId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.replaceFile(noticeId, fileId, newFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 공지사항에 연결되지 않은 파일입니다");

        verify(noticeFilesRepository).findByNoticesIdAndFilesId(noticeId, fileId);
        verifyNoInteractions(noticeFileManagementService);
    }

    @Test
    @DisplayName("파일 수정(교체) - 파일 관리 서비스 오류")
    void replaceFile_ManagementServiceError() {
        // given
        String noticeId = "test-notice-id";
        String fileId = "test-file-id";
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "new-test.pdf",
                "application/pdf",
                "새로운 파일 내용".getBytes()
        );

        given(noticeFilesRepository.findByNoticesIdAndFilesId(noticeId, fileId))
                .willReturn(Optional.of(testNoticeFile));

        doThrow(new RuntimeException("파일 교체 서비스 오류"))
                .when(noticeFileManagementService).replaceNoticeFile(fileId, newFile);

        // when & then
        assertThatThrownBy(() -> noticeService.replaceFile(noticeId, fileId, newFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("파일 교체 서비스 오류");

        verify(noticeFilesRepository).findByNoticesIdAndFilesId(noticeId, fileId);
        verify(noticeFileManagementService).replaceNoticeFile(fileId, newFile);
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