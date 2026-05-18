package com.material.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.material.platform.dto.FolderTreeNode;
import com.material.platform.entity.Folder;
import com.material.platform.mapper.FolderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderMapper folderMapper;
    private final AssetBatchOperationService assetBatchOperationService;

    public Folder createFolder(String name, Long parentId) {
        LocalDateTime now = LocalDateTime.now();
        Folder folder = new Folder();
        folder.setName(name);
        folder.setParentId(parentId);
        folder.setCreatedAt(now);
        folder.setUpdatedAt(now);
        folderMapper.insert(folder);
        return folder;
    }

    @Transactional
    public void deleteFolder(Long id) {
        requireFolder(id);
        List<Long> folderIds = folderMapper.selectDescendantIds(id);
        if (folderIds == null || folderIds.isEmpty()) {
            throw new RuntimeException("文件夹不存在");
        }

        assetBatchOperationService.deleteAssetsByFolderIds(folderIds);
        folderMapper.deleteByIds(folderIds);
    }

    public Folder updateFolder(Long id, String name, Long parentId) {
        Folder folder = folderMapper.selectById(id);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }
        if (name != null && !name.isBlank()) {
            folder.setName(name.trim());
        }
        if (parentId != null) {
            folder.setParentId(parentId);
        }
        folder.setUpdatedAt(LocalDateTime.now());
        folderMapper.updateById(folder);
        return folder;
    }

    public List<Folder> listFolders(Long parentId) {
        return folderMapper.selectList(
                new LambdaQueryWrapper<Folder>()
                        .eq(Folder::getParentId, parentId)
                        .orderByAsc(Folder::getCreatedAt, Folder::getId)
        );
    }

    public List<FolderTreeNode> listFolderTree() {
        return listFolderTree("name", "desc");
    }

    public List<FolderTreeNode> listFolderTree(String sortBy, String sortOrder) {
        List<Folder> allFolders = folderMapper.selectList(
                new LambdaQueryWrapper<Folder>().orderByAsc(Folder::getCreatedAt, Folder::getId)
        );
        Map<Long, List<Folder>> childrenByParent = new HashMap<>();
        for (Folder folder : allFolders) {
            childrenByParent.computeIfAbsent(folder.getParentId(), key -> new ArrayList<>()).add(folder);
        }
        return buildTree(0L, childrenByParent, normalizeSortBy(sortBy), normalizeSortOrder(sortOrder));
    }

    public Folder requireFolder(Long id) {
        Folder folder = folderMapper.selectById(id);
        if (folder == null) {
            throw new RuntimeException("目标文件夹不存在");
        }
        return folder;
    }

    public Long ensureFolderPath(Long baseFolderId, String relativeDirectory, Map<String, Long> folderCache) {
        requireFolder(baseFolderId);
        if (relativeDirectory == null || relativeDirectory.isBlank()) {
            return baseFolderId;
        }

        Long currentParentId = baseFolderId;
        String[] pathSegments = relativeDirectory.replace("\\", "/").split("/");
        for (String rawSegment : pathSegments) {
            String folderName = rawSegment.trim();
            if (folderName.isEmpty() || ".".equals(folderName)) {
                continue;
            }

            String cacheKey = currentParentId + ":" + folderName;
            if (folderCache != null && folderCache.containsKey(cacheKey)) {
                currentParentId = folderCache.get(cacheKey);
                continue;
            }

            Folder childFolder = folderMapper.selectOne(
                    new LambdaQueryWrapper<Folder>()
                            .eq(Folder::getParentId, currentParentId)
                            .eq(Folder::getName, folderName)
                            .last("LIMIT 1")
            );
            if (childFolder == null) {
                childFolder = createFolder(folderName, currentParentId);
            }

            currentParentId = childFolder.getId();
            if (folderCache != null) {
                folderCache.put(cacheKey, currentParentId);
            }
        }
        return currentParentId;
    }

    private List<FolderTreeNode> buildTree(Long parentId, Map<Long, List<Folder>> childrenByParent, String sortBy, String sortOrder) {
        List<Folder> children = new ArrayList<>(childrenByParent.getOrDefault(parentId, List.of()));
        children.sort(buildComparator(sortBy, sortOrder));

        List<FolderTreeNode> result = new ArrayList<>();
        for (Folder folder : children) {
            FolderTreeNode node = new FolderTreeNode();
            node.setId(folder.getId());
            node.setName(folder.getName());
            node.setParentId(folder.getParentId());

            List<FolderTreeNode> childNodes = buildTree(folder.getId(), childrenByParent, sortBy, sortOrder);
            node.setChildren(childNodes);
            node.setLeaf(childNodes.isEmpty());
            result.add(node);
        }
        return result;
    }

    private Comparator<Folder> buildComparator(String sortBy, String sortOrder) {
        Comparator<Folder> comparator = switch (sortBy) {
            case "createdAt" -> Comparator.comparing(Folder::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "updatedAt" -> Comparator.comparing(
                    folder -> folder.getUpdatedAt() != null ? folder.getUpdatedAt() : folder.getCreatedAt(),
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "name" -> Comparator.comparing(
                    folder -> folder.getName() == null ? "" : folder.getName().toLowerCase(Locale.ROOT)
            );
            default -> Comparator.comparing(
                    folder -> folder.getName() == null ? "" : folder.getName().toLowerCase(Locale.ROOT)
            );
        };

        if ("desc".equals(sortOrder)) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparing(Folder::getId);
    }

    private String normalizeSortBy(String sortBy) {
        if ("createdAt".equalsIgnoreCase(sortBy)) {
            return "createdAt";
        }
        if ("updatedAt".equalsIgnoreCase(sortBy)) {
            return "updatedAt";
        }
        return "name";
    }

    private String normalizeSortOrder(String sortOrder) {
        if ("desc".equalsIgnoreCase(sortOrder)) {
            return "desc";
        }
        return "asc";
    }
}
