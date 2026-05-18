package com.material.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.material.platform.entity.Folder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FolderMapper extends BaseMapper<Folder> {

    List<Long> selectDescendantIds(@Param("folderId") Long folderId);

    int deleteByIds(@Param("folderIds") List<Long> folderIds);
}
