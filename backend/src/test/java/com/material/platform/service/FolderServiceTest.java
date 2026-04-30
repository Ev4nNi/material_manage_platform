package com.material.platform.service;

import com.material.platform.entity.Folder;
import com.material.platform.mapper.AssetMapper;
import com.material.platform.mapper.FolderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FolderServiceTest {

    @Mock
    private FolderMapper folderMapper;

    @Mock
    private AssetMapper assetMapper;

    private FolderService folderService;

    @BeforeEach
    void setUp() {
        folderService = new FolderService(folderMapper, assetMapper);
    }

    @Test
    void testCreateRootFolder() {
        when(folderMapper.insert(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(1L);
            return 1;
        });

        Folder folder = folderService.createFolder("测试文件夹", 0L);
        assertNotNull(folder);
        assertEquals("测试文件夹", folder.getName());
        assertEquals(0L, folder.getParentId());
        verify(folderMapper).insert(any(Folder.class));
    }

    @Test
    void testCreateSubFolder() {
        when(folderMapper.insert(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(2L);
            return 1;
        });

        Folder folder = folderService.createFolder("子文件夹", 1L);
        assertNotNull(folder);
        assertEquals("子文件夹", folder.getName());
        assertEquals(1L, folder.getParentId());
    }

    @Test
    void testDeleteFolderSuccess() {
        Folder folder = new Folder();
        folder.setId(1L);
        folder.setName("待删除文件夹");
        folder.setParentId(0L);
        folder.setCreatedAt(LocalDateTime.now());

        when(folderMapper.selectById(1L)).thenReturn(folder);
        when(folderMapper.selectCount(any())).thenReturn(0L);
        when(assetMapper.selectCount(any())).thenReturn(0L);
        when(folderMapper.deleteById(1L)).thenReturn(1);

        folderService.deleteFolder(1L);
        verify(folderMapper).deleteById(1L);
    }

    @Test
    void testDeleteFolderWithChildren() {
        Folder folder = new Folder();
        folder.setId(1L);
        folder.setName("父文件夹");
        folder.setParentId(0L);

        when(folderMapper.selectById(1L)).thenReturn(folder);
        when(folderMapper.selectCount(any())).thenReturn(1L);

        assertThrows(RuntimeException.class, () -> {
            folderService.deleteFolder(1L);
        });
    }

    @Test
    void testDeleteFolderWithAssets() {
        Folder folder = new Folder();
        folder.setId(1L);
        folder.setName("含素材文件夹");
        folder.setParentId(0L);

        when(folderMapper.selectById(1L)).thenReturn(folder);
        when(folderMapper.selectCount(any())).thenReturn(0L);
        when(assetMapper.selectCount(any())).thenReturn(5L);

        assertThrows(RuntimeException.class, () -> {
            folderService.deleteFolder(1L);
        });
    }

    @Test
    void testRenameFolder() {
        Folder folder = new Folder();
        folder.setId(1L);
        folder.setName("原名称");
        folder.setParentId(0L);

        when(folderMapper.selectById(1L)).thenReturn(folder);
        when(folderMapper.updateById(any(Folder.class))).thenReturn(1);

        Folder updated = folderService.updateFolder(1L, "新名称", null);
        assertEquals("新名称", updated.getName());
    }

    @Test
    void testMoveFolder() {
        Folder child = new Folder();
        child.setId(2L);
        child.setName("子文件夹");
        child.setParentId(1L);

        when(folderMapper.selectById(2L)).thenReturn(child);
        when(folderMapper.updateById(any(Folder.class))).thenReturn(1);

        Folder moved = folderService.updateFolder(2L, null, 3L);
        assertEquals(3L, moved.getParentId());
    }

    @Test
    void testListRootFolders() {
        List<Folder> rootFolders = new ArrayList<>();
        rootFolders.add(createFolder(1L, "根文件夹1", 0L));
        rootFolders.add(createFolder(2L, "根文件夹2", 0L));

        when(folderMapper.selectList(any())).thenReturn(rootFolders);

        List<Folder> result = folderService.listFolders(0L);
        assertEquals(2, result.size());
    }

    @Test
    void testListSubFolders() {
        List<Folder> subFolders = new ArrayList<>();
        subFolders.add(createFolder(2L, "子1", 1L));
        subFolders.add(createFolder(3L, "子2", 1L));

        when(folderMapper.selectList(any())).thenReturn(subFolders);

        List<Folder> result = folderService.listFolders(1L);
        assertEquals(2, result.size());
    }

    private Folder createFolder(Long id, String name, Long parentId) {
        Folder folder = new Folder();
        folder.setId(id);
        folder.setName(name);
        folder.setParentId(parentId);
        folder.setCreatedAt(LocalDateTime.now());
        return folder;
    }
}
