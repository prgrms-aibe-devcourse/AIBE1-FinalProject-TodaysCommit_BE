package com.team5.catdogeats.support.domain.notice.service.impl;

import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.storage.domain.repository.FilesRepository;
import com.team5.catdogeats.storage.domain.service.FileStorageService;
import com.team5.catdogeats.support.domain.Notices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("공지사항 파일 다운로드 서비스 테스트")
class NoticeServiceImplFileDownloadTest {

    @Mock
    private FilesRepository filesRepository;

    @Mock
    private FileStorageService fileStorageService;

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

    // ========== 파일 다운로드 테스트 ==========
    @Test
    @DisplayName("파일 다운로드 - 성공")
    void downloadFile_Success() throws IOException {
        // given
        String fileId = "test-file-id";
        // 실제 존재하는 시스템 파일 사용 (Windows 기준)
        String filePath = "C:/Windows/System32/drivers/etc/hosts";

        Files fileEntity = Files.builder()
                .id(fileId)
                .fileUrl(filePath)
                .build();

        Resource mockResource = mock(Resource.class);

        given(filesRepository.findById(fileId)).willReturn(Optional.of(fileEntity));
        given(fileStorageService.downloadFile(filePath)).willReturn(mockResource);

        // when
        Resource result = noticeService.downloadFile(fileId);

        // then
        assertThat(result).isNotNull();
        verify(filesRepository).findById(fileId);
        verify(fileStorageService).downloadFile(filePath);
    }

    @Test
    @DisplayName("파일 다운로드 - 존재하지 않는 파일 ID")
    void downloadFile_FileNotFound() {
        // given
        String fileId = "non-existing-file-id";
        given(filesRepository.findById(fileId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.downloadFile(fileId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("파일을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("파일 다운로드 - 파일이 물리적으로 존재하지 않음")
    void downloadFile_FileNotExists() {
        // given
        String fileId = "test-file-id";
        String filePath = "/completely/non/existing/path/test-file.txt";

        Files fileEntity = Files.builder()
                .id(fileId)
                .fileUrl(filePath)
                .build();

        given(filesRepository.findById(fileId)).willReturn(Optional.of(fileEntity));

        // when & then
        assertThatThrownBy(() -> noticeService.downloadFile(fileId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("파일이 존재하지 않습니다");
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