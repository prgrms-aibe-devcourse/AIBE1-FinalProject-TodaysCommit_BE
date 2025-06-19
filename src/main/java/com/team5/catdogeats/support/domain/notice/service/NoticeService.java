package com.team5.catdogeats.support.domain.notice.service;

import com.team5.catdogeats.support.domain.notice.dto.NoticeCreateRequestDTO;
import com.team5.catdogeats.support.domain.notice.dto.NoticeListResponseDTO;
import com.team5.catdogeats.support.domain.notice.dto.NoticeResponseDTO;
import com.team5.catdogeats.support.domain.notice.dto.NoticeUpdateRequestDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface NoticeService {

//    공지사항 목록 조회 (페이징, 검색)
    NoticeListResponseDTO getNotices(int page, int size, String search);

//    공지사항 상세 조회
    NoticeResponseDTO getNotice(UUID noticeId);

//    공지사항 생성
    NoticeResponseDTO createNotice(NoticeCreateRequestDTO requestDTO);

//    공지사항 수정
    NoticeResponseDTO updateNotice(NoticeUpdateRequestDTO requestDTO);

//    공지사항 삭제
    void deleteNotice(UUID noticeId);

//    파일 업로드
    NoticeResponseDTO uploadFile(UUID noticeId, MultipartFile file);

//    파일 다운로드
    Resource downloadFile(UUID fileId);
}
