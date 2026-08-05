package com.x.storage.service;

import com.x.redis.cache.CacheNames;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageServiceCacheTest {

    @Test
    void cachesResolvedFileMetadataByRelativePath() throws NoSuchMethodException {
        Cacheable cacheable = StorageService.class
                .getMethod("getMetaByRelativePath", String.class)
                .getAnnotation(Cacheable.class);

        assertEquals(List.of(CacheNames.STORAGE_FILE_BY_RELATIVE_PATH), List.of(cacheable.cacheNames()));
        assertEquals("#relativePath", cacheable.key());
    }

    @Test
    void deletingAFileClearsResolvedPathEntries() throws NoSuchMethodException {
        CacheEvict cacheEvict = StorageService.class
                .getMethod("delete", Long.class)
                .getAnnotation(CacheEvict.class);

        assertEquals(List.of(CacheNames.STORAGE_FILE_BY_RELATIVE_PATH), List.of(cacheEvict.cacheNames()));
        assertTrue(cacheEvict.allEntries());
    }
}
