package com.team5.catdogeats.support.domain.notice.repository;

import com.team5.catdogeats.storage.domain.mapping.NoticeFiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoticeFilesRepository extends JpaRepository<NoticeFiles, String> {

    List<NoticeFiles> findByNoticesId(String noticeId);

    // 공지사항에 연결된 파일 목록 조회
    @Query("SELECT nf from NoticeFiles nf JOIN FETCH nf.files WHERE nf.notices.id = :noticeId")
    List<NoticeFiles> findByNoticeIdWithFiles(@Param("noticeId") String noticeId);

    @Modifying
    @Query("DELETE FROM NoticeFiles nf WHERE nf.notices.id = :noticeId")
    void deleteByNoticesId(@Param("noticeId") String noticeId);

    Optional<NoticeFiles> findByNoticesIdAndFilesId(String noticeId, String fileId);
}
