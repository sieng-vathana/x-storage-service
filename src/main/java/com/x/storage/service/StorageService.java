package com.x.storage.service;

import com.x.redis.cache.CacheNames;
import com.x.storage.config.StorageProperties;
import com.x.storage.domain.StorageKind;
import com.x.storage.dto.Base64FileUploadRequest;
import com.x.storage.dto.FileResponse;
import com.x.storage.entity.StoredFile;
import com.x.storage.repository.StoredFileRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class StorageService {

    private static final Set<String> MEDIA_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg", ".heic", ".heif",
            ".mp4", ".webm", ".mov", ".m4v", ".avi");

    private final StoredFileRepository repository;
    private final StorageProperties properties;
    private final ObjectStorage objectStorage;
    private final S3UrlBuilder s3UrlBuilder;

    public StorageService(
            StoredFileRepository repository,
            StorageProperties properties,
            ObjectStorage objectStorage,
            S3UrlBuilder s3UrlBuilder) {
        this.repository = repository;
        this.properties = properties;
        this.objectStorage = objectStorage;
        this.s3UrlBuilder = s3UrlBuilder;
    }

    @Transactional
    public FileResponse store(MultipartFile file, String ownerType, String ownerId, String kindParam) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        if (original.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        String ext = extensionOf(original);
        StorageKind kind = resolveKind(kindParam, ext, file.getContentType());
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;

        String relativePath = objectStorage.store(file, kind, storedName);

        StoredFile entity = new StoredFile();
        entity.setOriginalName(original);
        entity.setStoredName(storedName);
        entity.setContentType(file.getContentType());
        entity.setSizeBytes(file.getSize());
        entity.setRelativePath(relativePath);
        entity.setStorageKind(kind.name());
        entity.setStorageBackend(properties.isS3() ? "s3" : "local");
        entity.setOwnerType(ownerType);
        entity.setOwnerId(ownerId);

        StoredFile saved = repository.save(entity);
        return toResponse(saved);
    }

    /**
     * Store a picture/file from JSON base64 body → S3 (or local) + MySQL metadata.
     */
    @Transactional
    public FileResponse storeBase64(Base64FileUploadRequest request) {
        if (request == null || !StringUtils.hasText(request.getContentBase64())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentBase64 is required");
        }

        String raw = request.getContentBase64().trim();
        String contentType = request.getContentType();
        String original = StringUtils.hasText(request.getFileName()) ? request.getFileName() : "image.png";

        // Support data URL: data:image/png;base64,XXXX
        if (raw.startsWith("data:")) {
            int comma = raw.indexOf(',');
            if (comma < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid data URL in contentBase64");
            }
            String header = raw.substring(5, comma); // image/png;base64
            if (contentType == null || contentType.isBlank()) {
                int semi = header.indexOf(';');
                contentType = semi > 0 ? header.substring(0, semi) : header;
            }
            raw = raw.substring(comma + 1);
        }

        byte[] data;
        try {
            data = Base64.getDecoder().decode(raw.replaceAll("\\s", ""));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentBase64 is not valid base64", e);
        }
        if (data.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decoded image is empty");
        }

        original = StringUtils.cleanPath(original);
        if (original.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        if (!StringUtils.hasText(contentType)) {
            contentType = guessContentType(original);
        }

        String ext = extensionOf(original);
        if (ext.isEmpty()) {
            ext = extensionFromContentType(contentType);
            original = original + ext;
        }

        StorageKind kind = resolveKind(
                StringUtils.hasText(request.getKind()) ? request.getKind() : "media",
                ext,
                contentType);
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        String relativePath = objectStorage.storeBytes(data, contentType, kind, storedName);

        StoredFile entity = new StoredFile();
        entity.setOriginalName(original);
        entity.setStoredName(storedName);
        entity.setContentType(contentType);
        entity.setSizeBytes(data.length);
        entity.setRelativePath(relativePath);
        entity.setStorageKind(kind.name());
        entity.setStorageBackend(properties.isS3() ? "s3" : "local");
        entity.setOwnerType(request.getOwnerType());
        entity.setOwnerId(request.getOwnerId());

        return toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public FileResponse getMeta(Long id) {
        return toResponse(find(id));
    }

    /** Internal lookup for services migrating legacy direct S3 URLs to a fresh signed URL. */
    @Cacheable(cacheNames = CacheNames.STORAGE_FILE_BY_RELATIVE_PATH, key = "#relativePath")
    @Transactional(readOnly = true)
    public FileResponse getMetaByRelativePath(String relativePath) {
        if (!StringUtils.hasText(relativePath)
                || relativePath.startsWith("/")
                || relativePath.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid relative path");
        }
        StoredFile file = repository.findByRelativePath(relativePath)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        return toResponse(file);
    }

    @Transactional(readOnly = true)
    public Resource loadAsResource(Long id) {
        StoredFile file = find(id);
        return objectStorage.load(file.getRelativePath());
    }

    @Transactional(readOnly = true)
    public StoredFile find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    @CacheEvict(cacheNames = CacheNames.STORAGE_FILE_BY_RELATIVE_PATH, allEntries = true)
    @Transactional
    public void delete(Long id) {
        StoredFile file = find(id);
        objectStorage.delete(file.getRelativePath());
        repository.delete(file);
    }

    private StorageKind resolveKind(String kindParam, String ext, String contentType) {
        if (StringUtils.hasText(kindParam)) {
            String k = kindParam.trim().toLowerCase(Locale.ROOT);
            if (k.equals("media") || k.equals("image") || k.equals("photo")) {
                return StorageKind.MEDIA;
            }
            if (k.equals("file") || k.equals("files") || k.equals("document") || k.equals("doc")) {
                return StorageKind.FILE;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "kind must be 'media' or 'file'");
        }
        if (MEDIA_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) {
            return StorageKind.MEDIA;
        }
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            if (ct.startsWith("image/") || ct.startsWith("video/")) {
                return StorageKind.MEDIA;
            }
        }
        return StorageKind.FILE;
    }

    private static String extensionOf(String original) {
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            return original.substring(dot).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private static String extensionFromContentType(String contentType) {
        if (contentType == null) {
            return ".bin";
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/bmp" -> ".bmp";
            case "image/svg+xml" -> ".svg";
            default -> ".bin";
        };
    }

    private static String guessContentType(String fileName) {
        String ext = extensionOf(fileName).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".bmp" -> "image/bmp";
            case ".svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }

    private FileResponse toResponse(StoredFile file) {
        return new FileResponse(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getSizeBytes(),
                resolvePublicUrl(file),
                file.getRelativePath(),
                file.getStorageKind(),
                file.getStorageBackend(),
                file.getOwnerType(),
                file.getOwnerId(),
                file.getCreatedAt()
        );
    }

    /**
     * S3: full AWS URL (presigned by default — browser loads S3 directly, no API blob proxy).
     * Local: API content path.
     */
    private String resolvePublicUrl(StoredFile file) {
        boolean s3 = properties.isS3() || "s3".equalsIgnoreCase(file.getStorageBackend());
        if (s3 && StringUtils.hasText(file.getRelativePath())) {
            return s3UrlBuilder.buildObjectUrl(file.getRelativePath());
        }
        String base = properties.getPublicBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/api/v1/files/" + file.getId() + "/content";
    }
}
