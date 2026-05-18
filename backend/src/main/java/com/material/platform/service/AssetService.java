package com.material.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.material.platform.dto.PageResult;
import com.material.platform.entity.Asset;
import com.material.platform.mapper.AssetMapper;
import com.material.platform.metadata.MetadataExtractor;
import com.material.platform.metadata.MetadataExtractorFactory;
import com.material.platform.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService {

    private static final Logger log = LoggerFactory.getLogger(AssetService.class);
    private static final DateTimeFormatter STORAGE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "jpg", "image", "jpeg", "image", "png", "image",
            "gif", "image", "webp", "image", "bmp", "image"
    );

    private static final Map<String, String> VIDEO_TYPES = Map.of(
            "mp4", "video", "avi", "video", "mov", "video",
            "mkv", "video", "webm", "video"
    );

    private final AssetMapper assetMapper;
    private final StorageService storageService;
    private final MetadataExtractorFactory metadataExtractorFactory;
    private final FolderService folderService;
    private final AssetBatchOperationService assetBatchOperationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Asset uploadAsset(MultipartFile file, Long folderId, String relativePath, String uploadedBy) throws IOException {
        folderService.requireFolder(folderId);
        String normalizedPath = normalizeRelativePath(relativePath == null || relativePath.isBlank()
                ? extractFileName(file.getOriginalFilename())
                : relativePath);
        Long targetFolderId = folderService.ensureFolderPath(folderId, extractDirectory(normalizedPath), new HashMap<>());
        log.info("上传素材: 原始文件={}, 目标文件夹={}, 相对路径={}, 上传者={}", file.getOriginalFilename(), targetFolderId, normalizedPath, uploadedBy);
        return saveAsset(file, targetFolderId, extractFileName(normalizedPath), uploadedBy);
    }

    public List<Asset> uploadDirectory(List<MultipartFile> files, List<String> relativePaths, Long folderId, String uploadedBy)
            throws IOException {
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        if (relativePaths == null || relativePaths.size() != files.size()) {
            throw new RuntimeException("目录结构参数不完整");
        }

        folderService.requireFolder(folderId);
        Map<String, Long> folderCache = new HashMap<>();
        List<Asset> uploadedAssets = new ArrayList<>(files.size());

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String normalizedPath = normalizeRelativePath(relativePaths.get(i));
            String relativeDirectory = extractDirectory(normalizedPath);
            String fileName = extractFileName(normalizedPath);
            Long targetFolderId = folderService.ensureFolderPath(folderId, relativeDirectory, folderCache);
            uploadedAssets.add(saveAsset(file, targetFolderId, fileName, uploadedBy));
        }

        log.info("批量上传目录完成: 文件数={}, 根文件夹={}, 上传者={}", uploadedAssets.size(), folderId, uploadedBy);
        return uploadedAssets;
    }

    public void deleteAsset(Long id) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null) {
            throw new RuntimeException("素材不存在，ID: " + id);
        }

        assetBatchOperationService.batchDeleteByRefs(List.of(String.valueOf(id)));
        log.info("素材删除成功: ID={}, 文件={}", id, asset.getOriginalName());
    }

    public Asset updateAsset(Long id, String originalName, Long folderId) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null) {
            throw new RuntimeException("素材不存在，ID: " + id);
        }

        if (originalName != null && !originalName.isBlank()) {
            asset.setOriginalName(originalName.trim());
        }
        if (folderId != null) {
            folderService.requireFolder(folderId);
            asset.setFolderId(folderId);
        }

        assetMapper.updateById(asset);
        log.info("素材更新成功: ID={}, 新文件夹={}", id, folderId);
        return asset;
    }

    public Asset reExtractMetadata(Long id) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null) {
            throw new RuntimeException("素材不存在，ID: " + id);
        }

        String metadataJson = "{}";
        MetadataExtractor extractor = metadataExtractorFactory.getExtractor(asset.getOriginalName());
        if (extractor != null) {
            try {
                java.io.InputStream inputStream = storageService.getFile(asset.getStorageKey()).toURI().toURL().openStream();
                Map<String, Object> metadata = extractor.extract(inputStream, asset.getOriginalName());
                if (metadata != null && !metadata.isEmpty()) {
                    metadataJson = objectMapper.writeValueAsString(metadata);
                    log.info("元数据重新提取成功: ID={}, 元数据={}", id, metadataJson);
                } else {
                    log.warn("元数据提取结果为空: ID={}", id);
                }
            } catch (Exception e) {
                log.error("重新提取元数据失败: ID={}, 错误: {}", id, e.getMessage(), e);
            }
        }

        if (asset.getUploadDate() == null || asset.getUploadDate().isEmpty()) {
            asset.setUploadDate(asset.getCreatedAt() != null ? 
                asset.getCreatedAt().toLocalDate().format(DISPLAY_DATE_FORMAT) : 
                LocalDate.now().format(DISPLAY_DATE_FORMAT));
        }

        asset.setMetadata(metadataJson);
        assetMapper.updateById(asset);
        return asset;
    }

    public List<Asset> listByFolder(Long folderId, String startDate, String endDate, String fileType, String uploadedBy, String fileName) {
        folderService.requireFolder(folderId);
        LambdaQueryWrapper<Asset> wrapper = buildAssetQueryWrapper(folderId, startDate, endDate, fileType, uploadedBy, fileName);
        return assetMapper.selectList(wrapper);
    }

    public PageResult<Asset> listByFolder(Long folderId, String startDate, String endDate, String fileType, String uploadedBy, String fileName, int pageNum, int pageSize) {
        folderService.requireFolder(folderId);
        LambdaQueryWrapper<Asset> wrapper = buildAssetQueryWrapper(folderId, startDate, endDate, fileType, uploadedBy, fileName);
        Page<Asset> page = new Page<>(pageNum, pageSize);
        IPage<Asset> result = assetMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    private LambdaQueryWrapper<Asset> buildAssetQueryWrapper(Long folderId, String startDate, String endDate, String fileType, String uploadedBy, String fileName) {
        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<Asset>()
                .eq(Asset::getFolderId, folderId)
                .orderByDesc(Asset::getCreatedAt);
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(Asset::getUploadDate, startDate.trim());
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(Asset::getUploadDate, endDate.trim());
        }
        if (fileType != null && !fileType.isBlank()) {
            wrapper.eq(Asset::getFileType, fileType.trim());
        }
        if (uploadedBy != null && !uploadedBy.isBlank()) {
            wrapper.eq(Asset::getUploadedBy, uploadedBy.trim());
        }
        if (fileName != null && !fileName.isBlank()) {
            wrapper.like(Asset::getOriginalName, fileName.trim());
        }
        return wrapper;
    }

    public List<Asset> listByDateRange(String startDate, String endDate) {
        return assetMapper.selectByDateRange(startDate, endDate);
    }

    public PageResult<Asset> listByDateRange(String startDate, String endDate, int pageNum, int pageSize) {
        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<Asset>()
                .ge(Asset::getUploadDate, startDate)
                .le(Asset::getUploadDate, endDate)
                .orderByDesc(Asset::getCreatedAt);
        Page<Asset> page = new Page<>(pageNum, pageSize);
        IPage<Asset> result = assetMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    public Asset getAssetById(Long id) {
        return assetMapper.selectById(id);
    }

    public Asset getAssetByRef(String assetRef) {
        if (assetRef == null || assetRef.isBlank()) {
            return null;
        }

        Long legacyId = parseLegacyId(assetRef);
        if (legacyId != null) {
            return assetMapper.selectById(legacyId);
        }

        return assetMapper.selectOne(new LambdaQueryWrapper<Asset>()
                .eq(Asset::getPublicId, assetRef.trim()));
    }

    public void deleteAsset(String assetRef) {
        Asset asset = requireAsset(assetRef);

        assetBatchOperationService.batchDeleteByRefs(List.of(assetRef));
        log.info("素材删除成功: ID={}, 文件={}", asset.getId(), asset.getOriginalName());
    }

    public Asset updateAsset(String assetRef, String originalName, Long folderId) {
        Asset asset = requireAsset(assetRef);

        if (originalName != null && !originalName.isBlank()) {
            asset.setOriginalName(originalName.trim());
        }
        if (folderId != null) {
            folderService.requireFolder(folderId);
            asset.setFolderId(folderId);
        }

        assetMapper.updateById(asset);
        log.info("绱犳潗鏇存柊鎴愬姛: ID={}, 鏂版枃浠跺す={}", asset.getId(), folderId);
        return asset;
    }

    public int batchMoveAssets(List<String> assetRefs, Long folderId) {
        folderService.requireFolder(folderId);
        return assetBatchOperationService.batchMoveByRefs(assetRefs, folderId);
    }

    public int batchDeleteAssets(List<String> assetRefs) {
        return assetBatchOperationService.batchDeleteByRefs(assetRefs);
    }

    public Asset reExtractMetadata(String assetRef) {
        Asset asset = requireAsset(assetRef);
        return reExtractMetadata(asset.getId());
    }

    private Asset requireAsset(String assetRef) {
        Asset asset = getAssetByRef(assetRef);
        if (asset == null) {
            throw new RuntimeException("绱犳潗涓嶅瓨鍦紝ID: " + assetRef);
        }
        return asset;
    }

    private Long parseLegacyId(String assetRef) {
        try {
            return Long.valueOf(assetRef.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Asset saveAsset(MultipartFile file, Long folderId, String displayFileName, String uploadedBy) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        if (displayFileName == null || displayFileName.isBlank()) {
            throw new RuntimeException("文件名不能为空");
        }

        String extension = extractExtension(displayFileName);
        String storageKey = LocalDate.now().format(STORAGE_DATE_FORMAT)
                + "/"
                + UUID.randomUUID()
                + (extension.isEmpty() ? "" : "." + extension);

        String metadataJson = "{}";
        MetadataExtractor extractor = metadataExtractorFactory.getExtractor(displayFileName);
        if (extractor != null) {
            try {
                Map<String, Object> metadata = extractor.extract(file.getInputStream(), displayFileName);
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (Exception e) {
                log.warn("提取元数据失败: {}, 错误: {}", displayFileName, e.getMessage());
            }
        }

        storageService.upload(file, storageKey);

        Asset asset = new Asset();
        asset.setPublicId(UUID.randomUUID().toString());
        asset.setFolderId(folderId);
        asset.setOriginalName(displayFileName);
        asset.setStorageKey(storageKey);
        asset.setFileType(determineFileType(extension));
        asset.setFileSize(file.getSize());
        asset.setUploadDate(LocalDate.now().format(DISPLAY_DATE_FORMAT));
        asset.setMetadata(metadataJson);
        asset.setUploadedBy(uploadedBy != null ? uploadedBy : "admin");

        assetMapper.insert(asset);
        return asset;
    }

    private String determineFileType(String extension) {
        if (extension.isEmpty()) {
            return "other";
        }
        String normalizedExtension = extension.toLowerCase();
        if (IMAGE_TYPES.containsKey(normalizedExtension)) {
            return "image";
        }
        if (VIDEO_TYPES.containsKey(normalizedExtension)) {
            return "video";
        }
        return "other";
    }

    private String normalizeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new RuntimeException("目录上传缺少文件路径");
        }
        return relativePath.replace("\\", "/").replaceAll("/+", "/").replaceAll("^/|/$", "");
    }

    private String extractDirectory(String relativePath) {
        int lastSeparatorIndex = relativePath.lastIndexOf('/');
        if (lastSeparatorIndex < 0) {
            return "";
        }
        return relativePath.substring(0, lastSeparatorIndex);
    }

    private String extractFileName(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalizedPath = path.replace("\\", "/");
        int lastSeparatorIndex = normalizedPath.lastIndexOf('/');
        return lastSeparatorIndex >= 0 ? normalizedPath.substring(lastSeparatorIndex + 1) : normalizedPath;
    }

    private String extractExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
}
