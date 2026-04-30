package com.material.platform.service;

import com.material.platform.entity.Asset;
import com.material.platform.mapper.AssetMapper;
import com.material.platform.metadata.MetadataExtractor;
import com.material.platform.metadata.MetadataExtractorFactory;
import com.material.platform.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetMapper assetMapper;

    @Mock
    private StorageService storageService;

    @Mock
    private MetadataExtractorFactory metadataExtractorFactory;

    @Mock
    private MetadataExtractor metadataExtractor;

    @Mock
    private FolderService folderService;

    private AssetService assetService;

    @BeforeEach
    void setUp() {
        assetService = new AssetService(assetMapper, storageService, metadataExtractorFactory, folderService);
    }

    @Test
    void testUploadAsset() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "fake image content".getBytes()
        );

        when(metadataExtractorFactory.getExtractor(anyString())).thenReturn(metadataExtractor);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("width", 100);
        metadata.put("height", 200);
        when(metadataExtractor.extract(any(), anyString())).thenReturn(metadata);
        when(folderService.requireFolder(1L)).thenReturn(null);
        when(folderService.ensureFolderPath(anyLong(), anyString(), any())).thenReturn(1L);
        when(assetMapper.insert(any(Asset.class))).thenAnswer(invocation -> {
            Asset asset = invocation.getArgument(0);
            asset.setId(1L);
            return 1;
        });

        Asset asset = assetService.uploadAsset(file, 1L, null, "admin");

        assertNotNull(asset);
        assertEquals("test.jpg", asset.getOriginalName());
        assertEquals(1L, asset.getFolderId());
        assertNotNull(asset.getStorageKey());
        assertNotNull(asset.getUploadDate());
        verify(storageService).upload(any(), anyString());
        verify(assetMapper).insert(any(Asset.class));
    }

    @Test
    void testUploadAssetWithMetadata() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.png",
            "image/png",
            "content".getBytes()
        );

        when(metadataExtractorFactory.getExtractor(anyString())).thenReturn(metadataExtractor);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("width", 1920);
        metadata.put("height", 1080);
        metadata.put("format", "png");
        when(metadataExtractor.extract(any(), anyString())).thenReturn(metadata);
        when(folderService.requireFolder(1L)).thenReturn(null);
        when(folderService.ensureFolderPath(anyLong(), anyString(), any())).thenReturn(1L);
        when(assetMapper.insert(any(Asset.class))).thenAnswer(invocation -> {
            Asset asset = invocation.getArgument(0);
            asset.setId(1L);
            return 1;
        });

        Asset asset = assetService.uploadAsset(file, 1L, null, "admin");
        assertNotNull(asset.getMetadata());
        assertTrue(asset.getMetadata().contains("1920"));
    }

    @Test
    void testDeleteAsset() throws IOException {
        Asset asset = new Asset();
        asset.setId(1L);
        asset.setStorageKey("2026/04/24/test.jpg");

        when(assetMapper.selectById(1L)).thenReturn(asset);
        doNothing().when(storageService).delete(anyString());
        when(assetMapper.deleteById(1L)).thenReturn(1);

        assetService.deleteAsset(1L);
        verify(storageService).delete("2026/04/24/test.jpg");
        verify(assetMapper).deleteById(1L);
    }

    @Test
    void testDeleteAssetNotFound() {
        when(assetMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            assetService.deleteAsset(999L);
        });
    }

    @Test
    void testUpdateAssetRename() {
        Asset asset = new Asset();
        asset.setId(1L);
        asset.setOriginalName("original.jpg");
        asset.setFolderId(1L);

        when(assetMapper.selectById(1L)).thenReturn(asset);
        when(assetMapper.updateById(any(Asset.class))).thenReturn(1);

        Asset updated = assetService.updateAsset(1L, "new_name.jpg", null);
        assertEquals("new_name.jpg", updated.getOriginalName());
    }

    @Test
    void testUpdateAssetMove() {
        Asset asset = new Asset();
        asset.setId(1L);
        asset.setOriginalName("test.jpg");
        asset.setFolderId(1L);

        when(assetMapper.selectById(1L)).thenReturn(asset);
        when(folderService.requireFolder(2L)).thenReturn(null);
        when(assetMapper.updateById(any(Asset.class))).thenReturn(1);

        Asset updated = assetService.updateAsset(1L, null, 2L);
        assertEquals(2L, updated.getFolderId());
    }

    @Test
    void testListByFolder() {
        List<Asset> assets = List.of(
            createAsset(1L, "file1.jpg", 1L),
            createAsset(2L, "file2.jpg", 1L)
        );

        when(folderService.requireFolder(1L)).thenReturn(null);
        when(assetMapper.selectList(any())).thenReturn(assets);

        List<Asset> result = assetService.listByFolder(1L, null, null, null, null, null);
        assertEquals(2, result.size());
    }

    @Test
    void testListByDateRange() {
        List<Asset> assets = List.of(
            createAsset(1L, "file1.jpg", 1L)
        );

        when(assetMapper.selectByDateRange(anyString(), anyString())).thenReturn(assets);

        List<Asset> result = assetService.listByDateRange("2026-01-01", "2026-12-31");
        assertEquals(1, result.size());
    }

    private Asset createAsset(Long id, String name, Long folderId) {
        Asset asset = new Asset();
        asset.setId(id);
        asset.setOriginalName(name);
        asset.setFolderId(folderId);
        return asset;
    }
}
