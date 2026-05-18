package com.material.platform.controller;

import com.material.platform.common.Result;
import com.material.platform.dto.FolderTreeNode;
import com.material.platform.entity.Folder;
import com.material.platform.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    public Result<Folder> createFolder(@RequestBody Map<String, Object> params) {
        String name = params.get("name") == null ? null : params.get("name").toString();
        Long parentId = params.get("parentId") != null
                ? Long.valueOf(params.get("parentId").toString())
                : 0L;
        Folder folder = folderService.createFolder(name, parentId);
        return Result.success(folder);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteFolder(@PathVariable Long id) {
        try {
            folderService.deleteFolder(id);
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Folder> updateFolder(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            String name = params.get("name") == null ? null : params.get("name").toString();
            Long parentId = params.get("parentId") != null
                    ? Long.valueOf(params.get("parentId").toString())
                    : null;
            Folder folder = folderService.updateFolder(id, name, parentId);
            return Result.success(folder);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping
    public Result<List<Folder>> listFolders(@RequestParam(defaultValue = "0") Long parentId) {
        return Result.success(folderService.listFolders(parentId));
    }

    @GetMapping("/tree")
    public Result<List<FolderTreeNode>> getFolderTree(@RequestParam(defaultValue = "name") String sortBy,
                                                      @RequestParam(defaultValue = "desc") String sortOrder) {
        return Result.success(folderService.listFolderTree(sortBy, sortOrder));
    }
}
