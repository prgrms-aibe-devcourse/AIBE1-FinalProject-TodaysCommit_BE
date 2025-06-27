package com.team5.catdogeats.storage.domain.service.impl;

import com.team5.catdogeats.global.config.AwsS3Config;
import com.team5.catdogeats.storage.domain.service.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class AwsS3ServiceImpl implements ObjectStorageService {
    private final S3Client s3Client;
    private final AwsS3Config awsS3Config;

    @Override
    public String uploadImage(String key, InputStream inputStream, long contentLength, String contentType) {
        // 예: 이미지 전용 폴더
        String imageKey = "images/" + key;
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(awsS3Config.getBucket())
                        .key(imageKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromInputStream(inputStream, contentLength)
        );
        return awsS3Config.getDomain() + "/" + imageKey;
    }

    @Override
    public String uploadFile(String key, InputStream inputStream, long contentLength, String contentType) {
        // 예: 파일 전용 폴더
        String fileKey = "files/" + key;
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(awsS3Config.getBucket())
                        .key(fileKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromInputStream(inputStream, contentLength)
        );
        return awsS3Config.getDomain() + "/" + fileKey;
    }

    @Override
    public void deleteFile(String key) {
        String fileKey = "files/" + key;
        s3Client.deleteObject(builder -> builder
                .bucket(awsS3Config.getBucket())
                .key(fileKey)
                .build()
        );
    }

    public void deleteImage(String key) {
        String imageKey = "images/" + key;
        s3Client.deleteObject(builder -> builder
                .bucket(awsS3Config.getBucket())
                .key(imageKey)
                .build()
        );
    }

}
