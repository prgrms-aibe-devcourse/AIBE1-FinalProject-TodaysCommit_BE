package com.team5.catdogeats.storage.repository;

import com.team5.catdogeats.storage.domain.Images;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Images, String> {
}
