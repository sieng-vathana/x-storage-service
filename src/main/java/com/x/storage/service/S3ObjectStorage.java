package com.x.storage.service;

import com.x.storage.config.StorageProperties;
import com.x.storage.domain.StorageKind;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "x.storage.type", havingValue = "s3")
public class S3ObjectStorage implements ObjectStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3ObjectStorage(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.bucket = properties.getS3().getBucket();
    }

    @Override
    public String store(MultipartFile file, StorageKind kind, String storedFileName) {
        try {
            return storeBytes(file.getBytes(), file.getContentType(), kind, storedFileName);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read upload", e);
        }
    }

    @Override
    public String storeBytes(byte[] data, String contentType, StorageKind kind, String storedFileName) {
        String key = kind.folder() + "/" + storedFileName;
        try {
            PutObjectRequest.Builder req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key);
            if (contentType != null && !contentType.isBlank()) {
                req.contentType(contentType);
            }
            s3Client.putObject(req.build(), RequestBody.fromBytes(data));
            return key;
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "S3 upload failed: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public Resource load(String relativePath) {
        try {
            byte[] bytes = s3Client.getObject(
                    GetObjectRequest.builder().bucket(bucket).key(relativePath).build(),
                    ResponseTransformer.toBytes()).asByteArray();
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    int slash = relativePath.lastIndexOf('/');
                    return slash >= 0 ? relativePath.substring(slash + 1) : relativePath;
                }
            };
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File binary missing", e);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File binary missing", e);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "S3 download failed: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(relativePath)
                    .build());
        } catch (S3Exception ignored) {
            // still drop metadata
        }
    }
}
