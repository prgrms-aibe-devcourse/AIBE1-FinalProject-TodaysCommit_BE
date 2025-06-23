package com.team5.catdogeats.support.domain.notice.repository;

import com.team5.catdogeats.support.domain.Notices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("공지사항 Repository 목록 조회 테스트")
class NoticeRepositoryListTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NoticeRepository noticeRepository;

    private Notices notice1;
    private Notices notice2;
    private Notices notice3;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        notice1 = Notices.builder()
                .title("첫 번째 공지사항")
                .content("첫 번째 공지사항의 내용입니다.")
                .viewCount(10L)
                .build();

        notice2 = Notices.builder()
                .title("두 번째 공지사항")
                .content("두 번째 공지사항의 내용입니다.")
                .viewCount(5L)
                .build();

        notice3 = Notices.builder()
                .title("긴급 알림")
                .content("긴급하게 알려드릴 내용이 있습니다.")
                .viewCount(20L)
                .build();

        // 시간 순서대로 저장 (createdAt 차이를 보장)
        entityManager.persistAndFlush(notice1);

        try {
            Thread.sleep(10); // 10ms 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        entityManager.persistAndFlush(notice2);

        try {
            Thread.sleep(10); // 10ms 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        entityManager.persistAndFlush(notice3);
    }

    @Test
    @DisplayName("전체 공지사항 목록 조회 - 최신순")
    void findAll_OrderByCreatedAtDesc() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Notices> noticePage = noticeRepository.findAll(pageable);

        // then
        assertThat(noticePage.getContent()).hasSize(3);
        assertThat(noticePage.getTotalElements()).isEqualTo(3);
        assertThat(noticePage.getNumber()).isEqualTo(0);

        // 최신순 정렬 확인 - 마지막에 저장된 notice3가 첫 번째여야 함
        List<Notices> notices = noticePage.getContent();

        // 더 안정적인 검증: 시간 순서로 정렬되었는지 확인
        for (int i = 0; i < notices.size() - 1; i++) {
            assertThat(notices.get(i).getCreatedAt())
                    .isAfterOrEqualTo(notices.get(i + 1).getCreatedAt());
        }

        // 마지막에 저장된 notice3가 첫 번째 위치에 있는지 확인
        assertThat(notices.get(0).getTitle()).isEqualTo("긴급 알림");
    }

    @Test
    @DisplayName("전체 공지사항 목록 조회 - 조회수 높은 순")
    void findAll_OrderByViewCountDesc() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "viewCount"));

        // when
        Page<Notices> noticePage = noticeRepository.findAll(pageable);

        // then
        assertThat(noticePage.getContent()).hasSize(3);

        // 조회수 높은 순 확인
        List<Notices> notices = noticePage.getContent();
        assertThat(notices.get(0).getViewCount()).isEqualTo(20L); // notice3
        assertThat(notices.get(1).getViewCount()).isEqualTo(10L); // notice1
        assertThat(notices.get(2).getViewCount()).isEqualTo(5L);  // notice2
    }

    @Test
    @DisplayName("전체 공지사항 목록 조회 - 페이징")
    void findAll_WithPaging() {
        // given
        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Notices> noticePage = noticeRepository.findAll(pageable);

        // then
        assertThat(noticePage.getContent()).hasSize(2);
        assertThat(noticePage.getTotalElements()).isEqualTo(3);
        assertThat(noticePage.getTotalPages()).isEqualTo(2);
        assertThat(noticePage.hasNext()).isTrue();
        assertThat(noticePage.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("전체 공지사항 목록 조회 - 두 번째 페이지")
    void findAll_SecondPage() {
        // given
        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Notices> noticePage = noticeRepository.findAll(pageable);

        // then
        assertThat(noticePage.getContent()).hasSize(1); // 마지막 페이지에는 1개
        assertThat(noticePage.getTotalElements()).isEqualTo(3);
        assertThat(noticePage.getTotalPages()).isEqualTo(2);
        assertThat(noticePage.hasNext()).isFalse();
        assertThat(noticePage.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("전체 공지사항 목록 조회 - 오래된순")
    void findAll_OrderByCreatedAtAsc() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));

        // when
        Page<Notices> noticePage = noticeRepository.findAll(pageable);

        // then
        assertThat(noticePage.getContent()).hasSize(3);

        // 오래된순 정렬 확인
        List<Notices> notices = noticePage.getContent();
        for (int i = 0; i < notices.size() - 1; i++) {
            assertThat(notices.get(i).getCreatedAt())
                    .isBeforeOrEqualTo(notices.get(i + 1).getCreatedAt());
        }

        // 첫 번째로 저장된 notice1이 첫 번째 위치에 있는지 확인
        assertThat(notices.get(0).getTitle()).isEqualTo("첫 번째 공지사항");
    }
}