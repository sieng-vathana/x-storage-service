package com.x.storage.service;

import com.x.storage.domain.StorageKind;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Backend for binary bytes: local disk or S3.
 */
public interface ObjectStorage {

    /**
     * Store file under {@code media/} or {@code files/}. Returns object key / relative path.
     */
    String store(MultipartFile file, StorageKind kind, String storedFileName);

    /**
     * Store raw bytes (e.g. base64 JSON upload). Returns object key / relative path.
     */
    String storeBytes(byte[] data, String contentType, StorageKind kind, String storedFileName);

    Resource load(String relativePath);

    void delete(String relativePath);
}
