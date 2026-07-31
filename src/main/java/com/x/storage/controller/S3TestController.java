package com.x.storage.controller;

import com.x.storage.dto.S3TestResponse;
import com.x.storage.service.S3TestService;
import com.sharedlib.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Live S3 connectivity check for local/dev. Calls AWS put → get → delete.
 * Hit storage-service directly (port 8088); no auth on this service.
 */
@RestController
@RequestMapping("/api/v1/storage")
public class S3TestController {

    private final S3TestService s3TestService;

    public S3TestController(S3TestService s3TestService) {
        this.s3TestService = s3TestService;
    }

    /**
     * Probe S3: head-bucket, put a tiny object under {@code test/}, read it back, delete it.
     * Returns 200 when all steps succeed, 503 when S3/config fails.
     */
    @GetMapping("/s3-test")
    public ResponseEntity<ApiResponse<S3TestResponse>> testGet() {
        return toResponse(s3TestService.runProbe());
    }

    @PostMapping("/s3-test")
    public ResponseEntity<ApiResponse<S3TestResponse>> testPost() {
        return toResponse(s3TestService.runProbe());
    }

    private static ResponseEntity<ApiResponse<S3TestResponse>> toResponse(S3TestResponse body) {
        HttpStatus status = body.isOk() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        ApiResponse<S3TestResponse> response = body.isOk()
                ? ApiResponse.success(status.value(), body.getMessage(), body)
                : ApiResponse.error(status.value(), body.getMessage(), body);
        return ResponseEntity.status(status).body(response);
    }
}
