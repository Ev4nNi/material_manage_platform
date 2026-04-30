package com.material.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.material.platform.entity.Asset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssetMapper extends BaseMapper<Asset> {

    List<Asset> selectByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
