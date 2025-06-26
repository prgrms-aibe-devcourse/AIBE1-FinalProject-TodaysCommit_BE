package com.team5.catdogeats.support.domain.notice.service.impl;

import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.storage.domain.repository.FilesRepository;
import com.team5.catdogeats.storage.domain.service.ObjectStorageService;
import com.team5.catdogeats.support.domain.Notices;
import com.team5.catdogeats.support.domain.notice.dto.NoticeFileDownloadResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

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
    private ObjectStorageService objectStorageService;

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
    void downloadFile_Success() throws Exception {
        // given
        String fileId = "test-file-id";
        String s3FileUrl = "https://cdn.example.com/files/notice_12345678_20250625_120000_test.txt";

        Files fileEntity = Files.builder()
                .id(fileId)
                .fileUrl(s3FileUrl)
                .build();

        given(filesRepository.findById(fileId)).willReturn(Optional.of(fileEntity));

        // when
        NoticeFileDownloadResponseDTO result = noticeService.downloadFile(fileId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(UrlResource.class);
        verify(filesRepository).findById(fileId);

        // ObjectStorageService는 다운로드에 직접 사용되지 않음 (URL 기반 접근)
        // S3 URL을 통해 UrlResource로 직접 접근하는 방식
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

    // 실제 다운로드 실패는 네트워크/S3 문제이므로 단위 테스트에서 시뮬레이션 어려움
    // 대신 URL 형식은 정상이지만 존재하지 않는 파일을 테스트
    @Test
    @DisplayName("파일 다운로드 - 정상 URL (존재하지 않는 파일)")
    void downloadFile_NonExistentFile() {
        // given
        String fileId = "test-file-id";
        String validButNonExistentUrl = "https://cdn.example.com/files/non-existent-file.txt";

        Files fileEntity = Files.builder()
                .id(fileId)
                .fileUrl(validButNonExistentUrl)
                .build();

        given(filesRepository.findById(fileId)).willReturn(Optional.of(fileEntity));

        // when
        NoticeFileDownloadResponseDTO result = noticeService.downloadFile(fileId);

        // then
        // UrlResource는 생성되지만 실제 파일은 존재하지 않음
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(UrlResource.class);
        verify(filesRepository).findById(fileId);
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