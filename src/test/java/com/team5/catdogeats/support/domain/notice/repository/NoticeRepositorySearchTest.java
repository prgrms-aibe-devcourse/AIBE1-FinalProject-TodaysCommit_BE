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
@DisplayName("공지사항 Repository 검색 테스트")
class NoticeRepositorySearchTest {

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

        // 데이터 저장
        entityManager.persistAndFlush(notice1);
        entityManager.persistAndFlush(notice2);
        entityManager.persistAndFlush(notice3);
    }

    @Test
    @DisplayName("제목과 내용으로 검색 - 제목 매칭")
    void findByTitleOrContentContaining_TitleMatch() {
        // given
        String keyword = "첫 번째";
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Notices> result = noticeRepository.findByTitleOrContentContainingWithFiles(keyword, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("첫 번째 공지사항");
    }

    @Test
    @DisplayName("제목과 내용으로 검색 - 내용 매칭")
    void findByTitleOrContentContaining_ContentMatch() {
        // given
        String keyword = "긴급하게";
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Notices> result = noticeRepository.findByTitleOrContentContainingWithFiles(keyword, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("긴급 알림");
    }

    @Test
    @DisplayName("제목과 내용으로 검색 - 대소문자 무시")
    void findByTitleOrContentContaining_CaseInsensitive() {
        // given
        String keyword = "긴급"; // 대문자로 검색
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Notices> result = noticeRepository.findByTitleOrContentContainingWithFiles(keyword, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("긴급 알림");
    }

    @Test
    @DisplayName("제목과 내용으로 검색 - 공통 키워드")
    void findByTitleOrContentContaining_CommonKeyword() {
        // given
        String keyword = "공지사항"; // 제목과 내용 모두에 포함된 키워드
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Notices> result = noticeRepository.findByTitleOrContentContainingWithFiles(keyword, pageable);

        // then
        assertThat(result.getContent()).hasSize(2); // notice1, notice2
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("제목과 내용으로 검색 - 결과 없음")
    void findByTitleOrContentContaining_NoResult() {
        // given
        String keyword = "존재하지않는키워드";
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Notices> result = noticeRepository.findByTitleOrContentContainingWithFiles(keyword, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("검색 결과 정렬 - 조회수 높은 순")
    void findByTitleOrContentContaining_OrderByViewCount() {
        // given
        String keyword = "공지사항";
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "viewCount"));

        // when
        Page<Notices> result = noticeRepository.findByTitleOrContentContainingWithFiles(keyword, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);

        // 조회수 높은 순으로 정렬되었는지 확인
        List<Notices> notices = result.getContent();
        assertThat(notices.get(0).getViewCount()).isGreaterThan(notices.get(1).getViewCount());
    }

    @Test
    @DisplayName("검색 결과 정렬 - 최신순")
    void findByTitleOrContentContaining_OrderByCreatedAtDesc() {
        // given
        String keyword = "내용";
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Notices> result = noticeRepository.findByTitleOrContentContainingWithFiles(keyword, pageable);

        // then
        assertThat(result.getContent()).hasSize(3); // 모든 공지사항이 "내용" 포함

        // 최신순으로 정렬되었는지 확인
        List<Notices> notices = result.getContent();
        for (int i = 0; i < notices.size() - 1; i++) {
            assertThat(notices.get(i).getCreatedAt())
                    .isAfterOrEqualTo(notices.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("검색 결과 정렬 - 오래된순")
    void findByTitleOrContentContaining_OrderByCreatedAtAsc() {
        // given
        String keyword = "내용"; // 모든 공지사항이 "내용" 포함
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));

        // when
        Page<Notices> result = noticeRepository.findByTitleOrContentContainingWithFiles(keyword, pageable);

        // then
        assertThat(result.getContent()).hasSize(3); // 모든 공지사항이 "내용" 포함

        // 오래된순으로 정렬되었는지 확인
        List<Notices> notices = result.getContent();
        for (int i = 0; i < notices.size() - 1; i++) {
            assertThat(notices.get(i).getCreatedAt())
                    .isBeforeOrEqualTo(notices.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("검색 결과 페이징")
    void findByTitleOrContentContaining_WithPaging() {
        // given
        String keyword = "내용"; // 모든 공지사항이 매칭
        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Notices> result = noticeRepository.findByTitleOrContentContainingWithFiles(keyword, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("부분 키워드 검색 - 제목")
    void findByTitleOrContentContaining_PartialTitleKeyword() {
        // given
        String keyword = "알림"; // "긴급 알림"의 부분 키워드
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Notices> result = noticeRepository.findByTitleOrContentContainingWithFiles(keyword, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("긴급 알림");
    }

    @Test
    @DisplayName("부분 키워드 검색 - 내용")
    void findByTitleOrContentContaining_PartialContentKeyword() {
        // given
        String keyword = "알려드릴"; // notice3 내용의 부분 키워드
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Notices> result = noticeRepository.findByTitleOrContentContainingWithFiles(keyword, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("긴급 알림");
    }
}