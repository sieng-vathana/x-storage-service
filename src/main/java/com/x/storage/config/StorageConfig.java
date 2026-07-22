package com.x.storage.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "x.storage.type", havingValue = "s3")
    public S3Client s3Client(StorageProperties properties) {
        StorageProperties.S3 s3 = properties.getS3();
        StaticCredentialsProvider credentials = credentials(s3);

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(credentials);

        if (StringUtils.hasText(s3.getEndpoint())) {
            builder.endpointOverride(URI.create(s3.getEndpoint()))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }

        return builder.build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "x.storage.type", havingValue = "s3")
    public S3Presigner s3Presigner(StorageProperties properties) {
        StorageProperties.S3 s3 = properties.getS3();
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(credentials(s3));

        if (StringUtils.hasText(s3.getEndpoint())) {
            builder.endpointOverride(URI.create(s3.getEndpoint()))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }

        return builder.build();
    }

    private static StaticCredentialsProvider credentials(StorageProperties.S3 s3) {
        if (!StringUtils.hasText(s3.getAccessKey()) || !StringUtils.hasText(s3.getSecretKey())) {
            throw new IllegalStateException(
                    "x.storage.type=s3 requires access-key and secret-key "
                            + "(application-secrets.yml or X_S3_ACCESS_KEY / X_S3_SECRET_KEY)");
        }
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey()));
    }
}
