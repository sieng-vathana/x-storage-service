package com.x.storage.service;

import com.x.storage.config.StorageProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

/**
 * Builds browser-loadable S3 URLs (direct to AWS — no API proxy / blob download).
 */
@Component
public class S3UrlBuilder {

    private final StorageProperties properties;
    private final ObjectProvider<S3Presigner> presignerProvider;

    public S3UrlBuilder(StorageProperties properties, ObjectProvider<S3Presigner> presignerProvider) {
        this.properties = properties;
        this.presignerProvider = presignerProvider;
    }

    public String buildObjectUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        StorageProperties.S3 s3 = properties.getS3();
        String mode = s3.getUrlMode() == null ? "public" : s3.getUrlMode().trim().toLowerCase();

        // Default / "public": clean permanent object URL (no query params).
        if (!"presigned".equals(mode)) {
            return publicUrl(s3, objectKey);
        }

        S3Presigner presigner = presignerProvider.getIfAvailable();
        if (presigner == null) {
            return publicUrl(s3, objectKey);
        }

        int hours = Math.max(1, Math.min(s3.getPresignDurationHours(), 168));
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(s3.getBucket())
                .key(objectKey)
                .build();
        PresignedGetObjectRequest presigned = presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofHours(hours))
                        .getObjectRequest(get)
                        .build());
        return presigned.url().toExternalForm();
    }

    private static String publicUrl(StorageProperties.S3 s3, String objectKey) {
        if (StringUtils.hasText(s3.getEndpoint())) {
            String ep = s3.getEndpoint().endsWith("/")
                    ? s3.getEndpoint().substring(0, s3.getEndpoint().length() - 1)
                    : s3.getEndpoint();
            return ep + "/" + s3.getBucket() + "/" + objectKey;
        }
        return "https://" + s3.getBucket() + ".s3." + s3.getRegion() + ".amazonaws.com/" + objectKey;
    }
}
