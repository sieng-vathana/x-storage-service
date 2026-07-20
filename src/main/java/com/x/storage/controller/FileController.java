package com.x.storage.controller;

import com.x.storage.dto.FileResponse;
import com.x.storage.entity.StoredFile;
import com.x.storage.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Upload a file. Optional ownerType/ownerId e.g. product / 42.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "ownerType", required = false) String ownerType,
            @RequestParam(value = "ownerId", required = false) String ownerId) {
        return storageService.store(file, ownerType, ownerId);
    }

    @GetMapping("/{id}")
    public FileResponse meta(@PathVariable Long id) {
        return storageService.getMeta(id);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(@PathVariable Long id) {
        StoredFile meta = storageService.find(id);
        Resource resource = storageService.loadAsResource(id);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (meta.getContentType() != null && !meta.getContentType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(meta.getContentType());
            } catch (Exception ignored) {
                // keep octet-stream
            }
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + meta.getOriginalName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        storageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
