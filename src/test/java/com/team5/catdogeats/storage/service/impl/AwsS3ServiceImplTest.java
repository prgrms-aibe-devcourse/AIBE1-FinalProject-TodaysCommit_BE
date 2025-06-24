package com.team5.catdogeats.storage.service.impl;

import com.team5.catdogeats.global.config.AwsS3Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AwsS3ServiceImplTest {
    @Mock
    S3Client s3Client;

    @Mock
    AwsS3Config awsS3Config;

    @InjectMocks
    AwsS3ServiceImpl awsS3Service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(awsS3Config.getBucket()).thenReturn("test-bucket");
        when(awsS3Config.getDomain()).thenReturn("https://cdn.example.com");
    }

    @Test
    @DisplayName("이미지 업로드 성공 - 이미지 폴더에 저장")
    void uploadImage_Success() {
        // given
        String key = "cat.png";
        byte[] data = "hello".getBytes();
        InputStream inputStream = new ByteArrayInputStream(data);
        long size = data.length;
        String contentType = "image/png";

        // when
        String url = awsS3Service.uploadImage(key, inputStream, size, contentType);

        // then
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("test-bucket");
        assertThat(request.key()).isEqualTo("images/" + key);
        assertThat(request.contentType()).isEqualTo(contentType);

        assertThat(url).isEqualTo("https://cdn.example.com/images/" + key);
    }

    @Test
    @DisplayName("파일 업로드 성공 - files 폴더에 저장")
    void uploadFile_Success() {
        // given
        String key = "document.pdf";
        byte[] data = "pdf-content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(data);
        long size = data.length;
        String contentType = "application/pdf";

        // when
        String url = awsS3Service.uploadFile(key, inputStream, size, contentType);

        // then
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("test-bucket");
        assertThat(request.key()).isEqualTo("files/" + key);
        assertThat(request.contentType()).isEqualTo(contentType);

        assertThat(url).isEqualTo("https://cdn.example.com/files/" + key);
    }
}