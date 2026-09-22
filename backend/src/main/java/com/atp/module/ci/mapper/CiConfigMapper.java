package com.atp.module.ci.mapper;

import com.atp.module.ci.entity.CiConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * CI 集成配置数据访问
 */
@Mapper
public interface CiConfigMapper extends BaseMapper<CiConfig> {
}
