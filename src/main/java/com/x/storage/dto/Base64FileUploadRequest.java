package com.x.storage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * JSON body for picture/file upload (base64 content).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Base64FileUploadRequest {

    /** Original file name, e.g. photo.jpg */
    private String fileName;

    /** MIME type, e.g. image/jpeg or image/png */
    private String contentType;

    /**
     * Base64 image bytes. May include data-URL prefix:
     * {@code data:image/png;base64,iVBORw0KGgo...}
     */
    private String contentBase64;

    /** media | file — default media for images */
    private String kind;

    private String ownerType;
    private String ownerId;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}
