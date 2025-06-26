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

    // 전체 목록 조회 (N+1 쿼리 해결)
    @Query("SELECT DISTINCT n FROM Notices n " +
            "LEFT JOIN FETCH n.noticeFiles nf " +
            "LEFT JOIN FETCH nf.files")
    Page<Notices> findAllWithFiles(Pageable pageable);

    // 검색 조회 (N+1 쿼리 해결)
    @Query("SELECT DISTINCT n FROM Notices n " +
            "LEFT JOIN FETCH n.noticeFiles nf " +
            "LEFT JOIN FETCH nf.files " +
            "WHERE lower(n.title) LIKE lower(CONCAT('%', :keyword, '%')) OR " +
            "lower(n.content) LIKE lower(CONCAT('%', :keyword, '%'))")
    Page<Notices> findByTitleOrContentContainingWithFiles(@Param("keyword") String keyword, Pageable pageable);

    // 조회수 증가
    @Modifying
    @Transactional
    @Query("UPDATE Notices n SET n.viewCount = n.viewCount + 1 WHERE n.id = :noticeId")
    void incrementViewCount(@Param("noticeId") String noticeId);
}