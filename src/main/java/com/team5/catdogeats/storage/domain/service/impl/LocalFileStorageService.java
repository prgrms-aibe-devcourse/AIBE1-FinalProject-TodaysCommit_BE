package com.team5.catdogeats.storage.domain.service.impl;

import com.team5.catdogeats.storage.domain.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 로컬 파일 시스템 저장소 구현체
// AWS S3 구축 전까지 임시로 사용
@Service
@Primary // 현재 기본 구현체로 사용
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.upload.dir:uploads/notices}")
    private String uploadDir;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        // 1. 업로드 디렉토리 생성
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("업로드 디렉토리 생성: {}", uploadPath.toAbsolutePath());
        }

        // 2. 파일명 생성 (중복 방지)
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 올바르지 않습니다.");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = timestamp + "_" + originalFileName;
        Path filePath = uploadPath.resolve(fileName);

        // 3. 파일 저장
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("파일 업로드 완료: {} -> {}", originalFileName, filePath.toAbsolutePath());

        // 4. 파일 경로 반환 (절대 경로)
        return filePath.toAbsolutePath().toString();
    }

    @Override
    public Resource downloadFile(String fileUrl) throws IOException {
        try {
            Path filePath = Paths.get(fileUrl);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("파일 다운로드 준비 완료: {}", fileUrl);
                return resource;
            } else {
                throw new IOException("파일을 찾을 수 없거나 읽을 수 없습니다: " + fileUrl);
            }
        } catch (MalformedURLException e) {
            throw new IOException("잘못된 파일 경로입니다: " + fileUrl, e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) throws IOException {
        try {
            Path filePath = Paths.get(fileUrl);
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                log.info("파일 삭제 완료: {}", fileUrl);
            } else {
                log.warn("삭제할 파일이 존재하지 않습니다: {}", fileUrl);
            }
        } catch (Exception e) {
            throw new IOException("파일 삭제 실패: " + fileUrl, e);
        }
    }

    @Override
    public String extractFileName(String fileUrl) {
        try {
            String fileName = Paths.get(fileUrl).getFileName().toString();
            // 타임스탬프_원본파일명 형태에서 원본파일명만 추출
            if (fileName.matches("\\d{8}_\\d{6}_.*")) {
                return fileName.substring(fileName.indexOf('_', fileName.indexOf('_') + 1) + 1);
            }
            return fileName;
        } catch (Exception e) {
            log.warn("파일명 추출 실패: {}", fileUrl);
            return "파일명 불명";
        }
    }
}