package com.team5.catdogeats.storage.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Images extends BaseEntity {
    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "image_url", length = 255, nullable = false)
    private String imageUrl;
}