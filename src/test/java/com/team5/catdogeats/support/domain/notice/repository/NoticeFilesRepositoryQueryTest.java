package com.team5.catdogeats.support.domain.notice.repository;

import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.storage.domain.mapping.NoticeFiles;
import com.team5.catdogeats.storage.repository.FilesRepository;
import com.team5.catdogeats.support.domain.Notices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("공지사항-파일 매핑 Repository 조회 테스트")
class NoticeFilesRepositoryQueryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NoticeFilesRepository noticeFilesRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private FilesRepository filesRepository;

    private Notices notice1;
    private Notices notice2;
    private Files file1;
    private Files file2;
    private Files file3;
    private NoticeFiles noticeFile1;
    private NoticeFiles noticeFile2;
    private NoticeFiles noticeFile3;

    @BeforeEach
    void setUp() {
        // 공지사항 데이터 준비
        notice1 = Notices.builder()
                .title("첫 번째 공지사항")
                .content("첫 번째 공지사항의 내용입니다.")
                .viewCount(0L)
                .build();

        notice2 = Notices.builder()
                .title("두 번째 공지사항")
                .content("두 번째 공지사항의 내용입니다.")
                .viewCount(0L)
                .build();

        // 파일 데이터 준비
        file1 = Files.builder()
                .fileUrl("https://cdn.example.com/files/file1.pdf")
                .build();

        file2 = Files.builder()
                .fileUrl("https://cdn.example.com/files/file2.pdf")
                .build();

        file3 = Files.builder()
                .fileUrl("https://cdn.example.com/files/file3.docx")
                .build();

        // 엔티티 저장
        notice1 = entityManager.persistAndFlush(notice1);
        notice2 = entityManager.persistAndFlush(notice2);
        file1 = entityManager.persistAndFlush(file1);
        file2 = entityManager.persistAndFlush(file2);
        file3 = entityManager.persistAndFlush(file3);

        // 매핑 관계 생성 (notice1에 2개 파일, notice2에 1개 파일)
        noticeFile1 = NoticeFiles.builder()
                .notices(notice1)
                .files(file1)
                .build();

        noticeFile2 = NoticeFiles.builder()
                .notices(notice1)
                .files(file2)
                .build();

        noticeFile3 = NoticeFiles.builder()
                .notices(notice2)
                .files(file3)
                .build();

        // 매핑 관계 저장
        noticeFile1 = entityManager.persistAndFlush(noticeFile1);
        noticeFile2 = entityManager.persistAndFlush(noticeFile2);
        noticeFile3 = entityManager.persistAndFlush(noticeFile3);
    }

    @Test
    @DisplayName("공지사항에 연결된 파일 목록 조회 - findByNoticesId 성공")
    void findByNoticesId_Success() {
        // when
        List<NoticeFiles> result = noticeFilesRepository.findByNoticesId(notice1.getId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(nf -> nf.getFiles().getFileUrl())
                .containsExactlyInAnyOrder("https://cdn.example.com/files/file1.pdf", "https://cdn.example.com/files/file2.pdf");
    }

    @Test
    @DisplayName("공지사항에 연결된 파일 목록 조회 - findByNoticesId 결과 없음")
    void findByNoticesId_NoResult() {
        // when
        List<NoticeFiles> result = noticeFilesRepository.findByNoticesId("non-existing-notice-id");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("공지사항에 연결된 파일 목록 조회 with JOIN FETCH - findByNoticeIdWithFiles 성공")
    void findByNoticeIdWithFiles_Success() {
        // when
        List<NoticeFiles> result = noticeFilesRepository.findByNoticeIdWithFiles(notice1.getId());

        // then
        assertThat(result).hasSize(2);

        // FETCH JOIN으로 파일 정보가 즉시 로딩되었는지 확인
        result.forEach(noticeFile -> {
            assertThat(noticeFile.getFiles()).isNotNull();
            assertThat(noticeFile.getFiles().getFileUrl()).isNotNull();
        });

        // 파일 URL 확인
        assertThat(result)
                .extracting(nf -> nf.getFiles().getFileUrl())
                .containsExactlyInAnyOrder("https://cdn.example.com/files/file1.pdf", "https://cdn.example.com/files/file2.pdf");
    }

    @Test
    @DisplayName("공지사항에 연결된 파일 목록 조회 with JOIN FETCH - findByNoticeIdWithFiles 결과 없음")
    void findByNoticeIdWithFiles_NoResult() {
        // when
        List<NoticeFiles> result = noticeFilesRepository.findByNoticeIdWithFiles("non-existing-notice-id");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("여러 공지사항의 파일 매핑 조회")
    void findByNoticesId_MultipleNotices() {
        // when
        List<NoticeFiles> notice1Files = noticeFilesRepository.findByNoticesId(notice1.getId());
        List<NoticeFiles> notice2Files = noticeFilesRepository.findByNoticesId(notice2.getId());

        // then
        assertThat(notice1Files).hasSize(2);
        assertThat(notice2Files).hasSize(1);

        // 각 공지사항에 올바른 파일들이 연결되어 있는지 확인
        assertThat(notice1Files)
                .extracting(nf -> nf.getFiles().getFileUrl())
                .containsExactlyInAnyOrder("https://cdn.example.com/files/file1.pdf", "https://cdn.example.com/files/file2.pdf");

        assertThat(notice2Files)
                .extracting(nf -> nf.getFiles().getFileUrl())
                .containsExactly("https://cdn.example.com/files/file3.docx");
    }
}