package com.team5.catdogeats.support.domain.notice.service.impl;

import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.storage.domain.mapping.NoticeFiles;
import com.team5.catdogeats.storage.domain.repository.FilesRepository;
import com.team5.catdogeats.storage.domain.service.FileStorageService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("공지사항 파일 관리 서비스 테스트")
class NoticeServiceImplFileManagementTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private FilesRepository filesRepository;

    @Mock
    private NoticeFilesRepository noticeFilesRepository;

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

    // ========== 파일 삭제 테스트 (4개) ==========
    @Test
    @DisplayName("파일 삭제 - 성공")
    void deleteFile_Success() throws IOException {
        // given
        String noticeId = "test-notice-id";
        String fileId = "test-file-id";

        Files fileEntity = Files.builder()
                .id(fileId)
                .fileUrl("/path/to/file")
                .build();

        NoticeFiles noticeFile = NoticeFiles.builder()
                .id("notice-file-id")
                .notices(testNotice)
                .files(fileEntity)
                .build();

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(testNotice));
        given(filesRepository.findById(fileId)).willReturn(Optional.of(fileEntity));
        given(noticeFilesRepository.findByNoticesId(noticeId)).willReturn(List.of(noticeFile));
        given(noticeFilesRepository.count()).willReturn(0L);

        // when
        noticeService.deleteFile(noticeId, fileId);

        // then
        verify(noticeRepository).findById(noticeId);
        verify(filesRepository).findById(fileId);
        verify(fileStorageService).deleteFile(fileEntity.getFileUrl());
        verify(filesRepository).deleteById(fileId);
    }

    @Test
    @DisplayName("파일 삭제 - 존재하지 않는 공지사항")
    void deleteFile_NoticeNotFound() {
        // given
        String noticeId = "non-existing-notice-id";
        String fileId = "test-file-id";

        given(noticeRepository.findById(noticeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.deleteFile(noticeId, fileId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("파일 삭제 중 오류가 발생했습니다");
    }

    @Test
    @DisplayName("파일 삭제 - 존재하지 않는 파일")
    void deleteFile_FileNotFound() {
        // given
        String noticeId = "test-notice-id";
        String fileId = "non-existing-file-id";

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(testNotice));
        given(filesRepository.findById(fileId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.deleteFile(noticeId, fileId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("파일 삭제 중 오류가 발생했습니다");
    }

    @Test
    @DisplayName("파일 삭제 - 해당 공지사항에 연결되지 않은 파일")
    void deleteFile_FileNotLinkedToNotice() {
        // given
        String noticeId = "test-notice-id";
        String fileId = "test-file-id";

        Files fileEntity = Files.builder()
                .id(fileId)
                .fileUrl("/path/to/file")
                .build();

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(testNotice));
        given(filesRepository.findById(fileId)).willReturn(Optional.of(fileEntity));
        given(noticeFilesRepository.findByNoticesId(noticeId)).willReturn(new ArrayList<>());

        // when & then
        assertThatThrownBy(() -> noticeService.deleteFile(noticeId, fileId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("파일 삭제 중 오류가 발생했습니다");
    }

    // ========== 파일 수정(교체) 테스트 (5개) ==========
    @Test
    @DisplayName("파일 수정(교체) - 성공")
    void replaceFile_Success() throws IOException {
        // given
        String noticeId = "test-notice-id";
        String fileId = "test-file-id";
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "new-test.txt",
                "text/plain",
                "새로운 파일 내용".getBytes()
        );

        Files oldFileEntity = Files.builder()
                .id(fileId)
                .fileUrl("/path/to/old-file")
                .build();
        setTimeFields(oldFileEntity);

        NoticeFiles noticeFile = NoticeFiles.builder()
                .id("notice-file-id")
                .notices(testNotice)
                .files(oldFileEntity)
                .build();
        setTimeFields(noticeFile);

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(testNotice));
        given(filesRepository.findById(fileId)).willReturn(Optional.of(oldFileEntity));
        given(noticeFilesRepository.findByNoticesId(noticeId)).willReturn(List.of(noticeFile));
        given(fileStorageService.replaceFile("/path/to/old-file", newFile)).willReturn("/path/to/new-file");
        given(filesRepository.save(any(Files.class))).willReturn(oldFileEntity);

        // when
        NoticeResponseDTO result = noticeService.replaceFile(noticeId, fileId, newFile);

        // then
        assertThat(result).isNotNull();
        verify(noticeRepository).findById(noticeId);
        verify(filesRepository).findById(fileId);
        verify(fileStorageService).replaceFile("/path/to/old-file", newFile);
        verify(filesRepository).save(oldFileEntity);
    }

    @Test
    @DisplayName("파일 수정(교체) - 존재하지 않는 공지사항")
    void replaceFile_NoticeNotFound() {
        // given
        String noticeId = "non-existing-notice-id";
        String fileId = "test-file-id";
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "new-test.txt",
                "text/plain",
                "새로운 파일 내용".getBytes()
        );

        given(noticeRepository.findById(noticeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.replaceFile(noticeId, fileId, newFile))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("공지사항을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("파일 수정(교체) - 존재하지 않는 파일")
    void replaceFile_FileNotFound() {
        // given
        String noticeId = "test-notice-id";
        String fileId = "non-existing-file-id";
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "new-test.txt",
                "text/plain",
                "새로운 파일 내용".getBytes()
        );

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(testNotice));
        given(filesRepository.findById(fileId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.replaceFile(noticeId, fileId, newFile))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("파일을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("파일 수정(교체) - 해당 공지사항에 연결되지 않은 파일")
    void replaceFile_FileNotLinkedToNotice() {
        // given
        String noticeId = "test-notice-id";
        String fileId = "test-file-id";
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "new-test.txt",
                "text/plain",
                "새로운 파일 내용".getBytes()
        );

        Files fileEntity = Files.builder()
                .id(fileId)
                .fileUrl("/path/to/file")
                .build();

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(testNotice));
        given(filesRepository.findById(fileId)).willReturn(Optional.of(fileEntity));
        given(noticeFilesRepository.findByNoticesId(noticeId)).willReturn(new ArrayList<>());

        // when & then
        assertThatThrownBy(() -> noticeService.replaceFile(noticeId, fileId, newFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 공지사항에 연결되지 않은 파일입니다");
    }

    @Test
    @DisplayName("파일 수정(교체) - IOException 발생")
    void replaceFile_IOException() throws IOException {
        // given
        String noticeId = "test-notice-id";
        String fileId = "test-file-id";
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "new-test.txt",
                "text/plain",
                "새로운 파일 내용".getBytes()
        );

        Files oldFileEntity = Files.builder()
                .id(fileId)
                .fileUrl("/path/to/old-file")
                .build();

        NoticeFiles noticeFile = NoticeFiles.builder()
                .id("notice-file-id")
                .notices(testNotice)
                .files(oldFileEntity)
                .build();

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(testNotice));
        given(filesRepository.findById(fileId)).willReturn(Optional.of(oldFileEntity));
        given(noticeFilesRepository.findByNoticesId(noticeId)).willReturn(List.of(noticeFile));
        given(fileStorageService.replaceFile("/path/to/old-file", newFile)).willThrow(new IOException("새 파일 저장 실패"));

        // when & then
        assertThatThrownBy(() -> noticeService.replaceFile(noticeId, fileId, newFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("파일 수정(교체) 중 오류가 발생했습니다");
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