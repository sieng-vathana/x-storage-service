package com.x.storage.dto;

import java.time.LocalDateTime;

public record FileResponse(
        Long id,
        String originalName,
        String contentType,
        long sizeBytes,
        String url,
        /** S3 object key or local relative path, e.g. media/abc.jpg */
        String relativePath,
        String storageKind,
        String storageBackend,
        String ownerType,
        String ownerId,
        LocalDateTime createdAt
) {
}
