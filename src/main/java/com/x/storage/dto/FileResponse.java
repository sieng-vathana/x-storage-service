package com.x.storage.dto;

import java.time.LocalDateTime;

public record FileResponse(
        Long id,
        String originalName,
        String contentType,
        long sizeBytes,
        String url,
        String ownerType,
        String ownerId,
        LocalDateTime createdAt
) {
}
