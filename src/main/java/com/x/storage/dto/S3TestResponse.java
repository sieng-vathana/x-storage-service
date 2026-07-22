package com.x.storage.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result of an S3 connectivity probe (put → get → delete).
 */
public class S3TestResponse {

    private boolean ok;
    private String message;
    private String storageType;
    private String bucket;
    private String region;
    private String key;
    private Long elapsedMs;
    private Map<String, String> steps = new LinkedHashMap<>();

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public Map<String, String> getSteps() {
        return steps;
    }

    public void setSteps(Map<String, String> steps) {
        this.steps = steps;
    }

    public void step(String name, String status) {
        this.steps.put(name, status);
    }
}
