package com.material.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.material.platform.entity.Asset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssetMapper extends BaseMapper<Asset> {

    List<Asset> selectByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Asset> selectByRefs(@Param("assetRefs") List<String> assetRefs, @Param("legacyIds") List<Long> legacyIds);

    List<Asset> selectByFolderIds(@Param("folderIds") List<Long> folderIds);

    List<String> selectReferencedStorageKeys(@Param("storageKeys") List<String> storageKeys, @Param("excludedIds") List<Long> excludedIds);

    int batchUpdateFolder(@Param("assetIds") List<Long> assetIds, @Param("folderId") Long folderId);

    int batchDeleteByIds(@Param("assetIds") List<Long> assetIds);
}
