package com.material.platform.service;

import com.material.platform.entity.Asset;
import com.material.platform.mapper.AssetMapper;
import com.material.platform.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetBatchOperationServiceTest {

    @Mock
    private AssetMapper assetMapper;

    @Mock
    private StorageService storageService;

    private AssetBatchOperationService assetBatchOperationService;

    @BeforeEach
    void setUp() {
        assetBatchOperationService = new AssetBatchOperationService(assetMapper, storageService);
    }

    @Test
    void testBatchMoveByRefs() {
        Asset first = createAsset(1L, "public-1", 1L, "a.jpg", "key-a");
        Asset second = createAsset(2L, "public-2", 2L, "b.jpg", "key-b");
        when(assetMapper.selectByRefs(anyList(), anyList())).thenReturn(List.of(first, second));
        when(assetMapper.batchUpdateFolder(List.of(1L, 2L), 9L)).thenReturn(2);

        int moved = assetBatchOperationService.batchMoveByRefs(List.of("public-1", "public-2"), 9L);

        assertEquals(2, moved);
        verify(assetMapper).batchUpdateFolder(List.of(1L, 2L), 9L);
    }

    @Test
    void testBatchDeleteByRefsSkipsSharedStorageKey() throws IOException {
        Asset first = createAsset(1L, "public-1", 1L, "a.jpg", "shared-key");
        Asset second = createAsset(2L, "public-2", 1L, "b.jpg", "unique-key");
        when(assetMapper.selectByRefs(anyList(), anyList())).thenReturn(List.of(first, second));
        when(assetMapper.selectReferencedStorageKeys(List.of("shared-key", "unique-key"), List.of(1L, 2L)))
                .thenReturn(List.of("shared-key"));
        when(assetMapper.batchDeleteByIds(List.of(1L, 2L))).thenReturn(2);
        doNothing().when(storageService).delete("unique-key");

        int deleted = assetBatchOperationService.batchDeleteByRefs(List.of("public-1", "public-2"));

        assertEquals(2, deleted);
        verify(assetMapper).batchDeleteByIds(List.of(1L, 2L));
        verify(storageService).delete("unique-key");
        verify(storageService, never()).delete("shared-key");
    }

    @Test
    void testDeleteAssetsByFolderIds() throws IOException {
        Asset asset = createAsset(3L, "public-3", 7L, "c.jpg", "folder-key");
        when(assetMapper.selectByFolderIds(List.of(7L))).thenReturn(List.of(asset));
        when(assetMapper.selectReferencedStorageKeys(List.of("folder-key"), List.of(3L))).thenReturn(List.of());
        when(assetMapper.batchDeleteByIds(List.of(3L))).thenReturn(1);
        doNothing().when(storageService).delete("folder-key");

        int deleted = assetBatchOperationService.deleteAssetsByFolderIds(List.of(7L));

        assertEquals(1, deleted);
        verify(assetMapper).batchDeleteByIds(List.of(3L));
        verify(storageService).delete("folder-key");
    }

    private Asset createAsset(Long id, String publicId, Long folderId, String name, String storageKey) {
        Asset asset = new Asset();
        asset.setId(id);
        asset.setPublicId(publicId);
        asset.setFolderId(folderId);
        asset.setOriginalName(name);
        asset.setStorageKey(storageKey);
        return asset;
    }
}
