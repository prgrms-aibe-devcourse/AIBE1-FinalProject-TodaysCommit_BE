package com.team5.catdogeats.support.domain.notice.repository;

import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.storage.domain.mapping.NoticeFiles;
import com.team5.catdogeats.storage.domain.repository.FilesRepository;
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
@DisplayName("공지사항-파일 매핑 Repository CRUD 테스트")
class NoticeFilesRepositoryCRUDTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NoticeFilesRepository noticeFilesRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private FilesRepository filesRepository;

    private Notices notice1;
    private Files file1;
    private NoticeFiles noticeFile1;

    @BeforeEach
    void setUp() {
        // 공지사항 데이터 준비
        notice1 = Notices.builder()
                .title("첫 번째 공지사항")
                .content("첫 번째 공지사항의 내용입니다.")
                .viewCount(0L)
                .build();

        // 파일 데이터 준비
        file1 = Files.builder()
                .fileUrl("/uploads/file1.txt")
                .build();

        // 엔티티 저장
        notice1 = entityManager.persistAndFlush(notice1);
        file1 = entityManager.persistAndFlush(file1);

        // 매핑 관계 생성
        noticeFile1 = NoticeFiles.builder()
                .notices(notice1)
                .files(file1)
                .build();

        // 매핑 관계 저장
        noticeFile1 = entityManager.persistAndFlush(noticeFile1);
    }

    @Test
    @DisplayName("공지사항-파일 매핑 저장 - 성공")
    void save_Success() {
        // given
        Files newFile = Files.builder()
                .fileUrl("/uploads/new-file.jpg")
                .build();
        newFile = entityManager.persistAndFlush(newFile);

        NoticeFiles newNoticeFile = NoticeFiles.builder()
                .notices(notice1)
                .files(newFile)
                .build();

        // when
        NoticeFiles savedNoticeFile = noticeFilesRepository.save(newNoticeFile);

        // then
        assertThat(savedNoticeFile).isNotNull();
        assertThat(savedNoticeFile.getId()).isNotNull();
        assertThat(savedNoticeFile.getNotices().getId()).isEqualTo(notice1.getId());
        assertThat(savedNoticeFile.getFiles().getId()).isEqualTo(newFile.getId());
        assertThat(savedNoticeFile.getCreatedAt()).isNotNull();
        assertThat(savedNoticeFile.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("매핑 관계 조회 - findById 성공")
    void findById_Success() {
        // when
        Optional<NoticeFiles> result = noticeFilesRepository.findById(noticeFile1.getId());

        // then
        assertThat(result).isPresent();
        NoticeFiles noticeFile = result.get();
        assertThat(noticeFile.getNotices().getId()).isEqualTo(notice1.getId());
        assertThat(noticeFile.getFiles().getId()).isEqualTo(file1.getId());
    }

    @Test
    @DisplayName("매핑 관계 조회 - findById 결과 없음")
    void findById_NotFound() {
        // when
        Optional<NoticeFiles> result = noticeFilesRepository.findById("non-existing-id");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("개별 매핑 관계 삭제 - deleteById 성공")
    void deleteById_Success() {
        // given
        String noticeFileId = noticeFile1.getId();

        // 삭제 전 확인
        assertThat(noticeFilesRepository.findById(noticeFileId)).isPresent();

        // when
        noticeFilesRepository.deleteById(noticeFileId);
        entityManager.flush();

        // then
        assertThat(noticeFilesRepository.findById(noticeFileId)).isEmpty();
    }

    @Test
    @DisplayName("매핑 관계 존재 여부 확인 - existsById True")
    void existsById_True() {
        // when
        boolean exists = noticeFilesRepository.existsById(noticeFile1.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("매핑 관계 존재 여부 확인 - existsById False")
    void existsById_False() {
        // when
        boolean exists = noticeFilesRepository.existsById("non-existing-id");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("전체 매핑 관계 개수 조회")
    void count_Success() {
        // when
        long count = noticeFilesRepository.count();

        // then
        assertThat(count).isEqualTo(1);
    }
}