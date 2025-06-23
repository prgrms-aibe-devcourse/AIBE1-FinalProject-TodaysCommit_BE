package com.team5.catdogeats.storage.domain.repository;

import com.team5.catdogeats.storage.domain.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("파일 Repository 테스트")
class FilesRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FilesRepository filesRepository;

    private Files testFile;

    @BeforeEach
    void setUp() {
        testFile = Files.builder()
                .fileUrl("/uploads/test-file.pdf")
                .build();

        testFile = entityManager.persistAndFlush(testFile);
    }

    @Test
    @DisplayName("파일 저장 - 성공")
    void save_Success() {
        // given
        Files file = Files.builder()
                .fileUrl("/uploads/new-file.txt")
                .build();

        // when
        Files savedFile = filesRepository.save(file);

        // then
        assertThat(savedFile).isNotNull();
        assertThat(savedFile.getId()).isNotNull();
        assertThat(savedFile.getFileUrl()).isEqualTo("/uploads/new-file.txt");
        assertThat(savedFile.getCreatedAt()).isNotNull();
        assertThat(savedFile.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("파일 조회 - 성공")
    void findById_Success() {
        // when
        Optional<Files> foundFile = filesRepository.findById(testFile.getId());

        // then
        assertThat(foundFile).isPresent();
        assertThat(foundFile.get().getFileUrl()).isEqualTo("/uploads/test-file.pdf");
    }

    @Test
    @DisplayName("파일 조회 - 존재하지 않는 ID")
    void findById_NotFound() {
        // when
        Optional<Files> foundFile = filesRepository.findById("non-existing-id");

        // then
        assertThat(foundFile).isEmpty();
    }

    @Test
    @DisplayName("파일 삭제 - 성공")
    void delete_Success() {
        // given
        String fileId = testFile.getId();

        // when
        filesRepository.deleteById(fileId);

        // then
        Optional<Files> deletedFile = filesRepository.findById(fileId);
        assertThat(deletedFile).isEmpty();
    }

    @Test
    @DisplayName("모든 파일 조회")
    void findAll_Success() {
        // given
        Files file1 = Files.builder()
                .fileUrl("/uploads/file1.jpg")
                .build();
        Files file2 = Files.builder()
                .fileUrl("/uploads/file2.docx")
                .build();

        entityManager.persistAndFlush(file1);
        entityManager.persistAndFlush(file2);

        // when
        List<Files> allFiles = filesRepository.findAll();

        // then
        assertThat(allFiles).hasSize(3); // testFile + 2개 추가 파일
        assertThat(allFiles)
                .extracting(Files::getFileUrl)
                .containsExactlyInAnyOrder(
                        "/uploads/test-file.pdf",
                        "/uploads/file1.jpg",
                        "/uploads/file2.docx"
                );
    }

    @Test
    @DisplayName("파일 개수 조회")
    void count_Success() {
        // given
        Files file1 = Files.builder()
                .fileUrl("/uploads/file1.jpg")
                .build();
        Files file2 = Files.builder()
                .fileUrl("/uploads/file2.docx")
                .build();

        entityManager.persistAndFlush(file1);
        entityManager.persistAndFlush(file2);

        // when
        long count = filesRepository.count();

        // then
        assertThat(count).isEqualTo(3); // testFile + 2개 추가 파일
    }

    @Test
    @DisplayName("파일 존재 여부 확인 - 존재함")
    void existsById_True() {
        // when
        boolean exists = filesRepository.existsById(testFile.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("파일 존재 여부 확인 - 존재하지 않음")
    void existsById_False() {
        // when
        boolean exists = filesRepository.existsById("non-existing-id");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("파일 수정 - 성공")
    void update_Success() {
        // given
        String newFileUrl = "/uploads/updated-file.pdf";

        // when
        testFile.setFileUrl(newFileUrl);
        Files updatedFile = filesRepository.save(testFile);

        // then
        assertThat(updatedFile.getFileUrl()).isEqualTo(newFileUrl);
        assertThat(updatedFile.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("여러 파일 일괄 저장")
    void saveAll_Success() {
        // given
        Files file1 = Files.builder()
                .fileUrl("/uploads/batch1.txt")
                .build();
        Files file2 = Files.builder()
                .fileUrl("/uploads/batch2.txt")
                .build();

        List<Files> filesToSave = List.of(file1, file2);

        // when
        List<Files> savedFiles = filesRepository.saveAll(filesToSave);

        // then
        assertThat(savedFiles).hasSize(2);
        assertThat(savedFiles)
                .extracting(Files::getFileUrl)
                .containsExactlyInAnyOrder("/uploads/batch1.txt", "/uploads/batch2.txt");
    }
}