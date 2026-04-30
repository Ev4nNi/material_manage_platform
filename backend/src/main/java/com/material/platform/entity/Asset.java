package com.material.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("assets")
public class Asset {

    @TableId(value = "id", type = IdType.AUTO)
    @TableField("id")
    private Long id;

    @TableField("folder_id")
    private Long folderId;

    @TableField("original_name")
    private String originalName;

    @TableField("storage_key")
    private String storageKey;

    @TableField("file_type")
    private String fileType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("upload_date")
    private String uploadDate;

    @TableField(value = "metadata")
    private String metadata;

    @TableField("uploaded_by")
    private String uploadedBy;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
