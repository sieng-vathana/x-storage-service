package com.x.storage.service;

import com.x.storage.config.StorageProperties;
import com.x.storage.dto.FileResponse;
import com.x.storage.entity.StoredFile;
import com.x.storage.repository.StoredFileRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class StorageService {

    private final StoredFileRepository repository;
    private final StorageProperties properties;
    private final Path rootPath;

    public StorageService(StoredFileRepository repository, StorageProperties properties) {
        this.repository = repository;
        this.properties = properties;
        this.rootPath = Paths.get(properties.getRootDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage root: " + this.rootPath, e);
        }
    }

    @Transactional
    public FileResponse store(MultipartFile file, String ownerType, String ownerId) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        if (original.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            ext = original.substring(dot).toLowerCase(Locale.ROOT);
        }

        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path destination = rootPath.resolve(storedName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", e);
        }

        StoredFile entity = new StoredFile();
        entity.setOriginalName(original);
        entity.setStoredName(storedName);
        entity.setContentType(file.getContentType());
        entity.setSizeBytes(file.getSize());
        entity.setRelativePath(storedName);
        entity.setOwnerType(ownerType);
        entity.setOwnerId(ownerId);

        StoredFile saved = repository.save(entity);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public FileResponse getMeta(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public Resource loadAsResource(Long id) {
        StoredFile file = find(id);
        Path path = rootPath.resolve(file.getRelativePath()).normalize();
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

    @Transactional(readOnly = true)
    public StoredFile find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    @Transactional
    public void delete(Long id) {
        StoredFile file = find(id);
        Path path = rootPath.resolve(file.getRelativePath()).normalize();
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // still remove metadata
        }
        repository.delete(file);
    }

    private FileResponse toResponse(StoredFile file) {
        String base = properties.getPublicBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/api/v1/files/" + file.getId() + "/content";
        return new FileResponse(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getSizeBytes(),
                url,
                file.getOwnerType(),
                file.getOwnerId(),
                file.getCreatedAt()
        );
    }
}
