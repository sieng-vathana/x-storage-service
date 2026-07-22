package com.x.storage.service;

import com.x.storage.config.StorageProperties;
import com.x.storage.dto.S3TestResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Live S3 probe: head-bucket → put → get → delete a tiny object under {@code test/}.
 * Does not touch the files DB table.
 */
@Service
public class S3TestService {

    private final StorageProperties properties;
    private final ObjectProvider<S3Client> s3ClientProvider;

    public S3TestService(StorageProperties properties, ObjectProvider<S3Client> s3ClientProvider) {
        this.properties = properties;
        this.s3ClientProvider = s3ClientProvider;
    }

    public S3TestResponse runProbe() {
        long start = System.currentTimeMillis();
        S3TestResponse response = new S3TestResponse();
        response.setStorageType(properties.getType());

        if (!properties.isS3()) {
            response.setOk(false);
            response.setMessage("Storage backend is '" + properties.getType()
                    + "', not s3. Set x.storage.type=s3 (local profile already does) and restart.");
            response.setElapsedMs(System.currentTimeMillis() - start);
            return response;
        }

        StorageProperties.S3 s3 = properties.getS3();
        response.setBucket(s3.getBucket());
        response.setRegion(s3.getRegion());

        S3Client client = s3ClientProvider.getIfAvailable();
        if (client == null) {
            response.setOk(false);
            response.setMessage("S3Client bean is missing. Check access-key/secret-key in config/secrets.yml.");
            response.setElapsedMs(System.currentTimeMillis() - start);
            return response;
        }

        String key = "test/s3-probe-" + UUID.randomUUID().toString().replace("-", "") + ".txt";
        // Internal only — verify put/get integrity; never returned in the API body.
        String payload = "x-s3-probe " + Instant.now() + " " + UUID.randomUUID();
        response.setKey(key);

        try {
            // 1) Can we reach the bucket?
            client.headBucket(HeadBucketRequest.builder().bucket(s3.getBucket()).build());
            response.step("headBucket", "ok");

            // 2) Write
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3.getBucket())
                            .key(key)
                            .contentType("text/plain; charset=utf-8")
                            .build(),
                    RequestBody.fromString(payload, StandardCharsets.UTF_8));
            response.step("putObject", "ok");

            // 3) Read back and verify
            byte[] bytes = client.getObject(
                    GetObjectRequest.builder().bucket(s3.getBucket()).key(key).build(),
                    ResponseTransformer.toBytes()).asByteArray();
            String readBack = new String(bytes, StandardCharsets.UTF_8);
            if (!payload.equals(readBack)) {
                response.step("getObject", "mismatch");
                response.setOk(false);
                response.setMessage("S3 put succeeded but read-back content did not match.");
                response.setElapsedMs(System.currentTimeMillis() - start);
                return response;
            }
            response.step("getObject", "ok");

            // 4) Cleanup
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(s3.getBucket())
                    .key(key)
                    .build());
            response.step("deleteObject", "ok");

            response.setOk(true);
            response.setMessage("S3 works: wrote, read, and deleted object in bucket '"
                    + s3.getBucket() + "' (" + s3.getRegion() + ").");
        } catch (S3Exception e) {
            String detail = e.awsErrorDetails() != null
                    ? e.awsErrorDetails().errorMessage()
                    : e.getMessage();
            response.setOk(false);
            response.setMessage("S3 error HTTP " + e.statusCode() + ": " + detail
                    + " (code=" + (e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "?") + ")");
            response.step("error", detail);
        } catch (Exception e) {
            response.setOk(false);
            response.setMessage("S3 probe failed: " + e.getMessage());
            response.step("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        response.setElapsedMs(System.currentTimeMillis() - start);
        return response;
    }
}
