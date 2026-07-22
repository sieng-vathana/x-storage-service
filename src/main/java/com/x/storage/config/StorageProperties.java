package com.x.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "x.storage")
public class StorageProperties {

    /**
     * {@code local} = disk folders under root-dir; {@code s3} = AWS S3 bucket.
     */
    private String type = "local";

    /** Local disk root (used when type=local). Contains media/ and files/. */
    private String rootDir = "./uploads";

    private String publicBaseUrl = "http://127.0.0.1:8088";

    private final S3 s3 = new S3();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public S3 getS3() {
        return s3;
    }

    public boolean isS3() {
        return type != null && type.equalsIgnoreCase("s3");
    }

    public static class S3 {
        private String bucket = "x-storage-service";
        private String region = "ap-southeast-1";
        private String accessKey;
        private String secretKey;
        /** Optional custom endpoint (MinIO). Leave empty for real AWS. */
        private String endpoint = "";

        /**
         * How to build {@code FileResponse.url} for S3 objects:
         * <ul>
         *   <li>{@code public} — permanent {@code https://bucket.s3.region.amazonaws.com/key}
         *       (bucket/object must allow public read)</li>
         *   <li>{@code presigned} — temporary signed GET URL (private bucket OK; browser loads S3 directly)</li>
         * </ul>
         */
        /** public = clean permanent S3 URL (default). presigned = long signed query string. */
        private String urlMode = "public";

        /** Presigned URL lifetime in hours (only when url-mode=presigned). */
        private int presignDurationHours = 24;

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getUrlMode() {
            return urlMode;
        }

        public void setUrlMode(String urlMode) {
            this.urlMode = urlMode;
        }

        public int getPresignDurationHours() {
            return presignDurationHours;
        }

        public void setPresignDurationHours(int presignDurationHours) {
            this.presignDurationHours = presignDurationHours;
        }
    }
}
