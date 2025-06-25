package com.team5.catdogeats.support.domain.notice.service.impl;

import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.storage.domain.mapping.NoticeFiles;
import com.team5.catdogeats.storage.domain.repository.FilesRepository;
import com.team5.catdogeats.storage.domain.service.ObjectStorageService;
import com.team5.catdogeats.support.domain.Notices;
import com.team5.catdogeats.support.domain.notice.dto.*;
import com.team5.catdogeats.support.domain.notice.repository.NoticeFilesRepository;
import com.team5.catdogeats.support.domain.notice.repository.NoticeRepository;
import com.team5.catdogeats.support.domain.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(value = "jpaTransactionManager", readOnly = true)
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final FilesRepository filesRepository;
    private final NoticeFilesRepository noticeFilesRepository;
    private final ObjectStorageService objectStorageService; // 팀원이 만든 S3 서비스 주입

    private Sort createSort(String sortBy) {
        return switch (sortBy) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "views" -> Sort.by(Sort.Direction.DESC, "viewCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    // ========== 공지사항 목록 조회 ==========
    @Override
    public NoticeListResponseDTO getNotices(int page, int size, String search, String sortBy) {
        Sort sort = createSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Notices> noticePage;

        // JOIN FETCH로 N+1 쿼리 해결
        if (search != null && !search.trim().isEmpty()) {
            noticePage = noticeRepository.findByTitleOrContentContainingWithFiles(search.trim(), pageable);
        } else {
            noticePage = noticeRepository.findAllWithFiles(pageable);
        }

        Page<NoticeResponseDTO> responsePage = noticePage.map(notice ->
                NoticeResponseDTO.fromWithAttachments(notice, notice.getNoticeFiles())
        );

        return NoticeListResponseDTO.from(responsePage);
    }

    // ========== 공지사항 상세 조회 ==========
    @Override
    @JpaTransactional
    public NoticeResponseDTO getNotice(String noticeId) {
        Notices notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

        // 조회수 증가
        notice.incrementViewCount();

        List<NoticeFiles> attachments = noticeFilesRepository.findByNoticesId(noticeId);

        return NoticeResponseDTO.fromWithAttachments(notice, attachments);
    }

    // ========== 공지사항 생성 ==========
    @Override
    @JpaTransactional
    public NoticeResponseDTO createNotice(NoticeCreateRequestDTO requestDTO) {
        Notices notice = Notices.builder()
                .title(requestDTO.getTitle())
                .content(requestDTO.getContent())
                .build();

        Notices savedNotice = noticeRepository.save(notice);
        log.info("공지사항 생성 완료 - ID: {}, 제목: {}", savedNotice.getId(), savedNotice.getTitle());

        return NoticeResponseDTO.from(savedNotice);
    }

    // ========== 공지사항 수정 ==========
    @Override
    @JpaTransactional
    public NoticeResponseDTO updateNotice(String noticeId, NoticeUpdateRequestDTO requestDTO) {
        Notices notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

        notice.setTitle(requestDTO.getTitle());
        notice.setContent(requestDTO.getContent());

        Notices updatedNotice = noticeRepository.save(notice);
        log.info("공지사항 수정 완료 - ID: {}, 제목: {}", updatedNotice.getId(), updatedNotice.getTitle());

        return NoticeResponseDTO.from(updatedNotice);
    }

    // ========== 공지사항 삭제 ==========
    @Override
    @JpaTransactional
    public void deleteNotice(String noticeId) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId);
        }

        // 연결된 파일들을 S3에서 삭제
        List<NoticeFiles> noticeFiles = noticeFilesRepository.findByNoticesId(noticeId);
        for (NoticeFiles noticeFile : noticeFiles) {
            try {
                deleteFileFromS3(noticeFile.getFiles().getFileUrl());
            } catch (Exception e) {
                log.warn("S3 파일 삭제 실패 (무시 가능): {}", e.getMessage());
            }
        }

        noticeFilesRepository.deleteByNoticesId(noticeId);
        noticeRepository.deleteById(noticeId);
        log.info("공지사항 삭제 완료 - ID: {}", noticeId);
    }

    // ========== 파일 업로드 ==========
    @Override
    @JpaTransactional
    public NoticeResponseDTO uploadFile(String noticeId, MultipartFile file) {
        try {
            // 공지사항 존재 확인
            Notices notice = noticeRepository.findById(noticeId)
                    .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

            // 1. S3에 파일 업로드
            String fileName = generateNoticeFileName(file.getOriginalFilename());
            String s3FileUrl = objectStorageService.uploadFile(
                    fileName,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
            log.info("S3 파일 업로드 완료: {}", s3FileUrl);

            // 2. Files 테이블에 파일 정보 저장
            Files fileEntity = Files.builder()
                    .fileUrl(s3FileUrl)
                    .build();
            Files savedFile = filesRepository.save(fileEntity);

            // 3. 공지사항과 파일 연결
            NoticeFiles noticeFile = NoticeFiles.builder()
                    .notices(notice)
                    .files(savedFile)
                    .build();
            noticeFilesRepository.save(noticeFile);

            // 4. 첨부파일 포함된 상세 정보 반환
            List<NoticeFiles> updatedNoticeFiles = noticeFilesRepository.findByNoticesId(noticeId);
            return NoticeResponseDTO.fromWithAttachments(notice, updatedNoticeFiles);

        } catch (IOException e) {
            log.error("S3 파일 업로드 실패 - 공지사항 ID: {}, 오류: {}", noticeId, e.getMessage());
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생 - 공지사항 ID: {}, 오류: {}", noticeId, e.getMessage(), e);
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    // ========== 파일 다운로드 ==========
    @Override
    public Resource downloadFile(String fileId) {
        Files fileEntity = filesRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("파일을 찾을 수 없습니다: " + fileId));

        try {
            String fileUrl = fileEntity.getFileUrl();

            // https:// 프로토콜 추가
            if (!fileUrl.startsWith("http")) {
                fileUrl = "https://" + fileUrl;
            }

            // 파일명 부분만 추출해서 인코딩
            int lastSlash = fileUrl.lastIndexOf('/');
            String basePath = fileUrl.substring(0, lastSlash + 1);
            String fileName = fileUrl.substring(lastSlash + 1);

            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8");
            String finalUrl = basePath + encodedFileName;

            log.info("최종 URL: {}", finalUrl);

            Resource resource = new UrlResource(finalUrl);
            return resource;

        } catch (Exception e) {
            log.error("S3 파일 다운로드 실패 - 파일 ID: {}, 오류: {}", fileId, e.getMessage());
            throw new RuntimeException("파일 다운로드 중 오류가 발생했습니다: " + fileId, e);
        }
    }

    // ========== 파일 삭제 ==========
    @Override
    @JpaTransactional
    public void deleteFile(String noticeId, String fileId) {
        try {
            // 1. 공지사항 존재 확인
            Notices notice = noticeRepository.findById(noticeId)
                    .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

            // 2. 파일 존재 확인
            Files fileEntity = filesRepository.findById(fileId)
                    .orElseThrow(() -> new NoSuchElementException("파일을 찾을 수 없습니다. ID: " + fileId));

            // 3. 해당 공지사항에 파일이 연결되어 있는지 확인
            List<NoticeFiles> noticeFiles = noticeFilesRepository.findByNoticesId(noticeId);
            boolean isFileLinked = noticeFiles.stream()
                    .anyMatch(nf -> nf.getFiles().getId().equals(fileId));

            if (!isFileLinked) {
                throw new IllegalArgumentException("해당 공지사항에 연결되지 않은 파일입니다. noticeId: " + noticeId + ", fileId: " + fileId);
            }

            // 4. notice_files 테이블에서 매핑 관계 삭제
            noticeFiles.stream()
                    .filter(nf -> nf.getFiles().getId().equals(fileId))
                    .forEach(nf -> noticeFilesRepository.deleteById(nf.getId()));

            // 5. S3에서 실제 파일 삭제
            try {
                deleteFileFromS3(fileEntity.getFileUrl());
            } catch (Exception e) {
                log.warn("S3 파일 삭제 실패 (무시 가능): {}", e.getMessage());
            }

            // 6. files 테이블에서 파일 정보 삭제
            filesRepository.deleteById(fileId);

            log.info("파일 삭제 완료 - noticeId: {}, fileId: {}", noticeId, fileId);

        } catch (Exception e) {
            log.error("파일 삭제 실패 - noticeId: {}, fileId: {}, 오류: {}", noticeId, fileId, e.getMessage());
            throw new RuntimeException("파일 삭제 중 오류가 발생했습니다.", e);
        }
    }

    // ========== 파일 수정(교체) ==========
    @Override
    @JpaTransactional
    public NoticeResponseDTO replaceFile(String noticeId, String fileId, MultipartFile newFile) {
        try {
            // 1. 공지사항 존재 확인
            Notices notice = noticeRepository.findById(noticeId)
                    .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

            // 2. 기존 파일 존재 확인
            Files oldFileEntity = filesRepository.findById(fileId)
                    .orElseThrow(() -> new NoSuchElementException("파일을 찾을 수 없습니다. ID: " + fileId));

            // 3. 해당 공지사항에 파일이 연결되어 있는지 확인
            List<NoticeFiles> noticeFiles = noticeFilesRepository.findByNoticesId(noticeId);
            boolean isFileLinked = noticeFiles.stream()
                    .anyMatch(nf -> nf.getFiles().getId().equals(fileId));

            if (!isFileLinked) {
                throw new IllegalArgumentException("해당 공지사항에 연결되지 않은 파일입니다. noticeId: " + noticeId + ", fileId: " + fileId);
            }

            // 4. 새 파일을 S3에 업로드
            String newFileName = generateNoticeFileName(newFile.getOriginalFilename());
            String newFileUrl = objectStorageService.uploadFile(
                    newFileName,
                    newFile.getInputStream(),
                    newFile.getSize(),
                    newFile.getContentType()
            );

            // 5. 기존 파일을 S3에서 삭제
            try {
                deleteFileFromS3(oldFileEntity.getFileUrl());
            } catch (Exception e) {
                log.warn("기존 S3 파일 삭제 실패 (무시 가능): {}", e.getMessage());
            }

            // 6. DB 파일 정보 업데이트
            oldFileEntity.setFileUrl(newFileUrl);
            filesRepository.save(oldFileEntity);

            log.info("S3 파일 교체 완료: {} -> {}", oldFileEntity.getFileUrl(), newFileUrl);

            // 7. 업데이트된 공지사항 정보 반환
            List<NoticeFiles> updatedNoticeFiles = noticeFilesRepository.findByNoticesId(noticeId);
            return NoticeResponseDTO.fromWithAttachments(notice, updatedNoticeFiles);

        } catch (IOException e) {
            log.error("S3 파일 수정(교체) 실패 - noticeId: {}, fileId: {}, 오류: {}", noticeId, fileId, e.getMessage());
            throw new RuntimeException("파일 수정(교체) 중 오류가 발생했습니다.", e);
        }
    }

    // ========== 헬퍼 메서드들 ==========
    // 공지사항 전용 파일명 생성
    // 형식: notice_UUID_타임스탬프_원본파일명
    private String generateNoticeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 올바르지 않습니다.");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "notice_" + uuid + "_" + timestamp + "_" + originalFileName;
    }

    //S3에서 파일 삭제
    private void deleteFileFromS3(String fileUrl) {
        // CloudFront URL에서 key 추출
        String key = extractKeyFromUrl(fileUrl);
        objectStorageService.deleteFile(key);
    }

    // S3 URL에서 key 추출
    private String extractKeyFromUrl(String fileUrl) {
        int filesIndex = fileUrl.indexOf("files/");
        if (filesIndex != -1) {
            return fileUrl.substring(filesIndex);
        }

        // 혹시 다른 형태의 URL이라면 마지막 부분을 파일명으로 가정
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        return "files/" + fileName;
    }
}