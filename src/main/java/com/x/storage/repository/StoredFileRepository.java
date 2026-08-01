package com.x.storage.repository;

import com.x.storage.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    Optional<StoredFile> findByRelativePath(String relativePath);
}
