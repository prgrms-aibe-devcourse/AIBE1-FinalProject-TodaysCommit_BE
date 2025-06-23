package com.team5.catdogeats.support.domain.notice.service;

import com.team5.catdogeats.support.domain.notice.dto.NoticeCreateRequestDTO;
import com.team5.catdogeats.support.domain.notice.dto.NoticeListResponseDTO;
import com.team5.catdogeats.support.domain.notice.dto.NoticeResponseDTO;
import com.team5.catdogeats.support.domain.notice.dto.NoticeUpdateRequestDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface NoticeService {

    // 공지사항 목록 조회 (페이징, 검색, 정렬) - sortBy 파라미터 추가
    NoticeListResponseDTO getNotices(int page, int size, String search, String sortBy);

    // 공지사항 상세 조회 (조회수 증가 포함)
    NoticeResponseDTO getNotice(String noticeId);

    // 공지사항 생성
    NoticeResponseDTO createNotice(NoticeCreateRequestDTO requestDTO);

    // 공지사항 수정
    NoticeResponseDTO updateNotice(String noticeId, NoticeUpdateRequestDTO requestDTO);

    // 공지사항 삭제
    void deleteNotice(String noticeId);

    // 파일 업로드
    NoticeResponseDTO uploadFile(String noticeId, MultipartFile file);

    // 파일 다운로드
    Resource downloadFile(String fileId);

    // 파일 삭제
    void deleteFile(String noticeId, String fileId);

    // 파일 수정(교체)
    NoticeResponseDTO replaceFile(String noticeId, String fileId, MultipartFile newFile);
}