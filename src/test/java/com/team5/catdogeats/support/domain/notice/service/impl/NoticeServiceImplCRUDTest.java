package com.team5.catdogeats.support.domain.notice.service.impl;

import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.storage.domain.mapping.NoticeFiles;
import com.team5.catdogeats.storage.domain.repository.FilesRepository;
import com.team5.catdogeats.storage.domain.service.ObjectStorageService;
import com.team5.catdogeats.support.domain.Notices;
import com.team5.catdogeats.support.domain.notice.dto.NoticeCreateRequestDTO;
import com.team5.catdogeats.support.domain.notice.dto.NoticeResponseDTO;
import com.team5.catdogeats.support.domain.notice.dto.NoticeUpdateRequestDTO;
import com.team5.catdogeats.support.domain.notice.repository.NoticeFilesRepository;
import com.team5.catdogeats.support.domain.notice.repository.NoticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("공지사항 CRUD 서비스 테스트")
class NoticeServiceImplCRUDTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private FilesRepository filesRepository;

    @Mock
    private NoticeFilesRepository noticeFilesRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    private Notices testNotice;
    private NoticeCreateRequestDTO createRequestDTO;
    private NoticeUpdateRequestDTO updateRequestDTO;

    @BeforeEach
    void setUp() {
        testNotice = Notices.builder()
                .id("test-notice-id")
                .title("테스트 공지사항")
                .content("테스트 내용입니다.")
                .viewCount(5L)
                .build();

        setTimeFields(testNotice);

        createRequestDTO = new NoticeCreateRequestDTO();
        createRequestDTO.setTitle("새 공지사항");
        createRequestDTO.setContent("새 공지사항 내용");

        updateRequestDTO = new NoticeUpdateRequestDTO();
        updateRequestDTO.setTitle("수정된 공지사항");
        updateRequestDTO.setContent("수정된 내용");
    }

    @Test
    @DisplayName("공지사항 상세 조회 - 성공 (조회수 증가)")
    void getNotice_Success() {
        // given
        String noticeId = "test-notice-id";

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(testNotice));
        given(noticeFilesRepository.findByNoticesId(noticeId)).willReturn(new ArrayList<>());

        // when
        NoticeResponseDTO result = noticeService.getNotice(noticeId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testNotice.getId());
        assertThat(result.getTitle()).isEqualTo(testNotice.getTitle());
        assertThat(result.getContent()).isEqualTo(testNotice.getContent());

        // 조회수 증가 후 저장되는지 확인 (JpaTransactional에 의해 자동 저장)
        verify(noticeRepository).findById(noticeId);
        verify(noticeFilesRepository).findByNoticesId(noticeId);
    }

    @Test
    @DisplayName("공지사항 상세 조회 - 존재하지 않는 ID")
    void getNotice_NotFound() {
        // given
        String noticeId = "non-existing-id";
        given(noticeRepository.findById(noticeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.getNotice(noticeId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("공지사항을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("공지사항 생성 - 성공")
    void createNotice_Success() {
        // given
        given(noticeRepository.save(any(Notices.class))).willReturn(testNotice);

        // when
        NoticeResponseDTO result = noticeService.createNotice(createRequestDTO);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(testNotice.getTitle());
        assertThat(result.getContent()).isEqualTo(testNotice.getContent());
        assertThat(result.getViewCount()).isEqualTo(testNotice.getViewCount());
        verify(noticeRepository).save(any(Notices.class));
    }

    @Test
    @DisplayName("공지사항 수정 - 성공")
    void updateNotice_Success() {
        // given
        String noticeId = "test-notice-id";
        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(testNotice));
        given(noticeRepository.save(any(Notices.class))).willReturn(testNotice);

        // when
        NoticeResponseDTO result = noticeService.updateNotice(noticeId, updateRequestDTO);

        // then
        assertThat(result).isNotNull();
        verify(noticeRepository).findById(noticeId);
        verify(noticeRepository).save(testNotice);
    }

    @Test
    @DisplayName("공지사항 수정 - 존재하지 않는 ID")
    void updateNotice_NotFound() {
        // given
        String noticeId = "non-existing-id";
        given(noticeRepository.findById(noticeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.updateNotice(noticeId, updateRequestDTO))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("공지사항을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("공지사항 삭제 - 성공 (첨부파일 없음)")
    void deleteNotice_Success_NoFiles() {
        // given
        String noticeId = "test-notice-id";
        given(noticeRepository.existsById(noticeId)).willReturn(true);
        given(noticeFilesRepository.findByNoticesId(noticeId)).willReturn(new ArrayList<>());

        // when
        noticeService.deleteNotice(noticeId);

        // then
        verify(noticeRepository).existsById(noticeId);
        verify(noticeFilesRepository).findByNoticesId(noticeId);
        verify(noticeFilesRepository).deleteByNoticesId(noticeId);
        verify(noticeRepository).deleteById(noticeId);

        // 첨부파일이 없으므로 S3 삭제 호출되지 않음
        verify(objectStorageService, never()).deleteFile(anyString());
    }

    @Test
    @DisplayName("공지사항 삭제 - 성공 (첨부파일 있음)")
    void deleteNotice_Success_WithFiles() {
        // given
        String noticeId = "test-notice-id";

        Files file1 = Files.builder()
                .id("file-1")
                .fileUrl("https://cdn.example.com/files/test-file-1.txt")
                .build();

        Files file2 = Files.builder()
                .id("file-2")
                .fileUrl("https://cdn.example.com/files/test-file-2.txt")
                .build();

        NoticeFiles noticeFile1 = NoticeFiles.builder()
                .id("notice-file-1")
                .notices(testNotice)
                .files(file1)
                .build();

        NoticeFiles noticeFile2 = NoticeFiles.builder()
                .id("notice-file-2")
                .notices(testNotice)
                .files(file2)
                .build();

        given(noticeRepository.existsById(noticeId)).willReturn(true);
        given(noticeFilesRepository.findByNoticesId(noticeId))
                .willReturn(List.of(noticeFile1, noticeFile2));

        // when
        noticeService.deleteNotice(noticeId);

        // then
        verify(noticeRepository).existsById(noticeId);
        verify(noticeFilesRepository).findByNoticesId(noticeId);

        // S3에서 각 파일 삭제 확인
        verify(objectStorageService).deleteFile("files/test-file-1.txt");
        verify(objectStorageService).deleteFile("files/test-file-2.txt");

        verify(noticeFilesRepository).deleteByNoticesId(noticeId);
        verify(noticeRepository).deleteById(noticeId);
    }

    @Test
    @DisplayName("공지사항 삭제 - 존재하지 않는 ID")
    void deleteNotice_NotFound() {
        // given
        String noticeId = "non-existing-id";
        given(noticeRepository.existsById(noticeId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> noticeService.deleteNotice(noticeId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("공지사항을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("공지사항 삭제 - S3 파일 삭제 실패 (무시하고 계속 진행)")
    void deleteNotice_S3DeleteFailure() {
        // given
        String noticeId = "test-notice-id";

        Files file = Files.builder()
                .id("file-1")
                .fileUrl("https://cdn.example.com/files/test-file.txt")
                .build();

        NoticeFiles noticeFile = NoticeFiles.builder()
                .id("notice-file-1")
                .notices(testNotice)
                .files(file)
                .build();

        given(noticeRepository.existsById(noticeId)).willReturn(true);
        given(noticeFilesRepository.findByNoticesId(noticeId)).willReturn(List.of(noticeFile));

        // S3 삭제 실패 시뮬레이션
        doThrow(new RuntimeException("S3 삭제 실패"))
                .when(objectStorageService).deleteFile("files/test-file.txt");

        // when
        noticeService.deleteNotice(noticeId);

        // then
        // S3 삭제가 실패해도 공지사항 삭제는 계속 진행되어야 함
        verify(objectStorageService).deleteFile("files/test-file.txt");
        verify(noticeFilesRepository).deleteByNoticesId(noticeId);
        verify(noticeRepository).deleteById(noticeId);
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