package com.team5.catdogeats.support.domain.notice.service.impl;

import com.team5.catdogeats.storage.domain.repository.FilesRepository;
import com.team5.catdogeats.storage.domain.service.FileStorageService;
import com.team5.catdogeats.support.domain.Notices;
import com.team5.catdogeats.support.domain.notice.dto.NoticeListResponseDTO;
import com.team5.catdogeats.support.domain.notice.repository.NoticeFilesRepository;
import com.team5.catdogeats.support.domain.notice.repository.NoticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("공지사항 목록 조회 서비스 테스트")
class NoticeServiceImplSearchTest {

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

    @Test
    @DisplayName("전체 목록 조회 - 최신순")
    void getNotices_AllLatest_Success() {
        // given
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(0, 10, sort);
        List<Notices> noticeList = List.of(testNotice);
        Page<Notices> noticePage = new PageImpl<>(noticeList, pageable, 1);

        given(noticeRepository.findAll(pageable)).willReturn(noticePage);
        given(noticeFilesRepository.findByNoticesId(testNotice.getId())).willReturn(new ArrayList<>());

        // when
        NoticeListResponseDTO result = noticeService.getNotices(0, 10, null, "latest");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNotices()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(0);
        assertThat(result.getNotices().get(0).getViewCount()).isEqualTo(5L);
        verify(noticeRepository).findAll(pageable);
    }

    @Test
    @DisplayName("전체 목록 조회 - 오래된순")
    void getNotices_AllOldest_Success() {
        // given
        Sort sort = Sort.by(Sort.Direction.ASC, "createdAt");
        Pageable pageable = PageRequest.of(0, 10, sort);
        List<Notices> noticeList = List.of(testNotice);
        Page<Notices> noticePage = new PageImpl<>(noticeList, pageable, 1);

        given(noticeRepository.findAll(pageable)).willReturn(noticePage);
        given(noticeFilesRepository.findByNoticesId(testNotice.getId())).willReturn(new ArrayList<>());

        // when
        NoticeListResponseDTO result = noticeService.getNotices(0, 10, null, "oldest");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNotices()).hasSize(1);
        verify(noticeRepository).findAll(pageable);
    }

    @Test
    @DisplayName("전체 목록 조회 - 조회순")
    void getNotices_AllViews_Success() {
        // given
        Sort sort = Sort.by(Sort.Direction.DESC, "viewCount");
        Pageable pageable = PageRequest.of(0, 10, sort);
        List<Notices> noticeList = List.of(testNotice);
        Page<Notices> noticePage = new PageImpl<>(noticeList, pageable, 1);

        given(noticeRepository.findAll(pageable)).willReturn(noticePage);
        given(noticeFilesRepository.findByNoticesId(testNotice.getId())).willReturn(new ArrayList<>());

        // when
        NoticeListResponseDTO result = noticeService.getNotices(0, 10, null, "views");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNotices()).hasSize(1);
        verify(noticeRepository).findAll(pageable);
    }

    @Test
    @DisplayName("검색 + 최신순")
    void getNotices_SearchLatest_Success() {
        // given
        String searchKeyword = "테스트";
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(0, 10, sort);
        List<Notices> noticeList = List.of(testNotice);
        Page<Notices> noticePage = new PageImpl<>(noticeList, pageable, 1);

        given(noticeRepository.findByTitleOrContentContaining(searchKeyword, pageable))
                .willReturn(noticePage);
        given(noticeFilesRepository.findByNoticesId(testNotice.getId())).willReturn(new ArrayList<>());

        // when
        NoticeListResponseDTO result = noticeService.getNotices(0, 10, searchKeyword, "latest");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNotices()).hasSize(1);
        verify(noticeRepository).findByTitleOrContentContaining(searchKeyword, pageable);
    }

    @Test
    @DisplayName("검색 + 오래된순")
    void getNotices_SearchOldest_Success() {
        // given
        String searchKeyword = "테스트";
        Sort sort = Sort.by(Sort.Direction.ASC, "createdAt");
        Pageable pageable = PageRequest.of(0, 10, sort);
        List<Notices> noticeList = List.of(testNotice);
        Page<Notices> noticePage = new PageImpl<>(noticeList, pageable, 1);

        given(noticeRepository.findByTitleOrContentContaining(searchKeyword, pageable))
                .willReturn(noticePage);
        given(noticeFilesRepository.findByNoticesId(testNotice.getId())).willReturn(new ArrayList<>());

        // when
        NoticeListResponseDTO result = noticeService.getNotices(0, 10, searchKeyword, "oldest");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNotices()).hasSize(1);
        verify(noticeRepository).findByTitleOrContentContaining(searchKeyword, pageable);
    }

    @Test
    @DisplayName("검색 + 조회순")
    void getNotices_SearchViews_Success() {
        // given
        String searchKeyword = "공지";
        Sort sort = Sort.by(Sort.Direction.DESC, "viewCount");
        Pageable pageable = PageRequest.of(0, 10, sort);
        List<Notices> noticeList = List.of(testNotice);
        Page<Notices> noticePage = new PageImpl<>(noticeList, pageable, 1);

        given(noticeRepository.findByTitleOrContentContaining(searchKeyword, pageable))
                .willReturn(noticePage);
        given(noticeFilesRepository.findByNoticesId(testNotice.getId())).willReturn(new ArrayList<>());

        // when
        NoticeListResponseDTO result = noticeService.getNotices(0, 10, searchKeyword, "views");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getNotices()).hasSize(1);
        verify(noticeRepository).findByTitleOrContentContaining(searchKeyword, pageable);
    }

    @Test
    @DisplayName("정렬 기준 기본값 테스트")
    void sortBy_DefaultLatest() {
        // given
        Sort expectedSort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable expectedPageable = PageRequest.of(0, 10, expectedSort);
        Page<Notices> noticePage = new PageImpl<>(List.of(testNotice), expectedPageable, 1);

        given(noticeRepository.findAll(expectedPageable)).willReturn(noticePage);
        given(noticeFilesRepository.findByNoticesId(testNotice.getId())).willReturn(new ArrayList<>());

        // when
        noticeService.getNotices(0, 10, null, "invalidSort");

        // then
        verify(noticeRepository).findAll(expectedPageable);
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