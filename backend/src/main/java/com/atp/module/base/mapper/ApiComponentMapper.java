package com.atp.module.base.mapper;

import com.atp.module.base.entity.ApiComponent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 组合组件数据访问
 */
@Mapper
public interface ApiComponentMapper extends BaseMapper<ApiComponent> {
}
