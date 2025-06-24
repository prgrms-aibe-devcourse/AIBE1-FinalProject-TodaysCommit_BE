package com.team5.catdogeats.storage.controller;

import com.team5.catdogeats.storage.service.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("v1/users/upload")
public class UploadTestController {

    private final ObjectStorageService objectStorageService;

    // 이미지 업로드
    @PostMapping("/image")
    public ResponseEntity<String> uploadImage(@RequestPart MultipartFile file) {
        try {
            String url = objectStorageService.uploadImage(
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
            return ResponseEntity.ok(url);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("이미지 업로드 실패");
        }
    }

    // 파일 업로드
    @PostMapping("/file")
    public ResponseEntity<String> uploadFile(@RequestPart MultipartFile file) {
        try {
            String url = objectStorageService.uploadFile(
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
            return ResponseEntity.ok(url);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 업로드 실패");
        }
    }
}

