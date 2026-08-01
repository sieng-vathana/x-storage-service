package com.x.storage.controller;

import com.x.storage.dto.FileResponse;
import com.x.storage.service.StorageService;
import com.sharedlib.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Cluster-only file lookup used by the BFF; it is not exposed through the public gateway route. */
@RestController
@RequestMapping("/internal/files")
public class InternalFileController {

    private final StorageService storageService;

    public InternalFileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/by-relative-path")
    public ResponseEntity<ApiResponse<FileResponse>> byRelativePath(
            @RequestParam String relativePath) {
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), storageService.getMetaByRelativePath(relativePath)));
    }
}
