package com.team5.catdogeats.storage.domain.repository;

import com.team5.catdogeats.storage.domain.Files;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilesRepository extends JpaRepository<Files, String> {

    // 기본 CRUD 메서드들이 자동으로 제공됩니다
    // save(), findById(), deleteById() 등
}