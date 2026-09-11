package com.atp.module.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.atp.module.base.entity.ApplicationVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工程版本数据访问
 */
@Mapper
public interface ApplicationVersionMapper extends BaseMapper<ApplicationVersion> {
}
