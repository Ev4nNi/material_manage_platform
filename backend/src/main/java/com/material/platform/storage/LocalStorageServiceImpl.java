package com.material.platform.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service("localStorageService")
public class LocalStorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageServiceImpl.class);

    @Value("${storage.local.base-path}")
    private String basePath;

    @Override
    public String upload(MultipartFile file, String storageKey) throws IOException {
        validateStorageKey(storageKey);
        Path targetPath = Paths.get(basePath, storageKey);
        validatePath(targetPath);

        Path parentDir = targetPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            log.debug("Created storage directory: {}", parentDir);
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Uploaded file: {} -> {}", file.getOriginalFilename(), storageKey);
        return storageKey;
    }

    @Override
    public void delete(String storageKey) throws IOException {
        validateStorageKey(storageKey);
        Path filePath = Paths.get(basePath, storageKey);
        validatePath(filePath);

        boolean deleted = Files.deleteIfExists(filePath);
        if (deleted) {
            log.info("Deleted file: {}", storageKey);
        } else {
            log.warn("File did not exist, skipping delete: {}", storageKey);
        }
    }

    @Override
    public File getFile(String storageKey) {
        validateStorageKey(storageKey);
        Path filePath = Paths.get(basePath, storageKey);
        validatePath(filePath);
        return filePath.toFile();
    }

    @Override
    public String getBaseUrl() {
        return basePath;
    }

    private void validateStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key cannot be blank");
        }
        if (storageKey.contains("..") || storageKey.contains(":")) {
            throw new IllegalArgumentException("Invalid storage key format");
        }
    }

    private void validatePath(Path path) {
        try {
            String normalizedPath = path.normalize().toString();
            String normalizedBase = Paths.get(basePath).normalize().toString();
            if (!normalizedPath.startsWith(normalizedBase)) {
                throw new SecurityException("Path traversal detected: " + path);
            }
        } catch (Exception e) {
            throw new SecurityException("Path validation failed: " + e.getMessage(), e);
        }
    }
}
