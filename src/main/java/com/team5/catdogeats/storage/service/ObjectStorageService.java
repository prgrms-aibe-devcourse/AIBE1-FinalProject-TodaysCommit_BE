package com.team5.catdogeats.storage.service;

import java.io.InputStream;

public interface ObjectStorageService {
    String uploadImage(String key, InputStream inputStream, long contentLength, String contentType);
    String uploadFile(String key, InputStream inputStream, long contentLength, String contentType);
    void deleteFile(String key);
    void deleteImage(String key);
}
