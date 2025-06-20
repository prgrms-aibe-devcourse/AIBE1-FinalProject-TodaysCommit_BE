package com.team5.catdogeats.support.domain.notice.repository;

import com.team5.catdogeats.support.domain.Notices;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends JpaRepository<Notices, String> {

//    제목으로 검색 (대소문자 구분 X)
    Page<Notices> findByTitleContainingIgnoreCase(String title, Pageable pageable);

//    최신순 정렬
    @Query("SELECT n FROM Notices n ORDER BY n.createdAt DESC")
    Page<Notices> findAllOrderByCreatedAtDesc(Pageable pageable);

//    제목 검색 + 최신순 정렬
    @Query("SELECT n FROM Notices n WHERE lower(n.title) LIKE lower(CONCAT('%', :title, '%')) ORDER BY n.createdAt DESC")
    Page<Notices> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(@Param("title") String title, Pageable pageable);

}
