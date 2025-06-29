package com.team5.catdogeats.baseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Column(updatable = false, nullable = false, name = "created_at") // 생성 시간은 업데이트되면 안 됨
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @PrePersist
    public void prePersist() {
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

}
