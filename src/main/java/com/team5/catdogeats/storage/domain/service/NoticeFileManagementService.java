package com.team5.catdogeats.storage.domain.service;

import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.support.domain.notice.dto.NoticeFileDownloadResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface NoticeFileManagementService {

    // 공지사항 파일 업로드 및 DB 저장
    Files uploadNoticeFile(MultipartFile file);

    // 공지사항 파일 다운로드
    NoticeFileDownloadResponseDTO downloadNoticeFile(String fileId);

    //공지사항 파일 수정(교체)
    void replaceNoticeFile(String fileId, MultipartFile newFile);

    // 공지사항 파일 완전 삭제 (Storage + DB)
    void deleteNoticeFileCompletely(String fileId);

    // 공지사항 파일을 Storage에서만 삭제 (DB 유지)
    void deleteNoticeFileFromStorageOnly(String fileUrl);
}