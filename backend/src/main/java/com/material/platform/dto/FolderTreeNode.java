package com.material.platform.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FolderTreeNode {

    private Long id;
    private String name;
    private Long parentId;
    private boolean leaf;
    private List<FolderTreeNode> children = new ArrayList<>();
}
