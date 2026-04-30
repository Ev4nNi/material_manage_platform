package com.material.platform.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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
            log.debug("创建存储目录: {}", parentDir);
        }

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("文件上传成功: {} -> {}", file.getOriginalFilename(), storageKey);
        return storageKey;
    }

    @Override
    public void delete(String storageKey) throws IOException {
        validateStorageKey(storageKey);
        Path filePath = Paths.get(basePath, storageKey);
        validatePath(filePath);

        boolean deleted = Files.deleteIfExists(filePath);
        if (deleted) {
            log.info("文件删除成功: {}", storageKey);
        } else {
            log.warn("文件不存在，跳过删除: {}", storageKey);
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
            throw new IllegalArgumentException("存储键不能为空");
        }
        if (storageKey.contains("..") || storageKey.contains(":")) {
            throw new IllegalArgumentException("非法的存储键格式");
        }
    }

    private void validatePath(Path path) {
        try {
            String normalizedPath = path.normalize().toString();
            String normalizedBase = Paths.get(basePath).normalize().toString();
            if (!normalizedPath.startsWith(normalizedBase)) {
                throw new SecurityException("路径遍历攻击检测: " + path);
            }
        } catch (Exception e) {
            throw new SecurityException("路径验证失败: " + e.getMessage(), e);
        }
    }
}
