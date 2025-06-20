package com.team5.catdogeats.support.domain.notice.service.impl;

import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.storage.domain.mapping.NoticeFiles;
import com.team5.catdogeats.storage.domain.repository.FilesRepository;
import com.team5.catdogeats.support.domain.Notices;
import com.team5.catdogeats.support.domain.notice.dto.*;
import com.team5.catdogeats.support.domain.notice.repository.NoticeFilesRepository;
import com.team5.catdogeats.support.domain.notice.repository.NoticeRepository;
import com.team5.catdogeats.support.domain.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final FilesRepository filesRepository;
    private final NoticeFilesRepository noticeFilesRepository;

    @Value("${app.upload.dir:uploads/notices}")
    private String uploadDir;

    // ========== 공지사항 목록 조회 ==========
    @Override
    public NoticeListResponseDTO getNotices(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notices> noticePage;

        // 검색어가 있으면 검색 조회, 없으면 전체 조회
        if (search != null && !search.trim().isEmpty()) {
            noticePage = noticeRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(search.trim(), pageable);
            log.info("공지사항 검색 조회 - 검색어: {}, 페이지: {}, 사이즈: {}", search, page, size);
        } else {
            noticePage = noticeRepository.findAllOrderByCreatedAtDesc(pageable);
            log.info("공지사항 전체 조회 - 페이지: {}, 사이즈: {}", page, size);
        }

        Page<NoticeResponseDTO> responsePage = noticePage.map(NoticeResponseDTO::from);
        return NoticeListResponseDTO.from(responsePage);
    }

    // ========== 공지사항 상세 조회 ==========
    @Override
    public NoticeResponseDTO getNotice(UUID noticeId) {
        Notices notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

        log.info("공지사항 상세 조회 - ID: {}, 제목: {}", noticeId, notice.getTitle());
        return NoticeResponseDTO.from(notice);
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
    public NoticeResponseDTO updateNotice(NoticeUpdateRequestDTO requestDTO) {
        Notices notice = noticeRepository.findById(requestDTO.getId())
                .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + requestDTO.getId()));

        notice.setTitle(requestDTO.getTitle());
        notice.setContent(requestDTO.getContent());

        Notices updatedNotice = noticeRepository.save(notice);
        log.info("공지사항 수정 완료 - ID: {}, 제목: {}", updatedNotice.getId(), updatedNotice.getTitle());

        return NoticeResponseDTO.from(updatedNotice);
    }

    // ========== 공지사항 삭제 ==========
    @Override
    @Transactional
    public void deleteNotice(UUID noticeId) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId);
        }

        noticeFilesRepository.deleteByNoticesId(noticeId);
        noticeRepository.deleteById(noticeId);
        log.info("공지사항 삭제 완료 - ID: {}", noticeId);
    }

    // ========== 파일 업로드 ==========
    @Override
    @Transactional
    public NoticeResponseDTO uploadFile(UUID noticeId, MultipartFile file) {
        Notices notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공지사항을 찾을 수 없습니다. ID: " + noticeId));

        try {
            // 1. 파일을 서버에 저장
            String savedFilePath = saveFileToStorage(file);

            // 2. Files 테이블에 파일 정보 저장
            Files fileEntity = Files.builder()
                    .fileUrl(savedFilePath)
                    .build();
            Files savedFile = filesRepository.save(fileEntity);

            // 3. 공지사항과 파일 연결 (notice_files 테이블)
            NoticeFiles noticeFile = NoticeFiles.builder()
                    .notices(notice)
                    .files(savedFile)
                    .build();
            noticeFilesRepository.save(noticeFile);

            log.info("파일 업로드 완료 - 공지사항 ID: {}, 파일 ID: {}, 파일명: {}",
                    noticeId, savedFile.getId(), file.getOriginalFilename());

            // 첨부파일 포함된 상세 정보 반환으로 변경
            List<NoticeFiles> updatedNoticeFiles = noticeFilesRepository.findByNoticesId(noticeId);
            return NoticeResponseDTO.fromWithAttachments(notice, updatedNoticeFiles);

        } catch (IOException e) {
            log.error("파일 업로드 실패 - 공지사항 ID: {}, 오류: {}", noticeId, e.getMessage());
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    // ========== 파일 다운로드 ==========
    @Override
    public Resource downloadFile(UUID fileId) {
        // 파일 정보 조회
        Files fileEntity = filesRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다: " + fileId));

        try {
            // 실제 파일 경로에서 리소스 생성
            Path filePath = Paths.get(fileEntity.getFileUrl());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("파일을 읽을 수 없습니다: " + fileId);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("파일을 찾을 수 없습니다: " + fileId, e);
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
}