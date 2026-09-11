package com.atp.module.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.atp.module.base.entity.Application;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工程数据访问
 */
@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {
}
