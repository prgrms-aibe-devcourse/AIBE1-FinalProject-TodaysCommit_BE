package com.team5.catdogeats.storage.domain.repository;

import com.team5.catdogeats.storage.domain.Files;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FilesRepository extends JpaRepository<Files, UUID> {

    // 기본 CRUD 메서드들이 자동으로 제공됩니다
    // save(), findById(), deleteById() 등
}