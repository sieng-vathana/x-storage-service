package com.x.storage.service;

import com.x.storage.config.StorageProperties;
import com.x.storage.domain.StorageKind;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConditionalOnProperty(name = "x.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorage implements ObjectStorage {

    private final Path rootPath;

    public LocalObjectStorage(StorageProperties properties) {
        this.rootPath = Paths.get(properties.getRootDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootPath.resolve(StorageKind.MEDIA.folder()));
            Files.createDirectories(rootPath.resolve(StorageKind.FILE.folder()));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage dirs under " + rootPath, e);
        }
    }

    @Override
    public String store(MultipartFile file, StorageKind kind, String storedFileName) {
        try {
            return storeBytes(file.getBytes(), file.getContentType(), kind, storedFileName);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }
    }

    @Override
    public String storeBytes(byte[] data, String contentType, StorageKind kind, String storedFileName) {
        String relative = kind.folder() + "/" + storedFileName;
        Path destination = rootPath.resolve(relative).normalize();
        if (!destination.startsWith(rootPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid path");
        }
        try {
            Files.createDirectories(destination.getParent());
            Files.write(destination, data);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }
        return relative;
    }

    @Override
    public Resource load(String relativePath) {
        Path path = rootPath.resolve(relativePath).normalize();
        if (!path.startsWith(rootPath) || !Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File binary missing");
        }
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not readable");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid file path", e);
        }
    }

    @Override
    public void delete(String relativePath) {
        Path path = rootPath.resolve(relativePath).normalize();
        if (!path.startsWith(rootPath)) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // metadata still removed
        }
    }
}
