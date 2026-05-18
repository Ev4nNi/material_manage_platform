package com.material.platform.service;

import com.material.platform.entity.Asset;
import com.material.platform.mapper.AssetMapper;
import com.material.platform.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AssetBatchOperationService {

    private static final Logger log = LoggerFactory.getLogger(AssetBatchOperationService.class);

    private final AssetMapper assetMapper;
    private final StorageService storageService;

    public List<Asset> resolveAssetsByRefs(List<String> assetRefs) {
        List<String> normalizedRefs = normalizeAssetRefs(assetRefs);
        List<Long> legacyIds = normalizedRefs.stream()
                .map(this::parseLegacyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Asset> assets = assetMapper.selectByRefs(normalizedRefs, legacyIds);
        Map<Long, Asset> uniqueAssets = new LinkedHashMap<>();
        for (Asset asset : assets) {
            if (asset.getId() != null) {
                uniqueAssets.putIfAbsent(asset.getId(), asset);
            }
        }

        if (uniqueAssets.isEmpty()) {
            throw new RuntimeException("未找到对应素材");
        }
        return new ArrayList<>(uniqueAssets.values());
    }

    @Transactional
    public int batchMoveByRefs(List<String> assetRefs, Long folderId) {
        List<Asset> assets = resolveAssetsByRefs(assetRefs);
        List<Long> assetIds = assets.stream()
                .filter(asset -> !Objects.equals(asset.getFolderId(), folderId))
                .map(Asset::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (assetIds.isEmpty()) {
            return 0;
        }
        return assetMapper.batchUpdateFolder(assetIds, folderId);
    }

    @Transactional
    public int batchDeleteByRefs(List<String> assetRefs) {
        return deleteAssets(resolveAssetsByRefs(assetRefs));
    }

    @Transactional
    public int deleteAssetsByFolderIds(List<Long> folderIds) {
        if (folderIds == null || folderIds.isEmpty()) {
            return 0;
        }
        return deleteAssets(assetMapper.selectByFolderIds(folderIds));
    }

    private int deleteAssets(List<Asset> assets) {
        if (assets == null || assets.isEmpty()) {
            return 0;
        }

        Map<Long, Asset> uniqueAssets = new LinkedHashMap<>();
        for (Asset asset : assets) {
            if (asset.getId() != null) {
                uniqueAssets.putIfAbsent(asset.getId(), asset);
            }
        }
        if (uniqueAssets.isEmpty()) {
            return 0;
        }

        List<Long> assetIds = new ArrayList<>(uniqueAssets.keySet());
        List<String> storageKeys = uniqueAssets.values().stream()
                .map(Asset::getStorageKey)
                .filter(storageKey -> storageKey != null && !storageKey.isBlank())
                .distinct()
                .toList();
        List<String> referencedStorageKeys = storageKeys.isEmpty()
                ? List.of()
                : assetMapper.selectReferencedStorageKeys(storageKeys, assetIds);

        int deletedCount = assetMapper.batchDeleteByIds(assetIds);
        scheduleStorageDeletion(storageKeys, referencedStorageKeys);
        return deletedCount;
    }

    private void scheduleStorageDeletion(List<String> storageKeys, List<String> referencedStorageKeys) {
        Set<String> keysToDelete = new LinkedHashSet<>(storageKeys);
        keysToDelete.removeAll(new LinkedHashSet<>(referencedStorageKeys));
        if (keysToDelete.isEmpty()) {
            return;
        }

        Runnable deletionTask = () -> deleteStorageKeys(keysToDelete);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deletionTask.run();
                }
            });
            return;
        }
        deletionTask.run();
    }

    private void deleteStorageKeys(Set<String> storageKeys) {
        for (String storageKey : storageKeys) {
            try {
                storageService.delete(storageKey);
            } catch (IOException | RuntimeException e) {
                log.error("删除素材物理文件失败: storageKey={}, error={}", storageKey, e.getMessage(), e);
            }
        }
    }

    private List<String> normalizeAssetRefs(List<String> assetRefs) {
        if (assetRefs == null || assetRefs.isEmpty()) {
            throw new RuntimeException("素材ID列表不能为空");
        }
        List<String> normalizedRefs = assetRefs.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(assetRef -> !assetRef.isBlank())
                .distinct()
                .toList();
        if (normalizedRefs.isEmpty()) {
            throw new RuntimeException("素材ID列表不能为空");
        }
        return normalizedRefs;
    }

    private Long parseLegacyId(String assetRef) {
        try {
            return Long.valueOf(assetRef);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
