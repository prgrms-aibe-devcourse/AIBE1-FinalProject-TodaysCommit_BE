package com.team5.catdogeats.support.domain.notice.repository;

import com.team5.catdogeats.support.domain.Notices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("공지사항 Repository CRUD 테스트")
class NoticeRepositoryCRUDTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NoticeRepository noticeRepository;

    private Notices testNotice;

    @BeforeEach
    void setUp() {
        testNotice = Notices.builder()
                .title("테스트 공지사항")
                .content("테스트 공지사항의 내용입니다.")
                .viewCount(10L)
                .build();

        testNotice = entityManager.persistAndFlush(testNotice);
    }

    @Test
    @DisplayName("공지사항 저장 - 성공")
    void save_Success() {
        // given
        Notices newNotice = Notices.builder()
                .title("새로운 공지사항")
                .content("새로운 내용입니다.")
                .viewCount(0L)
                .build();

        // when
        Notices savedNotice = noticeRepository.save(newNotice);

        // then
        assertThat(savedNotice).isNotNull();
        assertThat(savedNotice.getId()).isNotNull();
        assertThat(savedNotice.getTitle()).isEqualTo("새로운 공지사항");
        assertThat(savedNotice.getContent()).isEqualTo("새로운 내용입니다.");
        assertThat(savedNotice.getViewCount()).isEqualTo(0L);
        assertThat(savedNotice.getCreatedAt()).isNotNull();
        assertThat(savedNotice.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("ID로 공지사항 조회 - 성공")
    void findById_Success() {
        // when
        Optional<Notices> foundNotice = noticeRepository.findById(testNotice.getId());

        // then
        assertThat(foundNotice).isPresent();
        assertThat(foundNotice.get().getTitle()).isEqualTo("테스트 공지사항");
        assertThat(foundNotice.get().getContent()).isEqualTo("테스트 공지사항의 내용입니다.");
        assertThat(foundNotice.get().getViewCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("ID로 공지사항 조회 - 존재하지 않는 ID")
    void findById_NotFound() {
        // when
        Optional<Notices> foundNotice = noticeRepository.findById("non-existing-id");

        // then
        assertThat(foundNotice).isEmpty();
    }

    @Test
    @DisplayName("공지사항 수정 - 성공")
    void update_Success() {
        // given
        testNotice.setTitle("수정된 제목");
        testNotice.setContent("수정된 내용");

        // when
        Notices updatedNotice = noticeRepository.save(testNotice);

        // then
        assertThat(updatedNotice.getTitle()).isEqualTo("수정된 제목");
        assertThat(updatedNotice.getContent()).isEqualTo("수정된 내용");
        assertThat(updatedNotice.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("공지사항 삭제 - 성공")
    void delete_Success() {
        // given
        String noticeId = testNotice.getId();

        // when
        noticeRepository.deleteById(noticeId);

        // then
        Optional<Notices> deletedNotice = noticeRepository.findById(noticeId);
        assertThat(deletedNotice).isEmpty();

        // 전체 개수 확인
        long count = noticeRepository.count();
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("조회수 증가 - 성공")
    void incrementViewCount_Success() {
        // given
        Long originalViewCount = testNotice.getViewCount();

        // when
        testNotice.incrementViewCount();
        Notices updatedNotice = noticeRepository.save(testNotice);

        // then
        assertThat(updatedNotice.getViewCount()).isEqualTo(originalViewCount + 1);
    }

    @Test
    @DisplayName("공지사항 존재 여부 확인 - 존재함")
    void existsById_True() {
        // when
        boolean exists = noticeRepository.existsById(testNotice.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("공지사항 존재 여부 확인 - 존재하지 않음")
    void existsById_False() {
        // when
        boolean exists = noticeRepository.existsById("non-existing-id");

        // then
        assertThat(exists).isFalse();
    }
}