package com.team5.catdogeats.support.domain.notice.repository;

import com.team5.catdogeats.support.domain.Notices;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface NoticeRepository extends JpaRepository<Notices, String> {

    // 검색 조회 (제목 + 내용 검색)
    @Query("SELECT n FROM Notices n WHERE n.title LIKE %:keyword% OR n.content LIKE %:keyword%")
    Page<Notices> findByTitleOrContentContaining(@Param("keyword") String keyword, Pageable pageable);

    // 조회수 증가
    @Modifying
    @Transactional
    @Query("UPDATE Notices n SET n.viewCount = n.viewCount + 1 WHERE n.id = :noticeId")
    void incrementViewCount(@Param("noticeId") String noticeId);
}