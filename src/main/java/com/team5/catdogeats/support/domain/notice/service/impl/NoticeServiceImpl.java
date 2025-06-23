package com.team5.catdogeats.support.domain.notice.service.impl;

import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.storage.domain.mapping.NoticeFiles;
import com.team5.catdogeats.storage.domain.repository.FilesRepository;
import com.team5.catdogeats.storage.domain.service.FileStorageService;
import com.team5.catdogeats.support.domain.Notices;
import com.team5.catdogeats.support.domain.notice.dto.*;
import com.team5.catdogeats.support.domain.notice.repository.NoticeFilesRepository;
import com.team5.catdogeats.support.domain.notice.repository.NoticeRepository;
import com.team5.catdogeats.support.domain.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Sort;


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final FilesRepository filesRepository;
    private final NoticeFilesRepository noticeFilesRepository;
    private final FileStorageService fileStorageService;

    private Sort createSort(String sortBy) {
        return switch (sortBy) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "views" -> Sort.by(Sort.Direction.DESC, "viewCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    @Value("${app.upload.dir:uploads/notices}")
    private String uploadDir;

    // ========== 공지사항 목록 조회 ==========
    @Override
    public NoticeListResponseDTO getNotices(int page, int size, String search, String sortBy) {
        Sort sort = createSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Notices> noticePage;

        // 새로운 JOIN FETCH 메서드 사용
        if (search != null && !search.trim().isEmpty()) {
            noticePage = noticeRepository.findByTitleOrContentContainingWithFiles(search.trim(), pageable);
        } else {
            noticePage = noticeRepository.findAllWithFiles(pageable);
        }

        // N+1 쿼리 제거! 이미 파일 정보가 로드되어 있음
        Page<NoticeResponseDTO> responsePage = noticePage.map(notice ->
                NoticeResponseDTO.fromWithAttachments(notice, notice.getNoticeFiles())
        );

        return NoticeListResponseDTO.from(responsePage);
    }

    // ========== 공지사항 상세 조회 ==========
    @Override
    @Transactional
    public NoticeResponseDTO getNotice(String noticeId) {
        Notices notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

        // 조회수 증가
        notice.incrementViewCount();
        noticeRepository.save(notice);

        List<NoticeFiles> attachments = noticeFilesRepository.findByNoticesId(noticeId);

        return NoticeResponseDTO.fromWithAttachments(notice, attachments);
    }

    // ========== 공지사항 생성 ==========
    @Override
    @Transactional
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
    @Transactional
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
    @Transactional
    public void deleteNotice(String noticeId) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId);
        }

        noticeFilesRepository.deleteByNoticesId((noticeId));
        noticeRepository.deleteById(noticeId);
        log.info("공지사항 삭제 완료 - ID: {}", noticeId);
    }

    // ========== 파일 업로드 ==========
    @Override
    @Transactional
    public NoticeResponseDTO uploadFile(String noticeId, MultipartFile file) {

        try {
            // 공지사항 존재 확인
            Notices notice = noticeRepository.findById(noticeId)
                    .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

            // 1. FileStorageService를 통해 파일 저장
            String savedFileUrl = fileStorageService.uploadFile(file);

            // 2. Files 테이블에 파일 정보 저장 (ID 자동 생성)
            Files fileEntity = Files.builder()
                    .fileUrl(savedFileUrl)
                    .build();
            Files savedFile = filesRepository.save(fileEntity);

            // 3. 공지사항과 파일 연결 (notice_files 테이블) (ID 자동 생성)
            NoticeFiles noticeFile = NoticeFiles.builder()
                    .notices(notice)
                    .files(savedFile)
                    .build();
            NoticeFiles savedNoticeFile = noticeFilesRepository.save(noticeFile);

            // 4. 첨부파일 포함된 상세 정보 반환
            List<NoticeFiles> updatedNoticeFiles = noticeFilesRepository.findByNoticesId(noticeId);

            return NoticeResponseDTO.fromWithAttachments(notice, updatedNoticeFiles);

        } catch (IOException e) {
            log.error("파일 업로드 실패 - 공지사항 ID: {}, 오류: {}", noticeId, e.getMessage());
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생 - 공지사항 ID: {}, 오류 타입: {}, 메시지: {}",
                    noticeId, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    // ========== 파일 다운로드 ==========
    @Override
    public Resource downloadFile(String fileId) {
        // 파일 정보 조회
        Files fileEntity = filesRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("파일을 찾을 수 없습니다: " + fileId));

        // 디버깅 로그 추가
        String fileUrl = fileEntity.getFileUrl();

        // 실제 파일 존재 여부 확인
        java.io.File file = new java.io.File(fileUrl);

        if (!file.exists()) {
            log.error("파일이 실제로 존재하지 않습니다: {}", fileUrl);
            throw new RuntimeException("파일이 존재하지 않습니다: " + fileUrl);
        }

        try {
            // FileStorageService를 통해 파일 다운로드
            return fileStorageService.downloadFile(fileEntity.getFileUrl());
        } catch (IOException e) {
            log.error("파일 다운로드 실패 - 파일 ID: {}, 오류: {}", fileId, e.getMessage());
            throw new RuntimeException("파일 다운로드 중 오류가 발생했습니다: " + fileId, e);
        }
    }

    // ========== 파일 저장 (내부 메서드) ==========
    private String saveFileToStorage(MultipartFile file) throws IOException {
        // 업로드 디렉토리 생성
        Path uploadPath = Paths.get(uploadDir);
        if (!java.nio.file.Files.exists(uploadPath)) {
            java.nio.file.Files.createDirectories(uploadPath);
        }

        // 파일명 중복 방지 (현재시간 추가)
        String originalFileName = file.getOriginalFilename();
        String fileName = System.currentTimeMillis() + "_" + originalFileName;
        Path filePath = uploadPath.resolve(fileName);

        // 실제 파일 저장
        java.nio.file.Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath.toString();
    }

    // ========== 파일 삭제 ==========
    @Override
    @Transactional
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

            // 5. 다른 공지사항에서도 사용하는지 확인
            long usageCount = noticeFilesRepository.count(); // 전체 파일 사용 횟수 확인을 위한 더 정확한 쿼리 필요

            // 6. 실제 파일 삭제 (FileStorageService 사용)
            try {
                fileStorageService.deleteFile(fileEntity.getFileUrl());
            } catch (IOException e) {
                log.warn("실제 파일 삭제 실패 (파일이 이미 없을 수 있음) - fileUrl: {}, 오류: {}",
                        fileEntity.getFileUrl(), e.getMessage());
            }

            // 7. files 테이블에서 파일 정보 삭제
            filesRepository.deleteById(fileId);

            log.info("파일 삭제 완료 - noticeId: {}, fileId: {}", noticeId, fileId);

        } catch (Exception e) {
            log.error("파일 삭제 실패 - noticeId: {}, fileId: {}, 오류: {}", noticeId, fileId, e.getMessage());
            throw new RuntimeException("파일 삭제 중 오류가 발생했습니다.", e);
        }
    }

    // ========== 파일 수정(교체) ==========
    @Override
    @Transactional
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

            // 4. FileStorageService를 통한 파일 교체 (핵심!)
            String newFileUrl = fileStorageService.replaceFile(oldFileEntity.getFileUrl(), newFile);

            // 5. DB 파일 정보 업데이트
            oldFileEntity.setFileUrl(newFileUrl);
            filesRepository.save(oldFileEntity);

            // 6. 업데이트된 공지사항 정보 반환
            List<NoticeFiles> updatedNoticeFiles = noticeFilesRepository.findByNoticesId(noticeId);
            return NoticeResponseDTO.fromWithAttachments(notice, updatedNoticeFiles);

        } catch (IOException e) {
            log.error("파일 수정(교체) 실패 - noticeId: {}, fileId: {}, 오류: {}", noticeId, fileId, e.getMessage());
            throw new RuntimeException("파일 수정(교체) 중 오류가 발생했습니다.", e);
        }
    }
}