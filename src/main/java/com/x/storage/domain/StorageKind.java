package com.x.storage.domain;

/**
 * Two local/S3 “spaces”:
 * <ul>
 *   <li>{@link #MEDIA} — images / video (prefix {@code media/})</li>
 *   <li>{@link #FILE} — documents pdf/docx/… (prefix {@code files/})</li>
 * </ul>
 */
public enum StorageKind {
    MEDIA("media"),
    FILE("files");

    private final String folder;

    StorageKind(String folder) {
        this.folder = folder;
    }

    public String folder() {
        return folder;
    }
}
