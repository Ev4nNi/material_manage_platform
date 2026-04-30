package com.material.platform.controller;

import com.material.platform.common.Result;
import com.material.platform.dto.LoginUserDto;
import com.material.platform.dto.PageResult;
import com.material.platform.entity.Asset;
import com.material.platform.service.AssetService;
import com.material.platform.service.AuthService;
import com.material.platform.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private static final Logger log = LoggerFactory.getLogger(AssetController.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final Map<String, String> IMAGE_MIME_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp",
            "bmp", "image/bmp"
    );

    private static final Map<String, String> VIDEO_MIME_TYPES = Map.of(
            "mp4", "video/mp4",
            "avi", "video/x-msvideo",
            "mov", "video/quicktime",
            "mkv", "video/x-matroska",
            "webm", "video/webm"
    );

    private final AssetService assetService;
    private final StorageService storageService;

    private String getCurrentUsername(HttpServletRequest request) {
        try {
            LoginUserDto loginUser = (LoginUserDto) request.getSession().getAttribute(AuthService.LOGIN_USER_SESSION_KEY);
            return loginUser != null ? loginUser.getUsername() : "admin";
        } catch (Exception e) {
            return "admin";
        }
    }

    @PostMapping("/upload")
    public Result<Asset> uploadAsset(@RequestParam("file") MultipartFile file,
                                     @RequestParam Long folderId,
                                     @RequestParam(required = false) String relativePath,
                                     HttpServletRequest request) {
        try {
            String uploadedBy = getCurrentUsername(request);
            return Result.success(assetService.uploadAsset(file, folderId, relativePath, uploadedBy));
        } catch (IOException | RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/upload-directory")
    public Result<List<Asset>> uploadDirectory(@RequestParam("files") List<MultipartFile> files,
                                               @RequestParam("relativePaths") List<String> relativePaths,
                                               @RequestParam Long folderId,
                                               HttpServletRequest request) {
        try {
            String uploadedBy = getCurrentUsername(request);
            return Result.success(assetService.uploadDirectory(files, relativePaths, folderId, uploadedBy));
        } catch (IOException | RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAsset(@PathVariable Long id) {
        try {
            assetService.deleteAsset(id);
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Asset> updateAsset(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            String originalName = params.get("originalName") == null ? null : params.get("originalName").toString();
            Long folderId = params.get("folderId") != null ? Long.valueOf(params.get("folderId").toString()) : null;
            return Result.success(assetService.updateAsset(id, originalName, folderId));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping
    public Result<PageResult<Asset>> listAssets(@RequestParam(required = false) Long folderId,
                                               @RequestParam(required = false) String startDate,
                                               @RequestParam(required = false) String endDate,
                                               @RequestParam(required = false) String fileType,
                                               @RequestParam(required = false) String uploadedBy,
                                               @RequestParam(required = false) String fileName,
                                               @RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "20") int pageSize) {
        try {
            if (pageNum < 1) pageNum = 1;
            if (pageSize < 1) pageSize = DEFAULT_PAGE_SIZE;
            if (pageSize > MAX_PAGE_SIZE) pageSize = MAX_PAGE_SIZE;

            if (folderId != null) {
                return Result.success(assetService.listByFolder(folderId, startDate, endDate, fileType, uploadedBy, fileName, pageNum, pageSize));
            }
            if (startDate != null && endDate != null) {
                return Result.success(assetService.listByDateRange(startDate, endDate, pageNum, pageSize));
            }
            return Result.error("必须提供 folderId 或 startDate/endDate");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> previewAsset(@PathVariable Long id) {
        return downloadAsset(id, "inline");
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadAsset(@PathVariable Long id) {
        return downloadAsset(id, "attachment");
    }

    private ResponseEntity<Resource> downloadAsset(Long id, String disposition) {
        try {
            Asset asset = assetService.getAssetById(id);
            if (asset == null) {
                return ResponseEntity.notFound().build();
            }

            File file = storageService.getFile(asset.getStorageKey());
            if (!file.exists()) {
                log.warn("{}文件不存在: ID={}, storageKey={}", disposition, id, asset.getStorageKey());
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = getContentType(asset);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename*=UTF-8''" + java.net.URLEncoder.encode(asset.getOriginalName(), java.nio.charset.StandardCharsets.UTF_8))
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()))
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            log.error("{}异常: ID={}, 错误={}", disposition, id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getContentType(Asset asset) {
        String extension = extractExtension(asset.getOriginalName());
        String mimeType = IMAGE_MIME_TYPES.get(extension);
        if (mimeType != null) {
            return mimeType;
        }
        mimeType = VIDEO_MIME_TYPES.get(extension);
        if (mimeType != null) {
            return mimeType;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    @GetMapping("/{id}")
    public Result<Asset> getAsset(@PathVariable Long id) {
        Asset asset = assetService.getAssetById(id);
        if (asset == null) {
            return Result.error("素材不存在");
        }
        return Result.success(asset);
    }

    @PostMapping("/{id}/re-extract")
    public Result<Asset> reExtractMetadata(@PathVariable Long id) {
        try {
            return Result.success(assetService.reExtractMetadata(id));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/batch-download")
    public Result<List<String>> batchDownload(@RequestBody Map<String, List<Long>> params) {
        try {
            List<Long> assetIds = params.get("assetIds");
            if (assetIds == null || assetIds.isEmpty()) {
                return Result.error("素材ID列表不能为空");
            }

            List<String> downloadUrls = new ArrayList<>();
            for (Long id : assetIds) {
                Asset asset = assetService.getAssetById(id);
                if (asset != null) {
                    downloadUrls.add("/api/assets/" + id + "/download");
                }
            }

            return Result.success(downloadUrls);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
