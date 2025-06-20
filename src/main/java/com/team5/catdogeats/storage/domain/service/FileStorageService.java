package com.team5.catdogeats.storage.domain.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


//파일 저장소 서비스 인터페이스
//로컬 파일 시스템 → AWS S3로 쉽게 전환하기 위한 추상화
public interface FileStorageService {


    //파일 업로드
    String uploadFile(MultipartFile file) throws IOException;


    // 파일 다운로드
    Resource downloadFile(String fileUrl) throws IOException;


    // 파일 삭제 (향후 구현 예정)
    void deleteFile(String fileUrl) throws IOException;


    // 파일명 추출
    String extractFileName(String fileUrl);
}