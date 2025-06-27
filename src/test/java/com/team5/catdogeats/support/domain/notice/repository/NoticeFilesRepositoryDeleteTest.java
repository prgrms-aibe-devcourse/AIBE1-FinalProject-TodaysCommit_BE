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
@DisplayName("공지사항-파일 매핑 Repository 삭제 테스트")
class NoticeFilesRepositoryDeleteTest {

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
    @DisplayName("공지사항에 연결된 모든 파일 매핑 삭제 - deleteByNoticesId 성공")
    void deleteByNoticesId_Success() {
        // given
        String noticeId = notice1.getId();

        // 삭제 전 매핑 관계 확인
        List<NoticeFiles> beforeDelete = noticeFilesRepository.findByNoticesId(noticeId);
        assertThat(beforeDelete).hasSize(2);

        // when
        noticeFilesRepository.deleteByNoticesId(noticeId);
        entityManager.flush(); // 즉시 DB 반영

        // then
        List<NoticeFiles> afterDelete = noticeFilesRepository.findByNoticesId(noticeId);
        assertThat(afterDelete).isEmpty();

        // 다른 공지사항의 매핑 관계는 유지되는지 확인
        List<NoticeFiles> otherNoticeFiles = noticeFilesRepository.findByNoticesId(notice2.getId());
        assertThat(otherNoticeFiles).hasSize(1);
    }

    @Test
    @DisplayName("공지사항에 연결된 파일 매핑 삭제 - 존재하지 않는 공지사항")
    void deleteByNoticesId_NotFound() {
        // given
        long beforeCount = noticeFilesRepository.count();

        // when
        noticeFilesRepository.deleteByNoticesId("non-existing-notice-id");
        entityManager.flush();

        // then
        long afterCount = noticeFilesRepository.count();
        assertThat(afterCount).isEqualTo(beforeCount); // 변화 없음
    }
}