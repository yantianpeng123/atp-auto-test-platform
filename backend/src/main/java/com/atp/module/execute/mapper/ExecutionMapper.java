package com.atp.module.execute.mapper;

import com.atp.module.execute.entity.Execution;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 执行记录头表数据访问。
 */
@Mapper
public interface ExecutionMapper extends BaseMapper<Execution> {
}
