package com.team5.catdogeats.storage.controller;

import com.team5.catdogeats.storage.domain.service.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("v1/users")
public class UploadTestController {

    private final ObjectStorageService objectStorageService;

    // 이미지 업로드
    @PostMapping("/upload/image")
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
    @PostMapping("/upload/file")
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

    @DeleteMapping(value = "/delete/file")
    public ResponseEntity<Void> deleteFile(@RequestParam String key) {
        objectStorageService.deleteFile(key);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete/image")
    public ResponseEntity<Void> deleteImage(@RequestParam String key) {
        objectStorageService.deleteImage(key);
        return ResponseEntity.noContent().build();
    }

}

